package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.check.dto.OriginCheckCreatedResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckSummaryResponse;
import com.tradedoc.origincheck.check.storage.FileStorage;
import com.tradedoc.origincheck.check.storage.PdfBundler;
import com.tradedoc.origincheck.common.ApiException;
import com.tradedoc.origincheck.common.ErrorCode;
import com.tradedoc.origincheck.common.PageResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class OriginCheckService {

    private static final Set<String> PDF_TYPES = Set.of("application/pdf", "application/octet-stream");
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    static final int MAX_FILES = 20;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    /** 화면이 보내는 서류 종류 짐작 값 (RequiredDocument + 패킹리스트) */
    private static final Set<String> DOC_TYPES = Set.of("인보이스", "패킹리스트", "B/L", "원산지증명서", "원산지확인서", "수출신고필증");

    private final OriginCheckRepository checks;
    private final OriginCheckFileRepository checkFiles;
    private final FileStorage storage;
    private final PdfBundler pdf;
    private final ObjectMapper objectMapper;

    public OriginCheckService(OriginCheckRepository checks, OriginCheckFileRepository checkFiles, FileStorage storage,
                              PdfBundler pdf, ObjectMapper objectMapper) {
        this.checks = checks;
        this.checkFiles = checkFiles;
        this.storage = storage;
        this.pdf = pdf;
        this.objectMapper = objectMapper;
    }

    /**
     * 한 선적 건의 PDF 들을 저장하고 점검 건을 QUEUED 로 만든다. agent 제출은 {@link OriginCheckWorker} 가 한다.
     * 파일이 여러 개면 올린 순서대로 PDF 하나로 이어 붙여 그 PDF 로 점검한다(워크플로는 그대로). 원본은 파일별로 보관한다.
     *
     * @param hints 파일과 같은 순서의 서류 종류 짐작(파일명 기준, 화면이 보냄). 없거나 모르는 값은 null 로 둔다
     */
    @Transactional
    public OriginCheckCreatedResponse create(Long userId, List<MultipartFile> files, String title, List<String> hints) {
        if (files == null || files.isEmpty() || files.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new ApiException(ErrorCode.INVALID_FILE, "빈 파일입니다.");
        }
        if (files.size() > MAX_FILES) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "한 번에 최대 " + MAX_FILES + "개까지 올릴 수 있습니다.");
        }
        long total = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > MAX_TOTAL_BYTES) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE, "한 번에 올리는 파일은 합계 100MB까지입니다.");
        }
        files.forEach(OriginCheckService::validatePdf);

        // 1) 원본을 하나씩 저장하고 쪽 수를 센다
        List<Stored> stored = new ArrayList<>();
        for (MultipartFile f : files) {
            String name = f.getOriginalFilename() == null || f.getOriginalFilename().isBlank() ? "upload.pdf" : f.getOriginalFilename();
            String key;
            try (InputStream in = f.getInputStream()) {
                key = storage.save(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            stored.add(new Stored(name, key, f.getSize(), pdf.pageCount(storage.path(key), name)));
        }

        // 2) 점검에 쓸 PDF: 하나면 그대로, 여러 개면 이어 붙인다
        String bundleKey;
        long bundleSize;
        String bundleName;
        if (stored.size() == 1) {
            bundleKey = stored.getFirst().key();
            bundleSize = stored.getFirst().size();
            bundleName = stored.getFirst().name();
        } else {
            Path tmp = null;
            try {
                tmp = Files.createTempFile("origin-check-bundle", ".pdf");
                pdf.merge(stored.stream().map(s -> storage.path(s.key())).toList(), tmp);
                try (InputStream in = Files.newInputStream(tmp)) {
                    bundleKey = storage.save(in);
                }
                bundleSize = Files.size(tmp);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (tmp != null) {
                    try {
                        Files.deleteIfExists(tmp);
                    } catch (IOException ignored) {
                        // 임시 파일은 OS 가 치운다
                    }
                }
            }
            String first = stored.getFirst().name();
            String stem = first.toLowerCase().endsWith(".pdf") ? first.substring(0, first.length() - 4) : first;
            bundleName = stem + " 외 " + (stored.size() - 1) + "건.pdf";
        }

        String t = title == null || title.isBlank() ? bundleName : title.trim();
        OriginCheck check = checks.save(new OriginCheck(userId, t.length() > 200 ? t.substring(0, 200) : t,
                cut(bundleName, 255), bundleKey, bundleSize));

        // 3) 파일별 쪽 범위
        int page = 1;
        for (int i = 0; i < stored.size(); i++) {
            Stored s = stored.get(i);
            String hint = hints != null && i < hints.size() && DOC_TYPES.contains(hints.get(i)) ? hints.get(i) : null;
            checkFiles.save(new OriginCheckFile(check.getId(), i, cut(s.name(), 255), s.key(), s.size(), page, page + s.pages() - 1, hint));
            page += s.pages();
        }
        return new OriginCheckCreatedResponse(check.getId(), check.getStatus());
    }

    private record Stored(String name, String key, long size, int pages) {
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public PageResponse<OriginCheckSummaryResponse> list(Long userId, CheckStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "id"));
        var result = status == null ? checks.findByUserId(userId, pageable)
                : checks.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(result, OriginCheckSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OriginCheckResponse get(Long userId, Long id) {
        OriginCheck c = find(userId, id);
        AgentCheckResult result = c.getResultJson() == null ? null
                : objectMapper.readValue(c.getResultJson(), AgentCheckResult.class);
        return OriginCheckResponse.of(c, result, checkFiles.findByCheckIdOrderBySeqAsc(c.getId()));
    }

    /**
     * 담당자 완료 처리 / 되돌리기. 처리 중(queued·in_progress)인 건은 완료 처리할 수 없다(409).
     * 되돌리기는 상태와 관계없이 된다.
     */
    @Transactional
    public OriginCheckSummaryResponse setDone(Long userId, Long id, boolean done) {
        OriginCheck c = find(userId, id);
        if (done) {
            if (!c.isFinished()) {
                throw new ApiException(ErrorCode.CHECK_NOT_FINISHED);
            }
            c.markDone();
        } else {
            c.reopen();
        }
        return OriginCheckSummaryResponse.from(c);
    }

    @Transactional(readOnly = true)
    public StoredFile file(Long userId, Long id) {
        OriginCheck c = find(userId, id);
        return new StoredFile(new FileSystemResource(storage.path(c.getFileKey())), c.getFileName());
    }

    private OriginCheck find(Long userId, Long id) {
        // 남의 점검 건은 존재 여부도 알리지 않는다(404)
        return checks.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(ErrorCode.ORIGIN_CHECK_NOT_FOUND));
    }

    private static void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_FILE, "빈 파일입니다." + (file == null ? "" : " " + file.getOriginalFilename()));
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!PDF_TYPES.contains(type)) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            if (!Arrays.equals(head, PDF_MAGIC)) {
                throw new ApiException(ErrorCode.INVALID_FILE);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_FILE);
        }
    }

    public record StoredFile(Resource resource, String fileName) {
    }
}
