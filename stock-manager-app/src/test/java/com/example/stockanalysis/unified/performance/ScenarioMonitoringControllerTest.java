package com.example.stockanalysis.unified.performance;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScenarioMonitoringControllerTest {
    @Test void readOnlyEndpointReturnsMonitoringIndependentlyOfPerformanceRegistration() throws Exception {
        var service=mock(ScenarioMonitoringService.class);
        when(service.get("saved-report")).thenReturn(new ScenarioMonitoringModels.View("READY","saved-report",LocalDate.of(2026,10,19),
                LocalDate.of(2026,10,20),LocalDate.of(2026,11,16),Instant.parse("2026-10-19T12:00:00Z"),List.of(),List.of()));
        var mvc=MockMvcBuilders.standaloneSetup(new ScenarioMonitoringController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(PerformanceFixtures.JSON)).build();
        mvc.perform(get("/api/v1/unified-analysis/runs/saved-report/monitoring"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("saved-report"))
                .andExpect(jsonPath("$.data.selectionDate").value("2026-10-19"));
        mvc.perform(post("/api/v1/unified-analysis/runs/saved-report/monitoring")).andExpect(status().isMethodNotAllowed());
        verify(service,times(1)).get("saved-report");
    }
}
