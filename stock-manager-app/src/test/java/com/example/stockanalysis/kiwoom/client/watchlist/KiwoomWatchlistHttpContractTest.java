package com.example.stockanalysis.kiwoom.client.watchlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class KiwoomWatchlistHttpContractTest {

    @Test
    void sendsEmptyJsonBodyWithoutStockCode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .baseUrl("https://mockapi.kiwoom.com")
                .build();
        KiwoomWatchlistHttpApi httpApi = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(KiwoomWatchlistHttpApi.class);
        KiwoomWatchlistClient client =
                new KiwoomWatchlistClient(httpApi, new KiwoomErrorDecoder());

        server.expect(once(), requestTo("https://mockapi.kiwoom.com/api/dostk/watchlist"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-id", "ka01300"))
                .andExpect(header("cont-yn", "N"))
                .andExpect(header("next-key", ""))
                .andExpect(content().json("{}"))
                .andRespond(withSuccess(
                        """
                        {
                          "rtcd": "S",
                          "nofi": [
                            {"gcod": "001", "name": "관심종목1"},
                            {"gcod": "002", "name": "관심종목2"}
                          ],
                          "return_code": 0,
                          "return_msg": "정상적으로 처리되었습니다"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        KiwoomResponse<Ka01300Response> response = client.getGroups();

        assertThat(response.body().getNofi()).hasSize(2);
        assertThat(response.body().getNofi().get(0).getGcod()).isEqualTo("001");
        server.verify();
    }
}
