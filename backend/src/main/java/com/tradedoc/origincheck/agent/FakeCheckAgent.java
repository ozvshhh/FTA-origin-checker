package com.tradedoc.origincheck.agent;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬·프론트 개발용. Upstage 를 부르지 않고, 제출 후 fake-delay 가 지나면 fake-result-path 의 결과를 돌려준다.
 * (app.agent.mode=fake)
 */
@Component
@ConditionalOnProperty(name = "app.agent.mode", havingValue = "fake")
public class FakeCheckAgent implements CheckAgent {

    private static final Logger log = LoggerFactory.getLogger(FakeCheckAgent.class);

    private final AgentProperties props;
    private final ObjectMapper objectMapper;
    private final Map<String, Instant> submittedAt = new ConcurrentHashMap<>();

    public FakeCheckAgent(AgentProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        log.warn("AI agent FAKE 모드: Upstage 를 호출하지 않고 {} 를 결과로 돌려줍니다.", props.fakeResultPath());
    }

    @Override
    public AgentSubmission submit(Path pdf, String fileName) {
        String jobId = "fake_" + UUID.randomUUID();
        submittedAt.put(jobId, Instant.now());
        return new AgentSubmission(jobId, null); // 올리는 파일이 없다
    }

    @Override
    public void deleteFile(String fileId) {
        // 지울 파일이 없다
    }

    @Override
    public AgentPoll poll(String jobId) {
        Instant at = submittedAt.get(jobId);
        if (at != null && Instant.now().isBefore(at.plus(props.fakeDelay()))) {
            return AgentPoll.running();
        }
        if (props.fakeResultPath() == null || props.fakeResultPath().isBlank()) {
            return AgentPoll.failed("AGENT_FAKE_RESULT 가 설정되지 않았습니다.");
        }
        try {
            String json = Files.readString(Path.of(props.fakeResultPath()));
            AgentCheckResult r = objectMapper.readValue(json, AgentCheckResult.class);
            submittedAt.remove(jobId);
            return AgentPoll.completed(new AgentCheckResult(jobId, r.judge(), r.mismatches(), r.followUp(),
                    r.extracted(), r.extractedMeta(), r.error()));
        } catch (Exception e) {
            return AgentPoll.failed("fake 결과 파일을 읽지 못했습니다: " + e.getMessage());
        }
    }
}
