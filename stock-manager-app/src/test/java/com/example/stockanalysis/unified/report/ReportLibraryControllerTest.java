package com.example.stockanalysis.unified.report;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportLibraryController.class)
class ReportLibraryControllerTest {
    @Autowired MockMvc mvc;@MockBean DailyReportService service;
    @Test void exposesReadOnlyLibraryWithStableReadyDefaultAndPagination() throws Exception {
        var ready=new ReportModels.Summary("ready",LocalDate.of(2026,10,13),"ALL","COMPLETED","READY");
        when(service.library(0,30)).thenReturn(new ReportModels.Library(true,ready.tradeDate(),ready,ready,List.of(ready),0,false));
        mvc.perform(get("/api/v1/unified-analysis/reports")).andExpect(status().isOk()).andExpect(jsonPath("$.data.latestReady.runId").value("ready"));
        verify(service).library(0,30);verifyNoMoreInteractions(service);
    }
    @Test void invalidPageIsAClientError() throws Exception {
        when(service.library(-1,30)).thenThrow(new IllegalArgumentException("보고서 페이지 범위를 확인하세요."));
        mvc.perform(get("/api/v1/unified-analysis/reports").param("page","-1")).andExpect(status().isBadRequest());
    }
}
