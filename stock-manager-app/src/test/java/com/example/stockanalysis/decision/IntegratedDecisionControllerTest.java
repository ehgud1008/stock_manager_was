package com.example.stockanalysis.decision;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntegratedDecisionController.class)
class IntegratedDecisionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IntegratedDecisionService integrated;
    @MockBean DecisionService decisions;
    @MockBean DecisionOutcomeService outcomes;
    @Test void integratedCreationReturnsDurableStatusLocationAndMode() throws Exception {
        var job=new DecisionModels.Job("one","owner","DECISION","005930","snapshot",null,"PENDING",null,null,null,null,1,null,0,null,null,null,false);
        var view=new DecisionModels.View("one","DECISION","005930","snapshot",null,"PENDING",null,null,null,1,null,0,null,false,"INTEGRATED_DECISION",IntegratedDecisionContract.VERSION);
        when(integrated.create("snapshot",new IntegratedDecisionService.Request(5,null),"key",null)).thenReturn(job);
        when(decisions.get("one","DECISION")).thenReturn(view);
        mvc.perform(post("/api/v1/analysis-snapshots/snapshot/integrated-decisions").header("Idempotency-Key","key").contentType("application/json").content("{\"horizonTradingDays\":5}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location","/api/v1/trade-decisions/one"))
                .andExpect(jsonPath("$.data.mode").value("INTEGRATED_DECISION"));
    }
    @Test void outcomeAndComparisonAreReadOnlyAndUseStandardEnvelope() throws Exception {
        when(outcomes.get("one")).thenReturn(Map.of("status","WAITING_DECISION"));
        when(outcomes.comparison("one")).thenReturn(Map.of("status","WAITING_DATA"));
        mvc.perform(get("/api/v1/trade-decisions/one/outcome")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAITING_DECISION"));
        mvc.perform(get("/api/v1/trade-decisions/one/comparison")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAITING_DATA"));
        verifyNoInteractions(integrated,decisions);
    }
    @Test void currentValidityHasAReadOnlyStandardEnvelope() throws Exception {
        when(integrated.currentValidity("one")).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(Map.of("status","EXPIRED","requiresAnalysisRefresh",true)));
        mvc.perform(get("/api/v1/trade-decisions/one/validity")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED")).andExpect(jsonPath("$.data.requiresAnalysisRefresh").value(true));
        verifyNoInteractions(decisions,outcomes);
    }
}
