package com.example.stockanalysis.backdata.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import com.example.stockanalysis.backdata.validation.model.ValidationResult;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import com.example.stockanalysis.backdata.validation.model.ValidationSummary;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.tr.ka10081.Ka10081Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackdataValidationResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preservesEveryKiwoomResponseFieldWithoutConvertingIt() {
        Ka10081Response kiwoomResponse = Ka10081Response.builder()
                .stkCd("005930")
                .stkDtPoleChartQry(List.of(
                        Ka10081Response.StkDtPoleChartQryItem.builder()
                                .curPrc("+70500")
                                .trdeQty("1000000")
                                .trdePrica("70500")
                                .dt("20260731")
                                .openPric("+70000")
                                .highPric("+71000")
                                .lowPric("-69500")
                                .predPre("+500")
                                .predPreSig("2")
                                .trdeTernRt("1.25")
                                .build()
                ))
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
        ValidationResult result = validResult();

        BackdataValidationRunResponse response = BackdataValidationRunResponse.of(
                UUID.randomUUID(),
                "ka10081",
                "주식일봉차트조회요청",
                BackdataDatasetType.DAILY_CANDLE,
                ValidationProfile.BACKTEST_READY,
                Instant.parse("2026-07-31T07:00:00Z"),
                result,
                kiwoomResponse,
                KiwoomContinuation.none()
        );

        JsonNode responseJson = objectMapper.valueToTree(response);
        assertThat(responseJson.get("kiwoomResponse"))
                .isEqualTo(objectMapper.valueToTree(kiwoomResponse));
        assertThat(responseJson.has("validRecords")).isFalse();
        assertThat(responseJson.has("quarantinedRecords")).isFalse();
        assertThat(response.validationStatus()).isEqualTo(BackdataValidationStatus.VALID);
    }

    @Test
    void batchSourceAlsoKeepsTheConcreteKiwoomDto() {
        Ka10081Response kiwoomResponse = Ka10081Response.builder()
                .stkCd("005930")
                .stkDtPoleChartQry(List.of())
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
        BackdataSourceCallResponse source = new BackdataSourceCallResponse(
                0,
                "ka10081",
                "주식일봉차트조회요청",
                BackdataDatasetType.DAILY_CANDLE,
                0,
                BackdataValidationStatus.INVALID,
                List.of(),
                kiwoomResponse,
                KiwoomContinuation.none()
        );

        JsonNode sourceJson = objectMapper.valueToTree(source);

        assertThat(sourceJson.get("kiwoomResponse"))
                .isEqualTo(objectMapper.valueToTree(kiwoomResponse));
    }

    private ValidationResult validResult() {
        ValidationSummary summary = new ValidationSummary(
                1,
                1,
                0,
                0,
                Map.of(
                        ValidationSeverity.INFO, 0L,
                        ValidationSeverity.WARNING, 0L,
                        ValidationSeverity.ERROR, 0L,
                        ValidationSeverity.FATAL, 0L
                )
        );
        return new ValidationResult(summary, List.of(), List.of(), List.of());
    }
}
