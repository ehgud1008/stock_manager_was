package com.example.stockanalysis.kiwoom.client.stockinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.KiwoomHeaders;
import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class KiwoomStockInfoClientTest {

    @Test
    void sendsTypedTrRequestAndPreservesContinuationHeaders() {
        KiwoomStockInfoHttpApi httpApi = Mockito.mock(KiwoomStockInfoHttpApi.class);
        Ka10001Response body = response();
        HttpHeaders headers = new HttpHeaders();
        headers.set(KiwoomHeaders.CONTINUATION, "Y");
        headers.set(KiwoomHeaders.NEXT_KEY, "next-page-key");

        when(httpApi.getStockBasicInformation(
                "ka10001",
                "N",
                "",
                Ka10001Request.builder().stkCd("005930").build()
        )).thenReturn(ResponseEntity.ok().headers(headers).body(body));

        KiwoomStockInfoClient client =
                new KiwoomStockInfoClient(httpApi, new KiwoomErrorDecoder());

        KiwoomResponse<Ka10001Response> result =
                client.getBasicInformation("005930");

        assertThat(result.body()).isSameAs(body);
        assertThat(result.continuation().hasNext()).isTrue();
        assertThat(result.continuation().nextKey()).isEqualTo("next-page-key");
        verify(httpApi).getStockBasicInformation(
                "ka10001",
                "N",
                "",
                Ka10001Request.builder().stkCd("005930").build()
        );
    }

    private Ka10001Response response() {
        return Ka10001Response.builder()
                .stkCd("005930")
                .stkNm("삼성전자")
                .curPrc("81000")
                .openPric("80000")
                .highPric("82000")
                .lowPric("79500")
                .mac("4830000")
                .floStk("5969782550")
                .per("15.1")
                .eps("5400")
                .roe("9.2")
                .pbr("1.4")
                .bps("57800")
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
    }
}
