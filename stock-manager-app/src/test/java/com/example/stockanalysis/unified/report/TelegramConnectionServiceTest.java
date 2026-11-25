package com.example.stockanalysis.unified.report;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;

class TelegramConnectionServiceTest {
    final HttpClient client=mock(HttpClient.class);
    TelegramConnectionService service(boolean enabled){return new TelegramConnectionService(new TelegramProperties(enabled,"12345:test_secret","123456",""),ReportFixtures.MAPPER,client);}
    @SuppressWarnings("unchecked")
    HttpResponse<String> response(int code,String body){var r=(HttpResponse<String>)mock(HttpResponse.class);when(r.statusCode()).thenReturn(code);when(r.body()).thenReturn(body);return r;}
    @Test void checksWhileDeliveryDisabledWithoutSendingOrExposingCredentials()throws Exception {
        doReturn(
                response(200,"{\"ok\":true,\"result\":{\"id\":12345,\"is_bot\":true,\"username\":\"stocks_bot\"}}"),
                response(200,"{\"ok\":true,\"result\":{\"id\":123456,\"type\":\"private\",\"first_name\":\"Private Name\"}}")).when(client).send(any(),any(HttpResponse.BodyHandler.class));
        var check=service(false).check();assertThat(check.state()).isEqualTo("VERIFIED");assertThat(check.automaticDeliveryEnabled()).isFalse();
        assertThat(ReportFixtures.MAPPER.writeValueAsString(check)).doesNotContain("test_secret","123456","Private Name");
        var calls=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client,times(2)).send(calls.capture(),any(HttpResponse.BodyHandler.class));
        assertThat(calls.getAllValues()).allMatch(r->r.uri().getPath().endsWith("/getMe")||r.uri().getPath().endsWith("/getChat"));
    }
    @Test void unconfiguredTokenDoesNotCallTelegram(){
        var check=new TelegramConnectionService(new TelegramProperties(false,"","",""),ReportFixtures.MAPPER,client).check();
        assertThat(check.state()).isEqualTo("TOKEN_NOT_CONFIGURED");verifyNoInteractions(client);
    }
    @Test void handlesAuthenticationRateLimitsAndUnknownErrorsWithoutRawDetails()throws Exception {
        for(var item:new String[][]{{"401","AUTH_FAILED"},{"429","RATE_LIMIT"},{"503","UNCONFIRMED"}}){
            doReturn(response(Integer.parseInt(item[0]),"{\"ok\":false,\"error_code\":"+item[0]+",\"description\":\"test_secret\"}")).when(client).send(any(),any(HttpResponse.BodyHandler.class));
            var check=service(true).check();assertThat(check.state()).isEqualTo(item[1]);assertThat(check.toString()).doesNotContain("test_secret");
        }
    }
    @Test void mismatchedDestinationCannotPass()throws Exception {
        doReturn(
                response(200,"{\"ok\":true,\"result\":{\"id\":12345,\"is_bot\":true,\"username\":\"stocks_bot\"}}"),
                response(200,"{\"ok\":true,\"result\":{\"id\":99999,\"type\":\"private\"}}")).when(client).send(any(),any(HttpResponse.BodyHandler.class));
        assertThat(service(true).check().state()).isEqualTo("UNCONFIRMED");
    }
}
