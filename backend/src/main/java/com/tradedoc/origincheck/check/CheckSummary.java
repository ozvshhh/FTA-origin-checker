package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.agent.dto.AgentExtracted;
import com.tradedoc.origincheck.agent.dto.extraction.InvoiceExtraction;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 목록 한 줄에 보여 줄 요약. 점검이 완료될 때 추출 결과에서 한 번 뽑아 엔티티 컬럼에 저장한다.
 * 값이 없으면 각 필드는 null.
 *
 * @param consignee          인보이스 consignee(바이어)
 * @param primaryItem        인보이스 첫 품목의 품명
 * @param itemCount          인보이스 품목 수
 * @param destinationCountry 수출신고필증 목적국, 없으면 원산지증명서 수입국(영문 원문)
 * @param shippedOn          B/L 본선 적재일, 없으면 원산지증명서 출항일
 */
public record CheckSummary(
        String consignee,
        String primaryItem,
        Integer itemCount,
        String destinationCountry,
        LocalDate shippedOn
) {
    static final int TEXT_MAX = 255;
    static final int COUNTRY_MAX = 100;

    public static final CheckSummary EMPTY = new CheckSummary(null, null, null, null, null);

    public static CheckSummary from(AgentCheckResult r) {
        if (r == null || r.extracted() == null) {
            return EMPTY;
        }
        AgentExtracted x = r.extracted();
        InvoiceExtraction inv = first(x.invoices());
        String consignee = null;
        String primaryItem = null;
        Integer itemCount = null;
        if (inv != null) {
            consignee = text(inv.consignee(), TEXT_MAX);
            if (!inv.items().isEmpty()) {
                primaryItem = text(inv.items().getFirst().description(), TEXT_MAX);
                itemCount = inv.items().size();
            }
        }
        String country = firstText(COUNTRY_MAX,
                pick(x.exportDeclarations(), e -> e.destinationCountry()),
                pick(x.certificatesOfOrigin(), c -> c.importingCountry()));
        LocalDate shippedOn = parseDate(pick(x.billsOfLading(), b -> b.ladenOnBoardDate()));
        if (shippedOn == null) {
            shippedOn = parseDate(pick(x.certificatesOfOrigin(), c -> c.departureDate()));
        }
        return new CheckSummary(consignee, primaryItem, itemCount, country, shippedOn);
    }

    // --- 날짜 정규화 ---

    private static final Pattern NUMERIC = Pattern.compile("(\\d{4})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})\\s*일?\\.?");
    private static final Pattern COMPACT = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private static final List<DateTimeFormatter> ENGLISH = List.of(
            english("d MMM uuuu"),   // 07 OCT 2026
            english("d-MMM-uuuu"),   // 07-OCT-2026
            english("MMM d uuuu"),   // OCT 07 2026 (쉼표·점은 미리 뺀다)
            english("d MMMM uuuu"),  // 7 OCTOBER 2026
            english("MMMM d uuuu")); // OCTOBER 7 2026

    private static DateTimeFormatter english(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH);
    }

    /** 워크플로는 YYYY-MM-DD 로 주기로 했지만 문서 표기 그대로 오는 경우를 대비해 흔한 형식을 맞춘다. 못 맞추면 null */
    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            Matcher m = NUMERIC.matcher(s);
            if (m.matches()) {
                return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
            m = COMPACT.matcher(s);
            if (m.matches()) {
                return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
        } catch (DateTimeException e) {
            return null; // 2026-13-45 같은 없는 날짜
        }
        String words = s.replaceAll("[,.]", " ").replaceAll("\\s+", " ").trim();
        for (DateTimeFormatter f : ENGLISH) {
            try {
                return LocalDate.parse(words, f);
            } catch (DateTimeParseException ignored) {
                // 다음 형식
            }
        }
        return null;
    }

    // --- 도우미 ---

    private static <T> T first(List<T> list) {
        return list == null || list.isEmpty() ? null : list.getFirst();
    }

    private static <T> String pick(List<T> list, Function<T, String> field) {
        T v = first(list);
        return v == null ? null : field.apply(v);
    }

    private static String firstText(int max, String... values) {
        for (String v : values) {
            String t = text(v, max);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private static String text(String v, int max) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
