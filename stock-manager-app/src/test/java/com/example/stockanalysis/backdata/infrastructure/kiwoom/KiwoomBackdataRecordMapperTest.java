package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.backdata.validation.internal.DefaultBackdataValidationEngine;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import com.example.stockanalysis.kiwoom.contract.tr.ka10081.Ka10081Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10086.Ka10086Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KiwoomBackdataRecordMapperTest {

    private final KiwoomBackdataCatalog catalog = new KiwoomBackdataCatalog();
    private final KiwoomBackdataRecordMapper mapper =
            new KiwoomBackdataRecordMapper(new ObjectMapper());

    @Test
    void mapsKiwoomDailyCandleToCanonicalRecord() {
        Ka10081Response response = Ka10081Response.builder()
                .stkCd("005930")
                .stkDtPoleChartQry(List.of(
                        Ka10081Response.StkDtPoleChartQryItem.builder()
                                .dt("20250103")
                                .openPric("-55000")
                                .highPric("+57000")
                                .lowPric("-54500")
                                .curPrc("+56500")
                                .trdeQty("1,000,000")
                                .trdePrica("56,000,000,000")
                                .build()
                ))
                .returnCode(0)
                .build();

        var records = mapper.map(
                catalog.getRequired("ka10081"),
                Map.of("stk_cd", "005930"),
                response,
                LocalDate.parse("2025-01-03"),
                Instant.parse("2025-01-03T08:00:00Z")
        );

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.stockCode()).isEqualTo("005930");
            assertThat(record.businessDate()).isEqualTo(LocalDate.parse("2025-01-03"));
            assertThat(record.value("open")).isEqualTo("55000");
            assertThat(record.value("high")).isEqualTo("57000");
            assertThat(record.value("low")).isEqualTo("54500");
            assertThat(record.value("close")).isEqualTo("56500");
            assertThat(record.value("volume")).isEqualTo("1000000");
            assertThat(record.value("tradingAmount")).isEqualTo("56000000000");
        });
    }

    @Test
    void normalizesRepeatedSignsInKiwoomDailyInvestorValues() {
        Ka10086Response response = Ka10086Response.builder()
                .dalyStkpc(List.of(
                        Ka10086Response.DalyStkpcItem.builder()
                                .date("20250103")
                                .openPric("+55000")
                                .highPric("+57000")
                                .lowPric("-54500")
                                .closePric("+56500")
                                .trdeQty("1,000,000")
                                .ind("--3740211")
                                .orgn("+2135737")
                                .frgn("--368595")
                                .prm("+2249798")
                                .forNetprps("+1326634")
                                .orgnNetprps("+2135737")
                                .indNetprps("--3740211")
                                .build()
                ))
                .returnCode(0)
                .build();

        var records = mapper.map(
                catalog.getRequired("ka10086"),
                Map.of("stk_cd", "005930"),
                response,
                LocalDate.parse("2025-01-03"),
                Instant.parse("2025-01-03T08:00:00Z")
        );

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.value("individual")).isEqualTo("-3740211");
            assertThat(record.value("institution")).isEqualTo("2135737");
            assertThat(record.value("foreigner")).isEqualTo("-368595");
            assertThat(record.value("program")).isEqualTo("2249798");
            assertThat(record.value("foreignNetBuy")).isEqualTo("1326634");
            assertThat(record.value("institutionNetBuy")).isEqualTo("2135737");
            assertThat(record.value("individualNetBuy")).isEqualTo("-3740211");
        });

        var validationResult = new DefaultBackdataValidationEngine(catalog.schemaRegistry())
                .validate(new ValidationCommand(
                        ValidationProfile.BACKTEST_READY,
                        Instant.parse("2025-01-03T08:01:00Z"),
                        records
                ));

        assertThat(validationResult.issues())
                .noneMatch(issue -> issue.code().equals("INVALID_NUMBER"));
        assertThat(validationResult.isBacktestReady()).isTrue();
    }

    @Test
    void preservesConflictingLeadingSignsAsInvalidInput() {
        Ka10086Response response = Ka10086Response.builder()
                .dalyStkpc(List.of(
                        Ka10086Response.DalyStkpcItem.builder()
                                .date("20250103")
                                .ind("+-100")
                                .build()
                ))
                .returnCode(0)
                .build();

        var records = mapper.map(
                catalog.getRequired("ka10086"),
                Map.of("stk_cd", "005930"),
                response,
                LocalDate.parse("2025-01-03"),
                Instant.parse("2025-01-03T08:00:00Z")
        );

        assertThat(records).singleElement().satisfies(record ->
                assertThat(record.value("individual")).isEqualTo("+-100")
        );
    }
}
