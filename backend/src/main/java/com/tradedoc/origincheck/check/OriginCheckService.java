package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.check.dto.OriginCheckCreatedResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckSummaryResponse;
import com.tradedoc.origincheck.check.storage.FileStorage;
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
import java.util.Arrays;
import java.util.Set;

@Service
public class OriginCheckService {

    private static final Set<String> PDF_TYPES = Set.of("application/pdf", "application/octet-stream");
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final OriginCheckRepository checks;
    private final FileStorage storage;
    private final ObjectMapper objectMapper;

    public OriginCheckService(OriginCheckRepository checks, FileStorage storage, ObjectMapper objectMapper) {
        this.checks = checks;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    /** PDF 를 저장하고 점검 건을 QUEUED 로 만든다. agent 제출은 {@link OriginCheckWorker} 가 한다. */
    @Transactional
    public OriginCheckCreatedResponse create(Long userId, MultipartFile file, String title) {
        validatePdf(file);
        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "upload.pdf" : file.getOriginalFilename();
        String key;
        try (InputStream in = file.getInputStream()) {
            key = storage.save(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String t = title == null || title.isBlank() ? fileName : title.trim();
        OriginCheck check = checks.save(new OriginCheck(userId, t.length() > 200 ? t.substring(0, 200) : t,
                fileName, key, file.getSize()));
        return new OriginCheckCreatedResponse(check.getId(), check.getStatus());
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
        return OriginCheckResponse.of(c, result);
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
            throw new ApiException(ErrorCode.INVALID_FILE, "빈 파일입니다.");
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
