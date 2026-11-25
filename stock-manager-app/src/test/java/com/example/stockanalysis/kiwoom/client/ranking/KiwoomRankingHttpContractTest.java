package com.example.stockanalysis.kiwoom.client.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class KiwoomRankingHttpContractTest {

    @Test
    void sendsRankingConditionsAndAcceptsCamelCaseResultFields() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .baseUrl("https://mockapi.kiwoom.com")
                .build();
        KiwoomRankingHttpApi httpApi = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(KiwoomRankingHttpApi.class);
        KiwoomRankingClient client =
                new KiwoomRankingClient(httpApi, new KiwoomErrorDecoder());

        server.expect(once(), requestTo("https://mockapi.kiwoom.com/api/dostk/rkinfo"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-id", "ka10030"))
                .andExpect(header("cont-yn", "Y"))
                .andExpect(header("next-key", "previous-page-key"))
                .andExpect(content().json(
                        """
                        {
                          "mrkt_tp": "000",
                          "sort_tp": "1",
                          "mang_stk_incls": "1",
                          "crd_tp": "0",
                          "trde_qty_tp": "0",
                          "pric_tp": "0",
                          "trde_prica_tp": "0",
                          "mrkt_open_tp": "0",
                          "stex_tp": "3"
                        }
                        """
                ))
                .andRespond(withSuccess(
                        """
                        {
                          "tdy_trde_qty_upper": [
                            {
                              "stk_cd": "005930",
                              "stk_nm": "삼성전자",
                              "cur_prc": "-152000",
                              "trde_qty": "34954641"
                            }
                          ],
                          "returnCode": 0,
                          "returnMsg": "정상적으로 처리되었습니다"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ).header("cont-yn", "Y")
                        .header("next-key", "following-page-key"));

        KiwoomResponse<Ka10030Response> response =
                client.getTradingVolumeRanking(request(), "previous-page-key");

        assertThat(response.body().getReturnCode()).isZero();
        assertThat(response.body().getTdyTrdeQtyUpper()).hasSize(1);
        assertThat(response.continuation().hasNext()).isTrue();
        assertThat(response.continuation().nextKey()).isEqualTo("following-page-key");
        server.verify();
    }

    private Ka10030Request request() {
        return Ka10030Request.builder()
                .mrktTp("000")
                .sortTp("1")
                .mangStkIncls("1")
                .crdTp("0")
                .trdeQtyTp("0")
                .pricTp("0")
                .trdePricaTp("0")
                .mrktOpenTp("0")
                .stexTp("3")
                .build();
    }
}
