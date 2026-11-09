package com.tradedoc.origincheck.agent.upstage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tradedoc.origincheck.agent.AgentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Upstage 로 실제로 무엇을 보내는지 로컬 가짜 서버로 확인한다.
 * (실제 호출에서 잡힌 문제: multipart 생성 실패, x-project-id 누락 시 403 No access to file)
 */
class UpstageClientTest {

    record Recorded(String method, String path, String query, Map<String, List<String>> headers, byte[] body) {
        String header(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(e -> e.getValue().get(0)).findFirst().orElse(null);
        }

        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private HttpServer server;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String responseBody = "{}";
    /** "METHOD /path" 별 응답 {상태, 본문}. 없으면 status·responseBody */
    private final Map<String, Object[]> routes = new java.util.concurrent.ConcurrentHashMap<>();

    @TempDir
    Path tmp;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        byte[] body = ex.getRequestBody().readAllBytes();
        requests.add(new Recorded(ex.getRequestMethod(), ex.getRequestURI().getPath(), ex.getRequestURI().getRawQuery(),
                Map.copyOf(ex.getRequestHeaders()), body));
        Object[] route = routes.getOrDefault(ex.getRequestMethod() + " " + ex.getRequestURI().getPath(),
                new Object[]{status, responseBody});
        byte[] out = ((String) route[1]).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders((int) route[0], out.length == 0 ? -1 : out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    private UpstageClient client(String projectId) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v2";
        return new UpstageClient(new UpstageProperties(base, "up_test_key", projectId, "agt_test", "5"));
    }

    @Test
    void 파일_업로드는_multipart_로_file과_purpose를_보낸다() throws IOException {
        Path pdf = Files.write(tmp.resolve("stored.pdf"), "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
        responseBody = "{\"id\": \"file_123\"}";

        String id = client("proj_test").uploadFile(pdf, "원본 서류.pdf");

        assertThat(id).isEqualTo("file_123");
        Recorded r = requests.get(0);
        assertThat(r.method()).isEqualTo("POST");
        assertThat(r.path()).isEqualTo("/v2/files");
        assertThat(r.header("Content-Type")).startsWith("multipart/form-data");
        assertThat(r.header("Authorization")).isEqualTo("Bearer up_test_key");
        assertThat(r.header("x-project-id")).isEqualTo("proj_test");
        String body = r.bodyText();
        assertThat(body).contains("name=\"file\"").contains("application/pdf").contains("%PDF-1.4 test");
        assertThat(body).contains("name=\"purpose\"").contains("user_data");
        // 한글 파일명은 UTF-8 그대로(ASCII 대체값 "_____.pdf" 가 아니라)
        assertThat(body).contains("filename=\"원본 서류.pdf\"").doesNotContain("filename*=").doesNotContain("_____");
    }

    @Test
    void 프로젝트_ID가_비면_x_project_id_헤더를_보내지_않는다() throws IOException {
        Path pdf = Files.write(tmp.resolve("a.pdf"), "%PDF".getBytes(StandardCharsets.UTF_8));
        responseBody = "{\"id\": \"file_1\"}";

        client("").uploadFile(pdf, "a.pdf");

        assertThat(requests.get(0).header("x-project-id")).isNull();
    }

    @Test
    void 작업_생성은_agent_config_input_file을_보낸다() {
        responseBody = "{\"id\": \"job_1\", \"status\": \"queued\"}";

        Map<String, Object> resp = client("proj_test").createResponse("file_123");

        assertThat(resp).containsEntry("id", "job_1");
        Recorded r = requests.get(0);
        assertThat(r.path()).isEqualTo("/v2/responses");
        assertThat(r.header("Content-Type")).startsWith("application/json");
        assertThat(r.header("x-project-id")).isEqualTo("proj_test");
        assertThat(r.bodyText())
                .contains("\"model\":\"agt_test\"")
                .contains("\"config_id\":\"5\"")
                .contains("\"type\":\"input_file\"")
                .contains("\"file_id\":\"file_123\"")
                .contains("\"role\":\"user\"");
    }

    @Test
    void 결과_조회는_include_all로_모든_노드를_요청한다() {
        responseBody = "{\"id\": \"job_1\", \"status\": \"in_progress\"}";

        Map<String, Object> resp = client("proj_test").retrieve("job_1");

        assertThat(resp).containsEntry("status", "in_progress");
        Recorded r = requests.get(0);
        assertThat(r.method()).isEqualTo("GET");
        assertThat(r.path()).isEqualTo("/v2/responses/job_1");
        assertThat(URLDecoder.decode(r.query(), StandardCharsets.UTF_8)).isEqualTo("include[]=all");
    }

    @Test
    void 오류_응답은_상태코드와_본문을_담은_AgentException() {
        status = 403;
        responseBody = "{\"error\":{\"message\":\"No access to file: file_1\"}}";

        assertThatThrownBy(() -> client("proj_test").createResponse("file_1"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("HTTP 403")
                .hasMessageContaining("No access to file")
                .satisfies(e -> assertThat(((AgentException) e).isRetryable()).as("4xx 는 다시 해도 같다").isFalse());
    }

    @Test
    void 서버_오류와_429는_재시도_대상() {
        for (int code : new int[]{500, 503, 429}) {
            status = code;
            responseBody = "{}";
            assertThatThrownBy(() -> client("proj_test").createResponse("file_1"))
                    .isInstanceOf(AgentException.class)
                    .satisfies(e -> assertThat(((AgentException) e).isRetryable()).as("HTTP " + code).isTrue());
        }
    }

    @Test
    void 연결_실패는_재시도_대상() {
        server.stop(0);
        assertThatThrownBy(() -> client("proj_test").createResponse("file_1"))
                .isInstanceOf(AgentException.class)
                .satisfies(e -> assertThat(((AgentException) e).isRetryable()).isTrue());
    }

    @Test
    void 파일명의_따옴표와_줄바꿈은_이스케이프한다() {
        assertThat(UpstageClient.formFileName("a\"b\r\n.pdf")).isEqualTo("a%22b%0D%0A.pdf");
        assertThat(UpstageClient.formFileName(" ")).isEqualTo("document.pdf");
    }

    @Test
    void 응답에_id가_없으면_AgentException() {
        responseBody = "{\"status\": \"queued\"}";

        assertThatThrownBy(() -> client("proj_test").createResponse("file_1"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void 파일_삭제는_DELETE_files_id() {
        client("proj_test").deleteFile("file_9");

        Recorded r = requests.get(0);
        assertThat(r.method()).isEqualTo("DELETE");
        assertThat(r.path()).isEqualTo("/v2/files/file_9");
        assertThat(r.header("x-project-id")).isEqualTo("proj_test");
    }

    @Test
    void 이미_없는_파일_삭제는_성공으로_본다() {
        status = 404;
        responseBody = "{\"error\":{\"message\":\"not found\"}}";

        client("proj_test").deleteFile("file_9"); // 예외 없음
    }

    @Test
    void 파일_삭제_서버_오류는_AgentException() {
        status = 500;

        assertThatThrownBy(() -> client("proj_test").deleteFile("file_9"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void 작업_생성에_실패하면_올린_파일을_지운다() throws IOException {
        Path pdf = Files.write(tmp.resolve("a.pdf"), "%PDF".getBytes(StandardCharsets.UTF_8));
        routes.put("POST /v2/files", new Object[]{200, "{\"id\": \"file_77\"}"});
        routes.put("POST /v2/responses", new Object[]{403, "{\"error\":{\"message\":\"No access to file\"}}"});
        routes.put("DELETE /v2/files/file_77", new Object[]{200, "{\"deleted\": true}"});
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v2";
        var agent = new UpstageCheckAgent(new UpstageProperties(base, "up_test_key", "proj_test", "agt_test", "5"),
                new UpstageResponseParser(tools.jackson.databind.json.JsonMapper.builder().build()));

        assertThatThrownBy(() -> agent.submit(pdf, "a.pdf")).isInstanceOf(AgentException.class).hasMessageContaining("403");

        assertThat(requests).extracting(r -> r.method() + " " + r.path())
                .containsExactly("POST /v2/files", "POST /v2/responses", "DELETE /v2/files/file_77");
    }

    @Test
    void 키가_없으면_서버는_뜨고_제출할_때_실패한다() {
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v2";
        var agent = new UpstageCheckAgent(new UpstageProperties(base, "", "proj_test", "agt_test", "5"),
                new UpstageResponseParser(tools.jackson.databind.json.JsonMapper.builder().build()));

        assertThatThrownBy(() -> agent.submit(tmp.resolve("a.pdf"), "a.pdf"))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("UPSTAGE_API_KEY");
        assertThat(requests).isEmpty();
    }
}
