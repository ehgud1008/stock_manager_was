package com.example.stockanalysis.kiwoom.tr.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.contract.tr.ka10082.Ka10082Response;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KiwoomTrClientHttpContractTest {

    @Test
    void sendsTypedBodyAndForwardedHeadersThenReadsTypedResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KiwoomTrClient client = new KiwoomTrClient(
                builder.baseUrl("https://mockapi.kiwoom.com").build(),
                new ObjectMapper(),
                new KiwoomErrorDecoder()
        );
        KiwoomTrDefinition definition = new KiwoomTrCatalog(new ObjectMapper())
                .getRequired("ka10082");

        server.expect(once(), requestTo("https://mockapi.kiwoom.com/api/dostk/chart"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("api-id", "ka10082"))
                .andExpect(header("cont-yn", "N"))
                .andExpect(header("next-key", ""))
                .andExpect(content().json("""
                        {
                          "stk_cd": "432720",
                          "base_dt": "20260430",
                          "upd_stkpc_tp": "1"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "stk_cd": "432720",
                          "stk_stk_pole_chart_qry": [],
                          "return_code": 0,
                          "return_msg": "정상적으로 처리되었습니다"
                        }
                        """, MediaType.APPLICATION_JSON)
                        .header("cont-yn", "Y")
                        .header("next-key", "weekly-next-key"));

        KiwoomTrCallResult result = client.execute(
                definition,
                "test-token",
                "N",
                "",
                Map.of(
                        "stk_cd", "432720",
                        "base_dt", "20260430",
                        "upd_stkpc_tp", "1"
                )
        );

        assertThat(result.body()).isInstanceOf(Ka10082Response.class);
        assertThat(((Ka10082Response) result.body()).getStkCd()).isEqualTo("432720");
        assertThat(result.continuation().hasNext()).isTrue();
        assertThat(result.continuation().nextKey()).isEqualTo("weekly-next-key");
        server.verify();
    }

    @Test
    void rejectsBodyFieldThatDoesNotExistInRequestDto() {
        RestClient.Builder builder = RestClient.builder();
        KiwoomTrClient client = new KiwoomTrClient(
                builder.baseUrl("https://mockapi.kiwoom.com").build(),
                new ObjectMapper(),
                new KiwoomErrorDecoder()
        );
        KiwoomTrDefinition definition = new KiwoomTrCatalog(new ObjectMapper())
                .getRequired("ka10081");

        assertThatThrownBy(() -> client.execute(
                definition,
                "Bearer test-token",
                "N",
                "",
                Map.of("stk_cd", "005930", "unknown_field", "value")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unknown_field");
    }
}
