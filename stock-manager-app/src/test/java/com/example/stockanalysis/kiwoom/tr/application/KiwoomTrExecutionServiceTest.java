package com.example.stockanalysis.kiwoom.tr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.auth.KiwoomTokenProvider;
import com.example.stockanalysis.kiwoom.contract.tr.ka10080.Ka10080Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10080.Ka10080Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10082.Ka10082Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10082.Ka10082Response;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionRequest;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionResponse;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrRequestHeader;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrCallResult;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrCatalog;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrClient;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrDefinition;
import com.example.stockanalysis.shared.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KiwoomTrExecutionServiceTest {

    private final KiwoomTrCatalog catalog = mock(KiwoomTrCatalog.class);
    private final KiwoomTrClient client = mock(KiwoomTrClient.class);
    private final KiwoomTokenProvider tokenProvider = mock(KiwoomTokenProvider.class);
    private final KiwoomTrExecutionService service =
            new KiwoomTrExecutionService(catalog, client, tokenProvider);

    @Test
    void masksAuthorizationWhenRequestHeaderIsRendered() {
        KiwoomTrRequestHeader header = new KiwoomTrRequestHeader(
                "ka10080", "Bearer secret-token", "N", ""
        );

        assertThat(header.toString()).contains("authorization=***");
        assertThat(header.toString()).doesNotContain("secret-token");
    }

    @Test
    void executesRegisteredTrWithoutReturningAuthorization() {
        KiwoomTrDefinition definition = new KiwoomTrDefinition(
                "ka10080",
                "주식분봉차트조회요청",
                "/api/dostk/chart",
                Ka10080Request.class,
                Ka10080Response.class
        );
        Ka10080Response body = Ka10080Response.builder()
                .stkCd("005930")
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
        Map<String, Object> requestBody = Map.of(
                "stk_cd", "005930",
                "tic_scope", "5",
                "upd_stkpc_tp", "1"
        );
        when(catalog.getRequired("ka10080")).thenReturn(definition);
        when(client.execute(
                definition,
                "Bearer secret-token",
                "N",
                "",
                requestBody
        )).thenReturn(new KiwoomTrCallResult(
                body,
                new KiwoomContinuation(true, "minute-next-key")
        ));

        KiwoomTrExecutionResponse result = service.execute(
                new KiwoomTrExecutionRequest(
                        new KiwoomTrRequestHeader(
                                "ka10080", "Bearer secret-token", "N", ""
                        ),
                        requestBody
                )
        );

        assertThat(result.body()).isSameAs(body);
        assertThat(result.header().apiId()).isEqualTo("ka10080");
        assertThat(result.header().contYn()).isEqualTo("Y");
        assertThat(result.header().nextKey()).isEqualTo("minute-next-key");
        verify(client).execute(
                definition,
                "Bearer secret-token",
                "N",
                "",
                requestBody
        );
    }

    @Test
    void requiresNextKeyForContinuationRequest() {
        KiwoomTrExecutionRequest request = new KiwoomTrExecutionRequest(
                new KiwoomTrRequestHeader("ka10081", "token", "Y", ""),
                Map.of("stk_cd", "005930")
        );

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("next-key");
    }

    @Test
    void executesInternalTrWithServerManagedAuthorization() {
        KiwoomTrDefinition definition = new KiwoomTrDefinition(
                "ka10082",
                "주식주봉차트조회요청",
                "/api/dostk/chart",
                Ka10082Request.class,
                Ka10082Response.class
        );
        Map<String, Object> requestBody = Map.of(
                "stk_cd", "432720",
                "base_dt", "20260430",
                "upd_stkpc_tp", "1"
        );
        var responseBody = Ka10082Response
                .builder()
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
        when(tokenProvider.getAccessToken()).thenReturn("server-managed-token");
        when(catalog.getRequired("ka10082")).thenReturn(definition);
        when(client.execute(
                definition,
                "server-managed-token",
                "N",
                "",
                requestBody
        )).thenReturn(new KiwoomTrCallResult(
                responseBody,
                KiwoomContinuation.none()
        ));

        KiwoomTrExecutionResponse result = service.executeWithServerAuthorization(
                "ka10082",
                "N",
                "",
                requestBody
        );

        assertThat(result.body()).isSameAs(responseBody);
        verify(tokenProvider).getAccessToken();
        verify(client).execute(
                definition,
                "server-managed-token",
                "N",
                "",
                requestBody
        );
    }
}
