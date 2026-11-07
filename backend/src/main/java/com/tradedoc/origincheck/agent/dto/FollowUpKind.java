package com.tradedoc.origincheck.agent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

/** 후속 노드 이름(Studio 워크플로 노드 이름 그대로). 특혜 적용은 후속 노드가 없다. */
public enum FollowUpKind {
    SUPPLEMENT_REQUEST_DRAFT("보완_요청_초안"),
    NOT_APPLICABLE_NOTICE("미적용_안내"),
    REVIEW_MEMO("검토_메모"),
    UNCLASSIFIED_NOTICE("미분류_안내");

    private final String nodeName;

    FollowUpKind(String nodeName) {
        this.nodeName = nodeName;
    }

    @JsonValue
    public String nodeName() {
        return nodeName;
    }

    @JsonCreator
    public static FollowUpKind fromNodeName(String value) {
        return findIn(value).orElseThrow(() -> new IllegalArgumentException("알 수 없는 후속 노드: " + value));
    }

    /** Upstage 출력의 model 이름(예: 'Instruct - 보완_요청_초안')에 들어 있는 후속 노드를 찾는다. */
    public static Optional<FollowUpKind> findIn(String text) {
        if (text != null) {
            for (FollowUpKind k : values()) {
                if (text.contains(k.nodeName)) {
                    return Optional.of(k);
                }
            }
        }
        return Optional.empty();
    }
}
