package com.example.stockanalysis.kiwoom.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stockanalysis.kiwoom.auth.KiwoomAuthHttpApi;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Request;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;

class KiwoomResponseValidationInterceptorTest {

    @Test
    void convertsHtmlRedirectToKiwoomApiException() {
        MockClientHttpResponse response = response(HttpStatus.FOUND, MediaType.TEXT_HTML);

        assertThatThrownBy(() -> KiwoomResponseValidationInterceptor.validate(response))
                .isInstanceOfSatisfying(KiwoomApiException.class, exception -> {
                    assertThat(exception.getExternalStatusCode()).isEqualTo(302);
                    assertThat(exception.getFailureCode()).isEqualTo("KIWOOM_REDIRECT");
                });
    }

    @Test
    void rejectsNonJsonSuccessResponseBeforeDtoConversion() {
        MockClientHttpResponse response = response(HttpStatus.OK, MediaType.TEXT_HTML);

        assertThatThrownBy(() -> KiwoomResponseValidationInterceptor.validate(response))
                .isInstanceOfSatisfying(KiwoomApiException.class, exception -> {
                    assertThat(exception.getExternalStatusCode()).isEqualTo(200);
                    assertThat(exception.getFailureCode()).isEqualTo("UNEXPECTED_CONTENT_TYPE");
                });
    }

    @Test
    void preservesHttpErrorStatusAndFailureCode() {
        MockClientHttpResponse response = response(
                HttpStatus.SERVICE_UNAVAILABLE,
                MediaType.TEXT_HTML
        );

        assertThatThrownBy(() -> KiwoomResponseValidationInterceptor.validate(response))
                .isInstanceOfSatisfying(KiwoomApiException.class, exception -> {
                    assertThat(exception.getExternalStatusCode()).isEqualTo(503);
                    assertThat(exception.getFailureCode()).isEqualTo("HTTP_503");
                });
    }

    @Test
    void acceptsJsonSuccessResponse() {
        MockClientHttpResponse response = response(HttpStatus.OK, MediaType.APPLICATION_JSON);

        assertThatCode(() -> KiwoomResponseValidationInterceptor.validate(response))
                .doesNotThrowAnyException();
    }

    @Test
    void authHttpApiRejectsHtmlRedirectBeforeDtoConversion() throws Exception {
        byte[] html = "<html><body>service unavailable</body></html>"
                .getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/token", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.getResponseHeaders().set("Location", "/start.html");
                exchange.sendResponseHeaders(HttpStatus.FOUND.value(), html.length);
                exchange.getResponseBody().write(html);
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            KiwoomProperties properties = new KiwoomProperties();
            properties.setUseMock(false);
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setConnectTimeout(Duration.ofSeconds(1));
            properties.setReadTimeout(Duration.ofSeconds(1));

            KiwoomHttpClientConfig config = new KiwoomHttpClientConfig();
            KiwoomAuthHttpApi authHttpApi = config.kiwoomAuthHttpApi(
                    org.springframework.web.client.RestClient.builder(),
                    properties,
                    config.kiwoomRequestFactory(properties)
            );

            Au10001Request request = Au10001Request.builder()
                    .grantType("client_credentials")
                    .appkey("test-app-key")
                    .secretkey("test-secret-key")
                    .build();

            assertThatThrownBy(() -> authHttpApi.issueToken(request))
                    .isInstanceOfSatisfying(KiwoomApiException.class, exception -> {
                        assertThat(exception.getExternalStatusCode()).isEqualTo(302);
                        assertThat(exception.getFailureCode()).isEqualTo("KIWOOM_REDIRECT");
                    });
        } finally {
            server.stop(0);
        }
    }

    private MockClientHttpResponse response(HttpStatus status, MediaType contentType) {
        MockClientHttpResponse response = new MockClientHttpResponse(
                "{}".getBytes(StandardCharsets.UTF_8),
                status
        );
        response.getHeaders().setContentType(contentType);
        return response;
    }
}
