package com.tradedoc.origincheck.agent.upstage;

import com.tradedoc.origincheck.agent.AgentException;
import com.tradedoc.origincheck.agent.AgentPoll;
import com.tradedoc.origincheck.agent.AgentSubmission;
import com.tradedoc.origincheck.agent.CheckAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

/** Upstage Studio 워크플로로 점검한다. (app.agent.mode=upstage, 기본값) */
@Component
@ConditionalOnProperty(name = "app.agent.mode", havingValue = "upstage", matchIfMissing = true)
public class UpstageCheckAgent implements CheckAgent {

    private static final Logger log = LoggerFactory.getLogger(UpstageCheckAgent.class);

    private final UpstageClient client;
    private final UpstageResponseParser parser;
    private final boolean configured;

    public UpstageCheckAgent(UpstageProperties props, UpstageResponseParser parser) {
        // 키가 없어도 서버는 뜬다(동료 파이썬 버전과 같은 동작). 제출할 때 AGENT_ERROR 로 알려 준다
        this.configured = props.apiKey() != null && !props.apiKey().isBlank();
        if (!configured) {
            log.warn("UPSTAGE_API_KEY 가 없습니다. 점검을 제출하면 실패합니다. .env 에 넣거나 AGENT_MODE=fake 로 실행하세요.");
        }
        this.client = new UpstageClient(props);
        this.parser = parser;
    }

    @Override
    public AgentSubmission submit(Path pdf, String fileName) {
        if (!configured) {
            throw new AgentException("UPSTAGE_API_KEY 가 설정되지 않았습니다.");
        }
        String fileId = client.uploadFile(pdf, fileName);
        try {
            return new AgentSubmission(client.createResponse(fileId).get("id").toString(), fileId);
        } catch (RuntimeException e) {
            deleteQuietly(fileId); // 작업을 못 만들었으면 올린 파일이 남지 않게
            throw e;
        }
    }

    @Override
    public void deleteFile(String fileId) {
        client.deleteFile(fileId);
    }

    private void deleteQuietly(String fileId) {
        try {
            client.deleteFile(fileId);
        } catch (RuntimeException e) {
            log.warn("Upstage 파일 {} 삭제 실패: {}", fileId, e.getMessage());
        }
    }

    @Override
    public AgentPoll poll(String jobId) {
        Map<String, Object> resp = client.retrieve(jobId);
        String status = String.valueOf(resp.get("status"));
        return switch (status) {
            case "queued", "in_progress" -> AgentPoll.running();
            case "completed" -> AgentPoll.completed(parser.toCheckResult(resp));
            // failed · cancelled · incomplete
            default -> AgentPoll.failed("Upstage 작업이 " + status + " 상태로 끝났습니다"
                    + errorSuffix(resp.get("error")));
        };
    }

    private static String errorSuffix(Object error) {
        if (error instanceof Map<?, ?> m && m.get("message") != null) {
            return ": " + m.get("message");
        }
        return error == null ? "" : ": " + error;
    }
}
