package com.example.stockanalysis.decision;
import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.example.stockanalysis.shared.exception.GlobalExceptionHandler;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DecisionPlanControllerTest {
    DecisionWorkflowTest fixture;UnifiedScanStore scans;DecisionService service;MockMvc mvc;
    @BeforeEach void setup() {
        fixture=new DecisionWorkflowTest();fixture.setup();scans=mock(UnifiedScanStore.class);service=mock(DecisionService.class);
        mvc=MockMvcBuilders.standaloneSetup(new DecisionPlanController(fixture.settings,service,scans,fixture.policy,fixture.json))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(scans.detail("saved-run","005930")).thenReturn(Optional.of(fixture.data));
    }
    @Test void sourcePreviewCalculatesFromStoredDataWithoutCreatingSnapshotOrDecision() throws Exception {
        mvc.perform(post("/api/v1/stocks/005930/decision-plan").param("sourceRunId","saved-run").contentType("application/json")
                .content("{\"positionStatus\":\"NOT_HELD\",\"horizonTradingDays\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.horizonPlan.horizonTradingDays").value(20))
                .andExpect(jsonPath("$.data.comparisons.length()").value(3));
        verify(scans).detail("saved-run","005930");verifyNoMoreInteractions(scans);verifyNoInteractions(service);
    }
    @Test void invalidHoldingContextAndOutOfRangePeriodsAreRejected() throws Exception {
        for(String body:List.of("{\"positionStatus\":\"HELD\",\"horizonTradingDays\":0}",
                "{\"positionStatus\":\"NOT_HELD\",\"horizonTradingDays\":10,\"averageBuyPrice\":100}",
                "{\"positionStatus\":\"HELD\",\"horizonTradingDays\":10,\"purchasedOn\":\"2099-01-01\"}"))
            mvc.perform(post("/api/v1/stocks/005930/decision-plan").param("sourceRunId","saved-run").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void nonexistentSourceIsNotRecollected() throws Exception {
        when(scans.detail("missing","005930")).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/stocks/005930/decision-plan").param("sourceRunId","missing").contentType("application/json")
                .content("{\"positionStatus\":\"HELD\",\"horizonTradingDays\":10}"))
                .andExpect(status().isNotFound());verifyNoInteractions(service);
    }
}
