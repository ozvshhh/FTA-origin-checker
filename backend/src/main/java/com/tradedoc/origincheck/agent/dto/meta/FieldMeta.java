package com.tradedoc.origincheck.agent.dto.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 추출 값 하나의 신뢰도와 문서상 위치.
 *
 * @param confidence 신뢰도 등급. 예시 값은 "high", "medium", "low"
 * @param score      신뢰도 점수 (0~1). 없으면 null
 * @param page       값이 있는 페이지(1부터, 묶음 PDF 기준). 위치가 없으면 -1
 * @param boxes      값이 적힌 영역. 단어마다 꼭짓점 4개(왼쪽 위부터 시계 방향)의 다각형
 */
public record FieldMeta(String confidence, Double score, Integer page, List<List<Point>> boxes) {

    public FieldMeta {
        boxes = boxes == null ? List.of() : List.copyOf(boxes);
    }

    @JsonIgnore
    public boolean isLow() {
        return "low".equalsIgnoreCase(confidence);
    }
}
