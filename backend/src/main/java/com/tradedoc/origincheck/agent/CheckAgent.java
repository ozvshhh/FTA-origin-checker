package com.tradedoc.origincheck.agent;

import java.nio.file.Path;

/**
 * 원산지 점검 AI agent. 제출과 결과 조회가 따로인 비동기 작업이다.
 * 구현: {@link com.tradedoc.origincheck.agent.upstage.UpstageCheckAgent}(실제), {@link FakeCheckAgent}(로컬 개발용).
 */
public interface CheckAgent {

    /** PDF 를 제출하고 작업 ID·올린 파일 ID 를 돌려준다. */
    AgentSubmission submit(Path pdf, String fileName);

    /** 작업 상태를 조회한다. 끝났으면 결과(또는 실패 사유)가 들어 있다. */
    AgentPoll poll(String jobId);

    /** 처리가 끝난 파일을 agent 쪽에서 지운다. 이미 없으면 성공으로 본다. */
    void deleteFile(String fileId);
}
