package com.example.stockanalysis.unified;

import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.unified.api.UnifiedAnalysisController;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UnifiedAnalysisController.class)
class UnifiedAnalysisControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private UnifiedAnalysisService service;

    @Test void serializesBothEngineResultsInTheStandardEnvelope() throws Exception {
        var asOf = LocalDateTime.of(2026, 9, 19, 0, 0);
        var price = new BigDecimal("100");
        var bar = new ScreenerBar(asOf.minusDays(1), asOf, price, price, price, price, 100, null);
        var result = new DefaultUnifiedStockAnalysisEngine().analyze(new UnifiedAnalysisInput(
                "005930", "삼성전자", "KOSPI", asOf, List.of(bar), Map.of()));
        when(service.execute("005930")).thenReturn(new UnifiedAnalysisResponse(result, DataQualityStatus.PARTIAL, List.of()));
        mvc.perform(post("/api/v1/stocks/005930/unified-analysis"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result.stock.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.result.engineVersion").value("unified-swing-v1"))
                .andExpect(jsonPath("$.data.result.analysis.signalReport.completeness").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.data.result.screener.quality").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.data.collectionQuality").value("PARTIAL"));
    }

    @Test void rejectsInvalidCodeWithoutExecuting() throws Exception {
        mvc.perform(post("/api/v1/stocks/INVALID/unified-analysis")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
