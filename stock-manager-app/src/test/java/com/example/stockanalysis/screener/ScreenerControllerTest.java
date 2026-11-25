package com.example.stockanalysis.screener;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.screener.api.ScreenerController;
import com.example.stockanalysis.screener.application.*;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest;
import com.example.stockanalysis.shared.exception.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScreenerController.class)
class ScreenerControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private ScreenerService service;
    @Test void recoveryUsesPostAndReturnsConflictForLiveJobs() throws Exception {
        mvc.perform(post("/api/v1/screener/runs/stale/recover"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).recover("stale");
        when(service.recover("busy")).thenThrow(new BusinessException(ErrorCode.SCREENER_CONFLICT));
        mvc.perform(post("/api/v1/screener/runs/busy/recover")).andExpect(status().isConflict());
        when(service.recover("missing")).thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        mvc.perform(post("/api/v1/screener/runs/missing/recover")).andExpect(status().isNotFound());
    }
    @Test void submissionIsAcceptedAndDefaultsAreBound() throws Exception {
        mvc.perform(post("/api/v1/screener/runs").header("Idempotency-Key","test1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.success").value(true));
        verify(service).submit(new ScreenerRunRequest(null,null,null,null,null),"test1");
    }
    @Test void queryBindsRecordFiltersAndReturnsEnvelope() throws Exception {
        when(service.query(any())).thenAnswer(inv -> { ScreenerQuery q = inv.getArgument(0); return new ScreenerService.ScreenerPage(null,"MEMORY",0,q.page(),q.size(),Map.of(),List.of()); });
        mvc.perform(get("/api/v1/screener").param("mode","SHORT_TERM").param("interval","5").param("stage","6").param("size","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(10));
        verify(service).query(argThat(q -> q.interval() == 5 && q.stage() == 6));
    }
    @Test void invalidRequestAndQueryReturn400() throws Exception {
        mvc.perform(post("/api/v1/screener/runs").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"SHORT_TERM\",\"interval\":2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/screener").param("stage","7")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/screener").param("size","101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void readOnlySearchBindsNewFiltersAndRejectsInvalidInputs() throws Exception {
        when(service.query(any())).thenAnswer(inv -> { ScreenerQuery q = inv.getArgument(0); return new ScreenerService.ScreenerPage(null,"DATABASE",0,q.page(),q.size(),Map.of(),List.of()); });
        mvc.perform(post("/api/v1/screener/search").contentType(MediaType.APPLICATION_JSON).content("""
                {"mode":"SHORT_TERM","interval":5,"page":2,"size":20,"slope":"MID_LONG_UP","middleLongGap":"EXPANDING",
                 "transitionFilter":"6_1","watchOnly":true,"watchedCodes":["005930","005930"],"minTradingAmountMillionWon":"29"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(2)).andExpect(jsonPath("$.data.recentCount").value(0));
        verify(service).query(argThat(q -> q.watchOnly() && q.watchedCodes().equals(List.of("005930")) && q.slope().equals("MID_LONG_UP")
                && q.transitionFilter().equals("6_1") && q.minTradingAmountMillionWon().intValueExact() == 29));
        for (String invalid : List.of("{\"sort\":\"stock_code; DROP TABLE sc_screening_item\"}", "{\"transitionFilter\":\"INVALID\"}",
                "{\"watchedCodes\":[null]}", "{\"watchedCodes\":[\"005930'\"]}", "{\"slope\":\"OTHER\"}")) {
            mvc.perform(post("/api/v1/screener/search").contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        verifyNoMoreInteractions(service);
    }
    @Test void notFoundAndConflictUseExistingErrorEnvelope() throws Exception {
        when(service.get("missing")).thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        when(service.resume("busy")).thenThrow(new BusinessException(ErrorCode.SCREENER_CONFLICT));
        mvc.perform(get("/api/v1/screener/runs/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/v1/screener/runs/busy/resume")).andExpect(status().isConflict());
    }
    @Test void evaluateIsPureAndReturnsUnroundedEmaWithNoSignal() throws Exception {
        when(service.evaluate(any())).thenAnswer(inv -> new DefaultScreenerEngine(ScreenerConfig.defaults()).analyze(inv.getArgument(0)));
        mvc.perform(post("/api/v1/screener/evaluate").contentType(MediaType.APPLICATION_JSON).content("""
                {"stockCode":"005930","timeframe":{"mode":"SWING","interval":0},"asOf":"2026-09-13T10:00:00","bars":[],"dailyBars":[]}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.quality").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.data.evaluationStatus").value("NOT_EVALUATED"));
    }
}
