package com.tradedoc.origincheck.common;

import org.springframework.data.domain.Page;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 응답.
 *
 * @param items         이번 페이지 항목
 * @param page          페이지 번호(0부터)
 * @param size          페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages    전체 페이지 수
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
