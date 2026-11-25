package com.example.stockanalysis.kiwoom.client.stockinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class KiwoomStockInfoHttpContractTest {

    @Test
    void httpInterfaceUsesExpectedPathHeadersAndJsonBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .baseUrl("https://mockapi.kiwoom.com")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth("test-token");
                    return execution.execute(request, body);
                })
                .build();
        KiwoomStockInfoHttpApi httpApi = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(KiwoomStockInfoHttpApi.class);

        server.expect(once(), requestTo("https://mockapi.kiwoom.com/api/dostk/stkinfo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("api-id", "ka10001"))
                .andExpect(header("cont-yn", "N"))
                .andExpect(header("next-key", ""))
                .andExpect(content().json("{\"stk_cd\":\"005930\"}"))
                .andRespond(withSuccess(
                        """
                        {
                          "stk_cd": "005930",
                          "stk_nm": "삼성전자",
                          "cur_prc": "+81000",
                          "return_code": 0,
                          "return_msg": "정상처리"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ResponseEntity<Ka10001Response> response =
                httpApi.getStockBasicInformation(
                        "ka10001",
                        "N",
                        "",
                        Ka10001Request.builder()
                                .stkCd("005930")
                                .build()
                );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStkNm()).isEqualTo("삼성전자");
        server.verify();
    }
}
