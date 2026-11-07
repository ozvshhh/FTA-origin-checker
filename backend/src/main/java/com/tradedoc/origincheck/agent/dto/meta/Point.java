package com.tradedoc.origincheck.agent.dto.meta;

/**
 * 페이지 크기 대비 비율 좌표 (0~1). 왼쪽 위가 (0, 0).
 *
 * @param x 가로 위치. 페이지 너비 대비 0~1
 * @param y 세로 위치. 페이지 높이 대비 0~1
 */
public record Point(double x, double y) {
}
