package com.tradedoc.origincheck.agent.dto.extraction;

/** 추출 값 정규화. 워크플로 규칙(없으면 "" / 0)을 null 이 와도 지키게 한다. */
final class Values {

    private Values() {
    }

    static String text(String v) {
        return v == null ? "" : v;
    }

    /** 빈 값은 그대로(서류 없음). 모르는 값은 확신이 없다는 뜻이라 unclear. */
    static String readability(String v) {
        if (v == null || v.isBlank()) {
            return "";
        }
        return "clear".equalsIgnoreCase(v.trim()) ? "clear" : "unclear";
    }

    /** Y · N · "" 만 둔다. 그 밖의 값은 "" (판정 불가). */
    static String fulfilled(String v) {
        if (v == null) {
            return "";
        }
        String s = v.trim().toUpperCase();
        return s.equals("Y") || s.equals("N") ? s : "";
    }
}
