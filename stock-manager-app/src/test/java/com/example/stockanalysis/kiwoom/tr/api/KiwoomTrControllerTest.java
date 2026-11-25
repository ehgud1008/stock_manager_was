package com.example.stockanalysis.kiwoom.tr.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.stockanalysis.kiwoom.contract.tr.ka10081.Ka10081Response;
import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KiwoomTrController.class)
class KiwoomTrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KiwoomTrExecutionService service;

    @Test
    void acceptsHeaderAndBodyEnvelope() throws Exception {
        Ka10081Response kiwoomBody = Ka10081Response.builder()
                .stkCd("432720")
                .returnCode(0)
                .returnMsg("정상적으로 처리되었습니다")
                .build();
        when(service.execute(any(KiwoomTrExecutionRequest.class)))
                .thenReturn(new KiwoomTrExecutionResponse(
                        new KiwoomTrResponseHeader("ka10081", "N", ""),
                        kiwoomBody
                ));

        mockMvc.perform(post("/api/v1/kiwoom/tr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "api-id": "ka10081",
                                    "authorization": "Bearer test-token",
                                    "cont-yn": "N",
                                    "next-key": ""
                                  },
                                  "body": {
                                    "stk_cd": "432720",
                                    "base_dt": "20260430",
                                    "upd_stkpc_tp": "1"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.header.api-id").value("ka10081"))
                .andExpect(jsonPath("$.data.header.cont-yn").value("N"))
                .andExpect(jsonPath("$.data.header.authorization").doesNotExist())
                .andExpect(jsonPath("$.data.body.stk_cd").value("432720"))
                .andExpect(jsonPath("$.data.body.return_code").value(0));
    }

    @Test
    void rejectsRequestWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/kiwoom/tr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "api-id": "ka10081",
                                    "cont-yn": "N",
                                    "next-key": ""
                                  },
                                  "body": {
                                    "stk_cd": "432720"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
