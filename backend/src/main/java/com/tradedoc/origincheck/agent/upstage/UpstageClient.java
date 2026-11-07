package com.tradedoc.origincheck.agent.upstage;

import com.tradedoc.origincheck.agent.AgentException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Upstage Agents API(OpenAI 호환) 호출. 파이썬 코드에서 openai SDK 로 하던 세 가지를 그대로 옮겼다.
 * <ol>
 *   <li>files.create(purpose=user_data) — PDF 업로드</li>
 *   <li>responses.create(model=agent, input=[input_file], config_id) — 워크플로 실행(비동기, 바로 id 반환)</li>
 *   <li>responses.retrieve(id, include=["all"]) — 상태·모든 노드 출력 조회. 기본("last")은 마지막 노드만 준다</li>
 *   <li>files.delete(id) — 처리 끝난 파일 삭제</li>
 * </ol>
 */
public class UpstageClient {

    private final RestClient rest;
    private final UpstageProperties props;

    public UpstageClient(UpstageProperties props) {
        this.props = props;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMinutes(2));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey());
        // 파일 업로드·작업 생성이 같은 프로젝트에서 일어나야 한다. 키가 접근 못 하는 프로젝트면 403(project_access_forbidden)
        if (props.projectId() != null && !props.projectId().isBlank()) {
            builder.defaultHeader("x-project-id", props.projectId());
        }
        this.rest = builder.build();
    }

    /** @return 업로드된 파일 ID */
    public String uploadFile(Path pdf, String fileName) {
        // MultipartBodyBuilder 는 reactive-streams 가 필요해서 쓰지 않는다
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);
        fileHeaders.setContentDisposition(ContentDisposition.formData()
                .name("file").filename(fileName, StandardCharsets.UTF_8).build());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new FileSystemResource(pdf), fileHeaders));
        body.add("purpose", "user_data");
        Map<String, Object> resp = call("파일 업로드", () -> rest.post().uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(MAP));
        return requireId(resp, "파일 업로드");
    }

    /** @return responses.create 응답 (id, status 포함) */
    public Map<String, Object> createResponse(String fileId) {
        Map<String, Object> req = Map.of(
                "model", props.agentId(),
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "input_file", "file_id", fileId)))),
                "config_id", props.configId());
        Map<String, Object> resp = call("워크플로 실행", () -> rest.post().uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(MAP));
        requireId(resp, "워크플로 실행");
        return resp;
    }

    /** @return responses.retrieve(include=all) 응답 원문 */
    public Map<String, Object> retrieve(String responseId) {
        // openai SDK 는 배열 쿼리를 include[]=all 로 보낸다
        return call("결과 조회", () -> rest.get()
                .uri(b -> b.path("/responses/{id}").queryParam("include[]", "all").build(responseId))
                .retrieve()
                .body(MAP));
    }

    /** 파일 삭제. 이미 없으면(404) 성공으로 본다. Upstage 는 파일을 지울 때까지 보관한다. */
    public void deleteFile(String fileId) {
        try {
            rest.delete().uri("/files/{id}", fileId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return;
            }
            throw new AgentException("Upstage 파일 삭제 실패 (HTTP " + e.getStatusCode().value() + "): "
                    + abbreviate(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new AgentException("Upstage 파일 삭제 실패: " + e.getMessage(), e);
        }
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private static <T> T call(String what, Supplier<T> fn) {
        try {
            T result = fn.get();
            if (result == null) {
                throw new AgentException("Upstage " + what + " 응답이 비었습니다.");
            }
            return result;
        } catch (RestClientResponseException e) {
            throw new AgentException("Upstage " + what + " 실패 (HTTP " + e.getStatusCode().value() + "): "
                    + abbreviate(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new AgentException("Upstage " + what + " 실패: " + e.getMessage(), e);
        }
    }

    private static String requireId(Map<String, Object> resp, String what) {
        Object id = resp.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new AgentException("Upstage " + what + " 응답에 id 가 없습니다.");
        }
        return id.toString();
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500) + "…";
    }
}
