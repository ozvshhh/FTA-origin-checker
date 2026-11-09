package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.agent.dto.AgentCheckResult;
import com.tradedoc.origincheck.agent.dto.AgentExtracted;
import com.tradedoc.origincheck.agent.dto.extraction.BillOfLadingExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.CertificateOfOriginExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.ExportDeclarationExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.InvoiceExtraction;
import com.tradedoc.origincheck.agent.dto.extraction.InvoiceItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckSummaryTest {

    @Test
    void 인보이스_수출신고필증_BL에서_요약을_뽑는다() {
        CheckSummary s = CheckSummary.from(result(
                invoice("  KL HOME SUPPLY SDN. BHD. ", "FROZEN MACKEREL", "SQUID"),
                bl("2026-10-07"),
                coo("VIETNAM", "2026-10-09"),
                export("MALAYSIA")));

        assertThat(s.consignee()).isEqualTo("KL HOME SUPPLY SDN. BHD.");
        assertThat(s.primaryItem()).isEqualTo("FROZEN MACKEREL");
        assertThat(s.itemCount()).isEqualTo(2);
        assertThat(s.destinationCountry()).isEqualTo("MALAYSIA");   // 수출신고필증이 우선
        assertThat(s.shippedOn()).isEqualTo(LocalDate.of(2026, 10, 7)); // B/L 이 우선
    }

    @Test
    void 수출신고필증_BL이_없으면_원산지증명서_값을_쓴다() {
        CheckSummary s = CheckSummary.from(result(
                invoice("BUYER"), bl(""), coo("VIETNAM", "2026/10/09"), export("")));

        assertThat(s.destinationCountry()).isEqualTo("VIETNAM");
        assertThat(s.shippedOn()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    @Test
    void 값이_없으면_모두_null() {
        CheckSummary s = CheckSummary.from(result(invoice(""), bl("unknown"), coo("", ""), export(" ")));

        assertThat(s).isEqualTo(CheckSummary.EMPTY);
        assertThat(CheckSummary.from(new AgentCheckResult("job", null, null, null, null, null, null)))
                .isEqualTo(CheckSummary.EMPTY);
        assertThat(CheckSummary.from(null)).isEqualTo(CheckSummary.EMPTY);
    }

    @Test
    void 긴_값은_컬럼_길이로_자른다() {
        String longName = "A".repeat(300);
        CheckSummary s = CheckSummary.from(result(invoice(longName, longName), null, null, null));

        assertThat(s.consignee()).hasSize(CheckSummary.TEXT_MAX);
        assertThat(s.primaryItem()).hasSize(CheckSummary.TEXT_MAX);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-10-07|2026-10-07",
            "2026/10/07|2026-10-07",
            "2026.10.07|2026-10-07",
            "2026. 10. 7.|2026-10-07",
            "2026년 10월 7일|2026-10-07",
            "20261007|2026-10-07",
            "07 OCT 2026|2026-10-07",
            "07-Oct-2026|2026-10-07",
            "OCT. 07, 2026|2026-10-07",
            "October 7, 2026|2026-10-07",
            "7 OCTOBER 2026|2026-10-07",
    })
    void 흔한_날짜_표기를_YYYY_MM_DD로_맞춘다(String raw, LocalDate expected) {
        assertThat(CheckSummary.parseDate(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "2026-13-45", "10/07/2026", "ASAP", "2026-02-30"})
    void 맞출_수_없는_날짜는_null(String raw) {
        assertThat(CheckSummary.parseDate(raw)).isNull();
    }

    // --- 만들기 ---

    private static AgentCheckResult result(InvoiceExtraction inv, BillOfLadingExtraction bl,
                                           CertificateOfOriginExtraction coo, ExportDeclarationExtraction exp) {
        AgentExtracted x = new AgentExtracted(list(inv), null, list(bl), list(coo), null, list(exp));
        return new AgentCheckResult("job", null, null, null, x, null, null);
    }

    private static <T> List<T> list(T v) {
        return v == null ? null : List.of(v);
    }

    private static InvoiceExtraction invoice(String consignee, String... items) {
        List<InvoiceItem> lines = java.util.Arrays.stream(items).map(d -> new InvoiceItem(d, null, null, null)).toList();
        return new InvoiceExtraction(null, null, null, consignee, null, null, null, null, null, null, null, null, lines);
    }

    private static BillOfLadingExtraction bl(String ladenOnBoard) {
        return new BillOfLadingExtraction(null, null, null, null, null, null, null, null, null, ladenOnBoard, null);
    }

    private static CertificateOfOriginExtraction coo(String importingCountry, String departure) {
        return new CertificateOfOriginExtraction(null, null, null, null, departure, null, null, null, null, null,
                importingCountry, null, null, null, null);
    }

    private static ExportDeclarationExtraction export(String destination) {
        return new ExportDeclarationExtraction(null, null, null, null, null, null, destination, null, null, null, null, null);
    }
}
