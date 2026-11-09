package com.tradedoc.origincheck;

import com.tradedoc.origincheck.agent.AgentException;
import com.tradedoc.origincheck.agent.AgentPoll;
import com.tradedoc.origincheck.agent.AgentSubmission;
import com.tradedoc.origincheck.agent.CheckAgent;
import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.check.OriginCheckWorker;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 전체를 실제 MySQL(컨테이너)과 함께 확인한다. AI agent 만 가짜({@link StubAgent})로 바꾸고,
 * 워커는 자동 실행을 끄고 테스트에서 tick() 을 직접 부른다. Docker 가 필요하다.
 */
@SpringBootTest(properties = {
        "app.agent.mode=test",                 // upstage·fake 에이전트 빈을 만들지 않는다 → StubAgent 사용
        "app.agent.initial-delay-ms=3600000",  // 스케줄러가 끼어들지 않게
        "app.agent.poll-interval-ms=3600000",
        "app.agent.timeout=30m",
        "app.jwt.secret=" + OriginCheckApiIntegrationTest.SECRET,
        "app.jwt.expires-min=60",
})
@AutoConfigureMockMvc
@Testcontainers
class OriginCheckApiIntegrationTest {

    static final String SECRET = "integration-test-secret-integration-test-secret";

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static final StubAgent AGENT = new StubAgent();

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("origin-check-it");
        registry.add("app.storage.dir", dir::toString);
    }

    @TestConfiguration
    static class StubAgentConfig {
        @Bean
        CheckAgent checkAgent() {
            return AGENT;
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    OriginCheckWorker worker;
    @Autowired
    com.tradedoc.origincheck.check.OriginCheckRepository checkRepo;
    @Autowired
    com.tradedoc.origincheck.check.OriginCheckFileRepository fileRepo;
    @Autowired
    com.tradedoc.origincheck.check.storage.FileStorage storage;

    private static byte[] pdf;

    @BeforeEach
    void reset() throws Exception {
        AGENT.reset();
        if (pdf == null) {
            pdf = pdfWithPages(1);
        }
    }

    // ---------- 인증 ----------

    @Test
    void 가입하면_201과_토큰_내정보에_반영() throws Exception {
        String email = uniqueEmail();
        MvcResult r = mvc.perform(json(post("/api/v1/auth/signup"),
                        "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\",\"name\":\" 테스터 \",\"company\":\"사카밤바스피스\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andReturn();
        String token = body(r).get("access_token").asString();

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)) // 소문자로 저장
                .andExpect(jsonPath("$.name").value("테스터"))
                .andExpect(jsonPath("$.company").value("사카밤바스피스"))
                .andExpect(jsonPath("$.expires_at").exists());
    }

    @Test
    void 같은_이메일로는_다시_가입할_수_없다_대소문자_무시() throws Exception {
        String email = uniqueEmail();
        signup(email);
        mvc.perform(json(post("/api/v1/auth/signup"), "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\",\"name\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/signup"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void 가입_입력값_검증() throws Exception {
        mvc.perform(json(post("/api/v1/auth/signup"), "{\"email\":\"not-email\",\"password\":\"short\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(json(post("/api/v1/auth/signup"), "{깨진 json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 로그인_성공과_실패() throws Exception {
        String email = uniqueEmail();
        signup(email);

        mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty());
        mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"" + uniqueEmail() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 토큰이_없거나_틀리거나_만료되면_401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/origin-checks").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String expired = Jwts.builder().subject("1").issuer("fta-origin-check-api")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200))).expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void health는_인증_없이_열린다() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
    }

    // ---------- 업로드 검증 ----------

    @Test
    void 업로드_파일_검증() throws Exception {
        String token = signup(uniqueEmail());

        mvc.perform(upload(token, new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(upload(token, new MockMultipartFile("file", "a.pdf", "application/pdf", "not a pdf".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
        mvc.perform(upload(token, new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
        mvc.perform(multipart("/api/v1/origin-checks").param("title", "x").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
        mvc.perform(multipart("/api/v1/origin-checks").file(pdfFile("a.pdf")))
                .andExpect(status().isUnauthorized());
        // application/octet-stream 으로 와도 내용이 PDF 면 받는다
        mvc.perform(upload(token, new MockMultipartFile("file", "a.pdf", "application/octet-stream", pdf)))
                .andExpect(status().isAccepted());
    }

    // ---------- 비동기 처리 흐름 ----------

    @Test
    void 접수_제출_완료까지_한_건의_전체_흐름() throws Exception {
        String token = signup(uniqueEmail());

        // 1) 접수 → 202
        MvcResult created = mvc.perform(upload(token, pdfFile("사카밤바스피스_set04.pdf")).param("title", "9월 말레이시아 선적"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.matchesPattern(".*/api/v1/origin-checks/\\d+$")))
                .andReturn();
        long id = body(created).get("id").asLong();

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.title").value("9월 말레이시아 선적"))
                .andExpect(jsonPath("$.file_name").value("사카밤바스피스_set04.pdf"))
                .andExpect(jsonPath("$.job_id").doesNotExist())
                .andExpect(jsonPath("$.judge").doesNotExist())
                .andExpect(jsonPath("$.mismatches").isEmpty())
                .andExpect(jsonPath("$.extracted.인보이스_추출").isEmpty());

        // 2) 워커가 제출 → in_progress
        worker.tick();
        assertThat(AGENT.lastSubmittedName).isEqualTo("사카밤바스피스_set04.pdf");
        assertThat(Files.readAllBytes(AGENT.lastSubmittedPath)).isEqualTo(pdf); // 저장된 원본이 그대로 agent 로 간다
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.job_id").value(AGENT.lastJobId));

        // 3) agent 처리 중 → 그대로
        worker.tick();
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token)).andExpect(jsonPath("$.status").value("in_progress"));

        // 4) agent 완료 → 결과 전체를 한 번에
        AGENT.next = AgentPoll.completed(set04Result());
        worker.tick();
        assertThat(AGENT.deleted).contains("file_of_" + AGENT.lastJobId); // 끝나면 Upstage 쪽 파일을 지운다
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completed_at").exists())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.judge.decision").value("보완 요청"))
                .andExpect(jsonPath("$.judge.invoice_no").value("SM-2609-0044"))
                .andExpect(jsonPath("$.judge.mismatch_codes[0]").value("CO_INVOICE_NO_MISMATCH"))
                .andExpect(jsonPath("$.mismatches[0].stage").value(2))
                .andExpect(jsonPath("$.mismatches[0].label").value("원산지증명서 송장번호 ≠ 인보이스"))
                .andExpect(jsonPath("$.follow_up.kind").value("보완_요청_초안"))
                .andExpect(jsonPath("$.extracted.원산지증명서_추출[0].invoice_no").value("SM-2609-0815"))
                .andExpect(jsonPath("$.extracted.원산지증명서_추출[0].items[0].fob_value").value(35100.0))
                .andExpect(jsonPath("$.extracted_meta.원산지증명서_추출[0].reference_no.page").value(1))
                .andExpect(jsonPath("$.extracted_meta.원산지증명서_추출[0].reference_no.boxes[0].length()").value(4))
                .andExpect(jsonPath("$.extracted_meta.원산지증명서_추출[0].reference_no.low").doesNotExist());

        // 5) 목록·필터
        mvc.perform(auth(get("/api/v1/origin-checks").param("status", "completed"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].decision").value("보완 요청"))
                .andExpect(jsonPath("$.items[0].invoice_no").value("SM-2609-0044"))
                // 목록 요약: 완료될 때 추출 결과에서 뽑아 저장한 값
                .andExpect(jsonPath("$.items[0].consignee").value("KL HOME SUPPLY SDN. BHD."))
                .andExpect(jsonPath("$.items[0].primary_item").value("FROZEN MACKEREL"))
                .andExpect(jsonPath("$.items[0].item_count").value(1))
                .andExpect(jsonPath("$.items[0].destination_country").value("MALAYSIA"))
                .andExpect(jsonPath("$.items[0].shipped_on").value("2026-10-07"));
        mvc.perform(auth(get("/api/v1/origin-checks").param("status", "failed"), token))
                .andExpect(jsonPath("$.total_elements").value(0));

        // 6) 원본 PDF
        MvcResult file = mvc.perform(auth(get("/api/v1/origin-checks/" + id + "/file"), token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andReturn();
        assertThat(file.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    void 제출이_실패하면_failed와_AGENT_ERROR() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        AGENT.submitError = new AgentException("Upstage 파일 업로드 실패 (HTTP 403): No access");

        worker.tick();

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error.code").value("AGENT_ERROR"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("HTTP 403")))
                .andExpect(jsonPath("$.judge").doesNotExist());
    }

    @Test
    void agent가_실패로_끝나면_failed와_AGENT_FAILED() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        worker.tick();
        AGENT.next = AgentPoll.failed("Upstage 작업이 cancelled 상태로 끝났습니다");

        worker.tick();

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error.code").value("AGENT_FAILED"));
    }

    // ---------- 여러 파일 접수 ----------

    @Test
    void 여러_파일을_올리면_이어_붙여_한_건으로_점검한다() throws Exception {
        String token = signup(uniqueEmail());
        MvcResult created = mvc.perform(multipart("/api/v1/origin-checks")
                        .file(new MockMultipartFile("file", "CI_SM-2609-0044.pdf", MediaType.APPLICATION_PDF_VALUE, pdfWithPages(1)))
                        .file(new MockMultipartFile("file", "FORM_AK.pdf", MediaType.APPLICATION_PDF_VALUE, pdfWithPages(2)))
                        .file(new MockMultipartFile("file", "scan.pdf", MediaType.APPLICATION_PDF_VALUE, pdfWithPages(3)))
                        .param("hint", "인보이스", "원산지증명서", "zzz")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();
        long id = body(created).get("id").asLong();

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.file_name").value("CI_SM-2609-0044 외 2건.pdf"))
                .andExpect(jsonPath("$.title").value("CI_SM-2609-0044 외 2건.pdf"))
                .andExpect(jsonPath("$.files.length()").value(3))
                .andExpect(jsonPath("$.files[0].name").value("CI_SM-2609-0044.pdf"))
                .andExpect(jsonPath("$.files[0].page_from").value(1))
                .andExpect(jsonPath("$.files[0].page_to").value(1))
                .andExpect(jsonPath("$.files[0].hint_type").value("인보이스"))
                .andExpect(jsonPath("$.files[1].page_from").value(2))
                .andExpect(jsonPath("$.files[1].page_to").value(3))
                .andExpect(jsonPath("$.files[1].hint_type").value("원산지증명서"))
                .andExpect(jsonPath("$.files[2].page_from").value(4))
                .andExpect(jsonPath("$.files[2].page_to").value(6))
                .andExpect(jsonPath("$.files[2].hint_type").doesNotExist()); // 모르는 짐작 값은 버린다

        // agent 에는 이어 붙인 6쪽짜리 PDF 하나가 간다
        worker.tick();
        try (var d = org.apache.pdfbox.Loader.loadPDF(AGENT.lastSubmittedPath.toFile())) {
            assertThat(d.getNumberOfPages()).isEqualTo(6);
        }
        MvcResult file = mvc.perform(auth(get("/api/v1/origin-checks/" + id + "/file"), token)).andExpect(status().isOk()).andReturn();
        try (var d = org.apache.pdfbox.Loader.loadPDF(file.getResponse().getContentAsByteArray())) {
            assertThat(d.getNumberOfPages()).isEqualTo(6);
        }
    }

    @Test
    void 파일_하나는_그대로_쓰고_목록에_한_줄() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.file_name").value("a.pdf"))
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].page_from").value(1))
                .andExpect(jsonPath("$.files[0].page_to").value(1));
    }

    @Test
    void 파일은_한_번에_20개까지() throws Exception {
        String token = signup(uniqueEmail());
        var req = multipart("/api/v1/origin-checks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        for (int i = 0; i < 21; i++) {
            req.file(new MockMultipartFile("file", "f" + i + ".pdf", MediaType.APPLICATION_PDF_VALUE, pdf));
        }
        mvc.perform(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ---------- 담당자 완료 처리 ----------

    @Test
    void 완료_처리와_되돌리기() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);

        // 처리 중(queued)인 건은 완료 처리할 수 없다
        mvc.perform(auth(put("/api/v1/origin-checks/" + id + "/done"), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECK_NOT_FINISHED"));

        worker.tick();
        AGENT.next = AgentPoll.completed(set04Result());
        worker.tick();
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.done_at").doesNotExist());

        // 완료 처리 → 목록·상세에 done_at
        MvcResult done = mvc.perform(auth(put("/api/v1/origin-checks/" + id + "/done"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.done_at").exists())
                .andReturn();
        String doneAt = body(done).get("done_at").asString();
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.done_at").value(doneAt));
        mvc.perform(auth(get("/api/v1/origin-checks"), token))
                .andExpect(jsonPath("$.items[0].done_at").value(doneAt));

        // 다시 완료 처리해도 처음 시각을 유지한다
        mvc.perform(auth(put("/api/v1/origin-checks/" + id + "/done"), token))
                .andExpect(jsonPath("$.done_at").value(doneAt));

        // 남의 건은 404
        String other = signup(uniqueEmail());
        mvc.perform(auth(put("/api/v1/origin-checks/" + id + "/done"), other))
                .andExpect(status().isNotFound());

        // 되돌리기 → 진행 중
        mvc.perform(auth(delete("/api/v1/origin-checks/" + id + "/done"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done_at").doesNotExist());
        mvc.perform(auth(get("/api/v1/origin-checks"), token))
                .andExpect(jsonPath("$.items[0].done_at").doesNotExist());
    }

    // ---------- 다시 실행·삭제 ----------

    @Test
    void 실패한_건을_다시_실행하면_이전_agent_파일을_지우고_다시_제출해_완료() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        worker.tick(); // 제출
        String firstJob = AGENT.lastJobId;
        AGENT.next = AgentPoll.failed("Upstage 작업이 failed 상태로 끝났습니다");
        mvc.perform(auth(post("/api/v1/origin-checks/" + id + "/retry"), token))
                .andExpect(status().isConflict()) // 처리 중에는 다시 실행할 수 없다
                .andExpect(jsonPath("$.code").value("CHECK_NOT_FAILED"));
        worker.tick(); // 실패로 끝나고 같은 주기에 agent 파일도 정리된다

        mvc.perform(auth(post("/api/v1/origin-checks/" + id + "/retry"), token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("queued"));
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.error").doesNotExist());

        AGENT.next = AgentPoll.running();
        worker.tick(); // 다시 제출
        assertThat(AGENT.deleted).contains("file_of_" + firstJob);
        assertThat(AGENT.lastJobId).isNotEqualTo(firstJob);

        AGENT.next = AgentPoll.completed(set04Result());
        worker.tick();
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.judge.decision").exists());

        // 완료된 건도 다시 실행할 수 없다, 남의 건은 404
        mvc.perform(auth(post("/api/v1/origin-checks/" + id + "/retry"), token))
                .andExpect(status().isConflict());
        mvc.perform(auth(post("/api/v1/origin-checks/" + id + "/retry"), signup(uniqueEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void 삭제하면_원본_파일과_agent_파일까지_지우고_404() throws Exception {
        String token = signup(uniqueEmail());
        MvcResult r = mvc.perform(multipart("/api/v1/origin-checks")
                        .file(pdfFile("a.pdf")).file(pdfFile("b.pdf"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isAccepted()).andReturn();
        long id = body(r).get("id").asLong();
        worker.tick();
        AGENT.next = AgentPoll.completed(set04Result());
        worker.tick(); // 완료(agent 파일도 정리됨)
        java.util.List<Path> paths = new java.util.ArrayList<>();
        paths.add(storage.path(checkRepo.findById(id).orElseThrow().getFileKey()));
        fileRepo.findByCheckIdOrderBySeqAsc(id).forEach(f -> paths.add(storage.path(f.getFileKey())));
        assertThat(paths).hasSize(3).allMatch(Files::exists);

        // 남의 건은 404, 지워지지 않는다
        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), signup(uniqueEmail())))
                .andExpect(status().isNotFound());

        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isNoContent());

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isNotFound());
        assertThat(fileRepo.findByCheckIdOrderBySeqAsc(id)).isEmpty();
        assertThat(paths).noneMatch(Files::exists);
        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리_중인_건은_삭제할_수_없고_agent_파일을_못_지우면_502로_그대로_둔다() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        worker.tick(); // in_progress

        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECK_IN_PROGRESS"));

        AGENT.next = AgentPoll.failed("x");
        AGENT.deleteError = new AgentException("Upstage 파일 삭제 실패 (HTTP 500)");
        worker.tick(); // 실패로 끝남, agent 파일 정리도 실패해 남아 있다
        worker.tick();

        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AGENT_UNAVAILABLE"));
        mvc.perform(auth(get("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isOk());

        AGENT.deleteError = null;
        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isNoContent());
    }

    @Test
    void 접수_대기_중인_건은_삭제할_수_있다() throws Exception {
        String token = signup(uniqueEmail());
        long id = createCheck(token);
        Path file = storage.path(checkRepo.findById(id).orElseThrow().getFileKey());

        mvc.perform(auth(delete("/api/v1/origin-checks/" + id), token))
                .andExpect(status().isNoContent());
        worker.tick();
        // 다른 테스트가 남긴 접수 건은 제출될 수 있으니 이 건의 파일로 제출됐는지만 본다
        assertThat(AGENT.lastSubmittedPath).isNotEqualTo(file);
        assertThat(checkRepo.findById(id)).isEmpty();
        assertThat(file).doesNotExist();
    }

    // ---------- 목록·권한 ----------

    @Test
    void 목록은_최신순_페이지() throws Exception {
        String token = signup(uniqueEmail());
        long first = createCheck(token);
        long second = createCheck(token);
        long third = createCheck(token);

        mvc.perform(auth(get("/api/v1/origin-checks").param("size", "2"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(third))
                .andExpect(jsonPath("$.items[1].id").value(second))
                .andExpect(jsonPath("$.total_elements").value(3))
                .andExpect(jsonPath("$.total_pages").value(2));
        mvc.perform(auth(get("/api/v1/origin-checks").param("size", "2").param("page", "1"), token))
                .andExpect(jsonPath("$.items[0].id").value(first));
        mvc.perform(auth(get("/api/v1/origin-checks").param("status", "zzz"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(auth(get("/api/v1/origin-checks").param("page", "abc"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 남의_점검_건은_보이지_않는다() throws Exception {
        String owner = signup(uniqueEmail());
        String other = signup(uniqueEmail());
        long id = createCheck(owner);

        mvc.perform(auth(get("/api/v1/origin-checks/" + id), other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORIGIN_CHECK_NOT_FOUND"));
        mvc.perform(auth(get("/api/v1/origin-checks/" + id + "/file"), other))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/origin-checks"), other))
                .andExpect(jsonPath("$.total_elements").value(0));
        mvc.perform(auth(get("/api/v1/origin-checks/999999"), owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORIGIN_CHECK_NOT_FOUND"));
    }

    @Test
    void 없는_경로는_404_JSON() throws Exception {
        String token = signup(uniqueEmail());
        mvc.perform(auth(get("/api/v1/nope"), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---------- 도우미 ----------

    private String signup(String email) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/v1/auth/signup"),
                        "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"테스터\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return body(r).get("access_token").asString();
    }

    private long createCheck(String token) throws Exception {
        MvcResult r = mvc.perform(upload(token, pdfFile("a.pdf"))).andExpect(status().isAccepted()).andReturn();
        return body(r).get("id").asLong();
    }

    /** PDFBox 로 만든 빈 쪽 n 개짜리 진짜 PDF (서버가 쪽 수를 세고 이어 붙이므로 열리는 PDF 여야 한다) */
    private static byte[] pdfWithPages(int n) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument d = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            for (int i = 0; i < n; i++) {
                d.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            }
            d.save(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, pdf);
    }

    private static MockMultipartHttpServletRequestBuilder upload(String token, MockMultipartFile file) {
        return multipart("/api/v1/origin-checks").file(file).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode body(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private AgentCheckResult set04Result() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/check_result_set04.json")) {
            return mapper.readValue(in, AgentCheckResult.class);
        }
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /** 테스트가 결과를 정하는 가짜 AI agent */
    static class StubAgent implements CheckAgent {
        volatile AgentPoll next;
        volatile RuntimeException submitError;
        volatile Path lastSubmittedPath;
        volatile String lastSubmittedName;
        volatile String lastJobId;
        volatile RuntimeException deleteError;

        void reset() {
            deleteError = null;
            next = AgentPoll.running();
            submitError = null;
            lastSubmittedPath = null;
            lastSubmittedName = null;
            lastJobId = null;
        }

        final java.util.List<String> deleted = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public AgentSubmission submit(Path pdf, String fileName) {
            if (submitError != null) {
                throw submitError;
            }
            lastSubmittedPath = pdf;
            lastSubmittedName = fileName;
            lastJobId = "job_" + UUID.randomUUID();
            return new AgentSubmission(lastJobId, "file_of_" + lastJobId);
        }

        @Override
        public void deleteFile(String fileId) {
            if (deleteError != null) {
                throw deleteError;
            }
            deleted.add(fileId);
        }

        @Override
        public AgentPoll poll(String jobId) {
            return next;
        }
    }
}
