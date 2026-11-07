package com.tradedoc.origincheck.agent.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 응답 예시(set04)를 자바 DTO 로 읽고 다시 써도 같은 모양인지 본다. */
class AgentCheckResultJsonTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void set04_왕복() throws Exception {
        JsonNode original;
        try (InputStream in = getClass().getResourceAsStream("/check_result_set04.json")) {
            original = mapper.readTree(in);
        }

        AgentCheckResult r = mapper.treeToValue(original, AgentCheckResult.class);

        assertThat(r.judge().decision()).isEqualTo(Decision.SUPPLEMENT_REQUEST);
        assertThat(r.mismatches().get(0).stage()).isEqualTo(2);
        assertThat(r.followUp().kind()).isEqualTo(FollowUpKind.SUPPLEMENT_REQUEST_DRAFT);
        assertThat(r.extracted().certificatesOfOrigin().get(0).items().get(0).fobValue()).isEqualTo(35100);
        assertThat(r.extracted().invoices()).hasSize(1);
        assertThat(r.extractedMeta().certificatesOfOrigin().get(0).referenceNo().boxes().get(0)).hasSize(4);

        JsonNode written = mapper.valueToTree(r);
        assertThat(written.get("judge")).isEqualTo(original.get("judge"));
        assertThat(written.get("mismatches")).isEqualTo(original.get("mismatches"));
        assertThat(written.get("follow_up")).isEqualTo(original.get("follow_up"));
        String coo = ExtractNode.CERTIFICATE_OF_ORIGIN;
        assertThat(written.get("extracted_meta").get(coo).get(0).get("reference_no"))
                .isEqualTo(original.get("extracted_meta").get(coo).get(0).get("reference_no"));
        for (String key : ExtractNode.ALL) {
            assertThat(written.get("extracted").get(key).size()).as(key).isEqualTo(original.get("extracted").get(key).size());
            assertThat(written.get("extracted").get(key).get(0).propertyNames())
                    .as(key).containsExactlyInAnyOrderElementsOf(original.get("extracted").get(key).get(0).propertyNames());
        }
    }

    @Test
    void 코드표는_21개이고_단계가_1에서_3() {
        assertThat(MismatchCode.values()).hasSize(21);
        assertThat(MismatchCode.values()).allSatisfy(c -> assertThat(c.stage()).isBetween(1, 3));
        assertThat(AgentMismatch.of("UNKNOWN_CODE")).isEqualTo(new AgentMismatch("UNKNOWN_CODE", null, "UNKNOWN_CODE"));
    }
}
