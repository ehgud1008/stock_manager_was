package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DecisionRecheckControllerTest {
    @Test void returnsSeparateReadOnlyResponseAndDisablesCaching() throws Exception {
        var service=mock(DecisionRecheckService.class);
        when(service.get("id")).thenReturn(JsonNodeFactory.instance.objectNode().put("status","PARTIAL"));
        var mvc=MockMvcBuilders.standaloneSetup(new DecisionRecheckController(service)).build();
        mvc.perform(get("/api/v1/trade-decisions/id/recheck")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.status").value("PARTIAL"));
        verify(service).get("id");verifyNoMoreInteractions(service);
    }
}
