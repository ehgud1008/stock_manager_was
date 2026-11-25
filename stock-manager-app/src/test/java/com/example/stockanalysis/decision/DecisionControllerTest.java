package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DecisionController.class)
class DecisionControllerTest {
    @Autowired MockMvc mvc; @MockBean DecisionService service;
    @Test void validatesContextBeforeSubmission() throws Exception {
        mvc.perform(post("/api/v1/analysis-snapshots/snapshot/decisions").header("Idempotency-Key","key")
                .contentType("application/json").content("{\"positionStatus\":\"NOT_HELD\",\"horizonTradingDays\":0}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void returnsDurableLocationAndStandardEnvelope() throws Exception {
        var view=new View("snapshot","SNAPSHOT","005930",null,null,"PENDING",null,null,null,1,null,0,null,false);
        when(service.snapshot(eq("005930"),any(),eq("key"))).thenReturn(view);
        mvc.perform(post("/api/v1/stocks/005930/analysis-snapshots").header("Idempotency-Key","key")
                .contentType("application/json").content("{\"source\":\"COLLECT\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location","/api/v1/analysis-snapshots/snapshot"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.status").value("PENDING"));
    }
    @Test void capabilitiesExposeNoCredentials() throws Exception {
        when(service.capabilities()).thenReturn(Map.of("enabled",false,"aiConfigured",false,"scopeMode","SINGLE_OWNER"));
        mvc.perform(get("/api/v1/trade-decisions/capabilities")).andExpect(status().isOk()).andExpect(jsonPath("$.data.apiKey").doesNotExist());
    }
    @Test void newsInsightsHaveAnExplicitPostAndReuseDurableStatusEndpoint() throws Exception {
        var view=new View("news-one","DECISION","005930","snapshot",null,"PENDING",null,null,null,1,null,0,null,false);
        when(service.insights(eq("snapshot"),eq(new DecisionService.InsightRequest(14,"반도체")),eq("news-key"))).thenReturn(view);
        mvc.perform(post("/api/v1/analysis-snapshots/snapshot/news-insights").header("Idempotency-Key","news-key")
                .contentType("application/json").content("{\"lookbackDays\":14,\"sectorQuery\":\"반도체\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location","/api/v1/trade-decisions/news-one"))
                .andExpect(jsonPath("$.data.snapshotId").value("snapshot"));
    }
}
