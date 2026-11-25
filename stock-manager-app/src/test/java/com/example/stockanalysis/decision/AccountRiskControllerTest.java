package com.example.stockanalysis.decision;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountRiskController.class)
class AccountRiskControllerTest {
    @Autowired MockMvc mvc;@MockBean AccountRiskService service;
    @Test void readsSettingsWithoutSavingOrLeakingOwnerDetails() throws Exception {
        when(service.get()).thenReturn(new AccountRiskModels.View("NOT_CONFIGURED",null,Instant.parse("2026-11-17T01:00:00Z"),"설정을 등록하세요."));
        mvc.perform(get("/api/v1/account-risk")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.owner_scope").doesNotExist());
        verify(service).get();verifyNoMoreInteractions(service);
    }
    @Test void acceptsMoneyStringsAndReturnsTheNewRevision() throws Exception {
        when(service.save(any())).thenReturn(new AccountRiskModels.Settings(new BigDecimal("100000"),new BigDecimal("10000"),new BigDecimal("500"),new BigDecimal("5"),LocalDate.of(2026,11,17),1,Instant.parse("2026-11-17T01:00:00Z")));
        mvc.perform(put("/api/v1/account-risk").contentType("application/json").content("{\"totalAssets\":\"100000\",\"availableCash\":\"10000\",\"maxLossPerTrade\":\"500\",\"maxPositionPercent\":\"5\",\"balanceDate\":\"2026-11-17\",\"expectedRevision\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(1));
    }
    @Test void validatesTheInputBeforeInvokingTheService() throws Exception {
        mvc.perform(put("/api/v1/account-risk").contentType("application/json").content("{\"totalAssets\":100000,\"availableCash\":10000,\"maxLossPerTrade\":500,\"maxPositionPercent\":101,\"balanceDate\":\"2026-11-17\",\"expectedRevision\":0}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
}
