package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.auth.AuthUser;
import com.tradedoc.origincheck.check.dto.OriginCheckCreatedResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckResponse;
import com.tradedoc.origincheck.check.dto.OriginCheckSummaryResponse;
import com.tradedoc.origincheck.common.PageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/origin-checks")
public class OriginCheckController {

    private final OriginCheckService service;

    public OriginCheckController(OriginCheckService service) {
        this.service = service;
    }

    /** 서류 묶음 PDF 를 접수한다. 처리는 비동기라 바로 202 와 id 를 준다. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OriginCheckCreatedResponse> create(@AuthenticationPrincipal AuthUser user,
                                                             @RequestPart("file") MultipartFile file,
                                                             @RequestParam(value = "title", required = false) String title) {
        OriginCheckCreatedResponse created = service.create(user.id(), file, title);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.accepted().location(location).body(created);
    }

    /** 내 점검 목록(최신순). status 로 거를 수 있다. */
    @GetMapping
    public PageResponse<OriginCheckSummaryResponse> list(@AuthenticationPrincipal AuthUser user,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                                         @RequestParam(value = "size", defaultValue = "20") int size) {
        return service.list(user.id(), CheckStatus.fromValue(status), page, size);
    }

    /** 점검 상세. 완료 전에는 status 만 의미가 있다 → 프론트는 completed/failed 가 될 때까지 몇 초마다 조회한다. */
    @GetMapping("/{id}")
    public OriginCheckResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    /** 원본 PDF (화면의 PDF 뷰어·하이라이트용) */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        OriginCheckService.StoredFile f = service.file(user.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(f.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(f.resource());
    }
}
