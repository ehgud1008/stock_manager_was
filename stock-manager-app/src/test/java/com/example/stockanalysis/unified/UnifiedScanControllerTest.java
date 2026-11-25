package com.example.stockanalysis.unified;

import com.example.stockanalysis.unified.scan.UnifiedScanController;
import com.example.stockanalysis.unified.scan.UnifiedScanService;
import com.example.stockanalysis.unified.scan.UnifiedScanModels.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@WebMvcTest(UnifiedScanController.class)
class UnifiedScanControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private UnifiedScanService service;
    @MockBean private com.example.stockanalysis.analysis.application.StockAnalysisService stockAnalysis;
    private Run run() { return new Run("run-1", "key-1", "ALL", LocalDate.of(2026,9,18), "unified-swing-v1",
            "READY", false, 1L, null, null, 0, 0, 0, 0); }

    @Test void requiresIdempotencyKeyAndReturnsAsyncRun() throws Exception {
        when(service.submit(any(), eq("key-1"))).thenReturn(run());
        mvc.perform(post("/api/v1/unified-analysis/runs").contentType(MediaType.APPLICATION_JSON).content("{\"market\":\"ALL\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        mvc.perform(post("/api/v1/unified-analysis/runs").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"market\":\"ALL\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.runId").value("run-1"));
    }
    @Test void bindsCombinedSearchConditionsAndRejectsInvalidStages() throws Exception {
        when(service.query(eq("run-1"), any())).thenReturn(new Page(run(), 0, 0, 20, List.of()));
        mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("stage", "6").param("allSlopesUp", "true")
                        .param("maxBarsSinceTransition", "3").param("minScore", "70").param("signal", "BUY").param("entryStatus", "IN_RANGE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.run.runId").value("run-1"));
        var capture = org.mockito.ArgumentCaptor.forClass(Query.class); verify(service).query(eq("run-1"), capture.capture());
        assertThat(capture.getValue().stage()).isEqualTo(6); assertThat(capture.getValue().allSlopesUp()).isTrue();
        assertThat(capture.getValue().minScore()).isEqualByComparingTo("70");
        assertThat(capture.getValue().entryStatus()).isEqualTo("IN_RANGE");
        mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("entryStatus", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("stage", "7")).andExpect(status().isBadRequest());
    }
    @Test void exposesReadOnlyRecentRunsAndExplicitResume() throws Exception {
        when(service.recent()).thenReturn(List.of(run())); when(service.resume("run-1")).thenReturn(run());
        mvc.perform(get("/api/v1/unified-analysis/runs")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].runId").value("run-1"));
        mvc.perform(post("/api/v1/unified-analysis/runs/run-1/resume")).andExpect(status().isAccepted());
        verify(service).resume("run-1");
    }
    @Test void bindsMarketVolumeAndAmountAndRejectsInvalidInputs() throws Exception {
        when(service.query(eq("run-1"), any())).thenReturn(new Page(run(), 0, 0, 20, List.of()));
        mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("market", "KOSDAQ")
                .param("minVolume", "100000").param("minTradingAmountEok", "100.25")).andExpect(status().isOk());
        var capture = org.mockito.ArgumentCaptor.forClass(Query.class); verify(service).query(eq("run-1"), capture.capture());
        assertThat(capture.getValue().market()).isEqualTo("KOSDAQ");
        assertThat(capture.getValue().minVolume()).isEqualTo(100000L);
        assertThat(capture.getValue().minTradingAmountEok()).isEqualByComparingTo("100.25");
        for (String volume : List.of("-1", "1.5", "NaN", "9223372036854775808"))
            mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("minVolume", volume)).andExpect(status().isBadRequest());
        for (String amount : List.of("-1", "NaN", "0.0000001"))
            mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("minTradingAmountEok", amount)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/unified-analysis/runs/run-1/items").param("market", "NASDAQ")).andExpect(status().isBadRequest());
        verify(service, times(1)).query(anyString(), any());
    }
}
