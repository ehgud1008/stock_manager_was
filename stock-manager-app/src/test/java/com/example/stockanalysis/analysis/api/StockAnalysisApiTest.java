package com.example.stockanalysis.analysis.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.stockanalysis.analysis.application.AnalysisExecutionCommand;
import com.example.stockanalysis.analysis.application.AnalysisHistoryPage;
import com.example.stockanalysis.analysis.application.StockAnalysisService;
import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisStatus;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.analysis.domain.StockChartPoint;
import com.example.stockanalysis.engine.model.AnalysisMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
        StockAnalysisController.class,
        AnalysisRunController.class,
        StockPriceController.class
})
class StockAnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockAnalysisService service;

    @Test
    void executesAnalysis() throws Exception {
        when(service.execute(eq("005930"), any(AnalysisExecutionCommand.class)))
                .thenReturn(completedRun());

        mockMvc.perform(post("/api/v1/stocks/005930/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "analysisMode": "SWING",
                                  "baseDate": "2026-08-14",
                                  "forceRecalculate": false
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analysisRunId").value(101))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(72.5))
                .andExpect(jsonPath("$.data.timeframeScores.daily").value(70.0))
                .andExpect(jsonPath("$.data.targets[0]").value(76000));
    }

    @Test
    void getsRunAndLatestResult() throws Exception {
        when(service.getRun(101L)).thenReturn(completedRun());
        when(service.getLatest("005930", AnalysisMode.SWING)).thenReturn(completedRun());

        mockMvc.perform(get("/api/v1/analysis-runs/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("005930"));

        mockMvc.perform(get("/api/v1/stocks/005930/analysis/latest")
                        .param("mode", "SWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strategyAction").value("WATCH"));
    }

    @Test
    void acceptsExplicitPositionContext() throws Exception {
        when(service.execute(eq("005930"), any(AnalysisExecutionCommand.class))).thenReturn(completedRun());
        mockMvc.perform(post("/api/v1/stocks/005930/analysis").contentType(MediaType.APPLICATION_JSON).content("""
                {"analysisMode":"SWING","baseDate":"2026-08-14","position":{
                  "held":true,"openedAt":"2026-08-01T01:00:00Z","entryPrice":70000,
                  "stopPrice":67000,"targets":[76000],"stopPolicy":"CLOSE"}}
                """)).andExpect(status().isAccepted());
        var captor = org.mockito.ArgumentCaptor.forClass(AnalysisExecutionCommand.class);
        org.mockito.Mockito.verify(service).execute(eq("005930"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().position().held()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().position().stopPrice()).isEqualByComparingTo("67000");
    }

    @Test
    void rejectsInvalidPositionInsteadOfSilentlyInventingPrices() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/005930/analysis").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"analysisMode":"SWING","position":{"held":true,"entryPrice":-1}}
                        """)).andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void getsAnalysisHistory() throws Exception {
        when(service.getHistory("005930", AnalysisMode.SWING, 0, 20))
                .thenReturn(new AnalysisHistoryPage(List.of(completedRun()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/stocks/005930/analysis/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].analysisRunId").value(101))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getsStoredDailyPrices() throws Exception {
        when(service.getPrices(
                "005930", "DAY", 1,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 14)
        )).thenReturn(List.of(new StockChartPoint(
                OffsetDateTime.parse("2026-08-14T00:00:00+09:00"),
                amount("70000"), amount("72000"),
                amount("69500"), amount("71000"), 10_000_000L
        )));

        mockMvc.perform(get("/api/v1/stocks/005930/prices")
                        .param("period", "DAY")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].businessDate").value("2026-08-14"))
                .andExpect(jsonPath("$.data[0].timestamp")
                        .value("2026-08-14T00:00:00+09:00"))
                .andExpect(jsonPath("$.data[0].close").value(71000));
    }

    @Test
    void getsMinutePricesWithSelectedInterval() throws Exception {
        when(service.getPrices("005930", "MINUTE", 5, null, null))
                .thenReturn(List.of(new StockChartPoint(
                        OffsetDateTime.parse("2026-08-22T15:25:00+09:00"),
                        amount("71000"), amount("71600"), amount("70800"),
                        amount("71500"), 320_000L
                )));

        mockMvc.perform(get("/api/v1/stocks/005930/prices")
                        .param("period", "MINUTE")
                        .param("interval", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].timestamp")
                        .value("2026-08-22T15:25:00+09:00"))
                .andExpect(jsonPath("$.data[0].close").value(71500));
    }

    @Test
    void rejectsInvalidExecutionRequest() throws Exception {
        mockMvc.perform(post("/api/v1/stocks/005930/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private AnalysisRunView completedRun() {
        return new AnalysisRunView(
                101L,
                "005930",
                "삼성전자",
                AnalysisMode.SWING,
                AnalysisStatus.COMPLETED,
                DataQualityStatus.COMPLETE,
                LocalDate.of(2026, 8, 14),
                "analysis-v1",
                amount("72.5"),
                "B",
                "SWING_WATCH",
                "스윙 관찰",
                "WATCH",
                amount("71000"),
                amount("70000"),
                amount("71000"),
                List.of(amount("76000"), amount("80000")),
                amount("67000"),
                60,
                List.of(new AnalysisFactorView(
                        "DAILY_TECHNICAL", amount("4.1"), amount("70"), amount("0.3"),
                        amount("21"), "상승 추세입니다."
                )),
                List.of("추세가 양호합니다."),
                List.of(),
                null,
                null,
                Instant.parse("2026-08-14T07:00:00Z"),
                Instant.parse("2026-08-14T07:00:02Z")
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
