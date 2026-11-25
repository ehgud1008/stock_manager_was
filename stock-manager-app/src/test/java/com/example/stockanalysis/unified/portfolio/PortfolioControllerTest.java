package com.example.stockanalysis.unified.portfolio;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PortfolioController.class,OperationsController.class})
class PortfolioControllerTest {
    @Autowired MockMvc mvc;@MockBean PortfolioService service;@MockBean OperationsService operations;
    static final String REQUEST="{\"reportIds\":[\"report-a\"],\"initialCash\":10000000,\"maximumPositions\":5,\"positionPercent\":20}";
    @Test void requiresRequestKeyAndRejectsInvalidMoneyBeforeCallingService() throws Exception {
        mvc.perform(post("/api/v1/unified-analysis/portfolios").contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/unified-analysis/portfolios").header("Idempotency-Key","key").contentType(MediaType.APPLICATION_JSON).content(REQUEST.replace("10000000","-1"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service,operations);
    }
    @Test void acceptedCreateReturnsIdentityAndReproducibilityErrorsAreActionable() throws Exception {
        when(service.create(any(),eq("key"))).thenReturn(new PortfolioModels.View("account","PENDING",null,0,null,null));
        mvc.perform(post("/api/v1/unified-analysis/portfolios").header("Idempotency-Key","key").contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isAccepted()).andExpect(jsonPath("$.data.id").value("account"));
        when(service.create(any(),eq("key"))).thenThrow(new IllegalArgumentException("같은 거래일 보고서는 하나만 선택하세요."));
        mvc.perform(post("/api/v1/unified-analysis/portfolios").header("Idempotency-Key","key").contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("같은 거래일 보고서는 하나만 선택하세요."));
    }
    @Test void readsNeverRegisterOrSend() throws Exception {
        when(service.recent()).thenReturn(List.of());when(service.available()).thenReturn(List.of());
        when(operations.preview("report-a")).thenReturn(new OperationsService.Preview("report-a","preview"));
        mvc.perform(get("/api/v1/unified-analysis/portfolios")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/unified-analysis/portfolios/reports/report-a/selection-audit")).andExpect(status().isOk());
        verify(service).audit("report-a");
        mvc.perform(get("/api/v1/unified-analysis/operations/reports/report-a/preview")).andExpect(status().isOk()).andExpect(jsonPath("$.data.text").value("preview"));
        verify(service,never()).create(any(),anyString());verify(operations,never()).test(anyString(),any());
    }
    @Test void testEndpointAlsoRequiresIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/unified-analysis/operations/telegram-tests").contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true,\"destinationFingerprint\":\"x\"}")).andExpect(status().isBadRequest());verifyNoInteractions(operations);
    }
}
