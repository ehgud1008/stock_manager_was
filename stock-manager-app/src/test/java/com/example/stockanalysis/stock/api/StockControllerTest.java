package com.example.stockanalysis.stock.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.stockanalysis.stock.application.StockQueryService;
import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockQueryService stockQueryService;

    @Test
    void returnsStockProfileUsingBusinessApi() throws Exception {
        when(stockQueryService.getStockProfile("005930"))
                .thenReturn(new StockProfile(
                        StockCode.of("005930"),
                        "삼성전자",
                        new BigDecimal("81000"),
                        new BigDecimal("80000"),
                        new BigDecimal("82000"),
                        new BigDecimal("79500"),
                        new BigDecimal("4830000"),
                        new BigDecimal("5969782550"),
                        new BigDecimal("15.1"),
                        new BigDecimal("5400"),
                        new BigDecimal("9.2"),
                        new BigDecimal("1.4"),
                        new BigDecimal("57800")
                ));

        mockMvc.perform(get("/api/v1/stocks/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data.currentPrice").value(81000))
                .andExpect(jsonPath("$.data.per").value(15.1));
    }

    @Test
    void rejectsInvalidStockCodeBeforeCallingApplicationService() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/5930"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
