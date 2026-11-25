package com.example.stockanalysis.kiwoom.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.kiwoom.auth.KiwoomAuthHttpApi;
import com.example.stockanalysis.kiwoom.auth.KiwoomTokenProvider;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class KiwoomEnvironmentRoutingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void routesTokenRequestAndCredentialsToSelectedEnvironment(boolean useMock)
            throws Exception {
        TestTokenServer production = startTokenServer("production-token");
        TestTokenServer mock = startTokenServer("mock-token");

        try {
            KiwoomProperties properties = properties(production, mock, useMock);
            KiwoomHttpClientConfig config = new KiwoomHttpClientConfig();
            KiwoomAuthHttpApi authHttpApi = config.kiwoomAuthHttpApi(
                    RestClient.builder(),
                    properties,
                    config.kiwoomRequestFactory(properties)
            );
            KiwoomTokenProvider tokenProvider = new KiwoomTokenProvider(
                    authHttpApi,
                    properties,
                    new KiwoomErrorDecoder()
            );

            String token = tokenProvider.getAccessToken();

            TestTokenServer selected = useMock ? mock : production;
            TestTokenServer unselected = useMock ? production : mock;
            assertThat(token).isEqualTo(useMock ? "mock-token" : "production-token");
            assertThat(selected.requestBodies()).hasSize(1);
            assertThat(unselected.requestBodies()).isEmpty();

            JsonNode request = objectMapper.readTree(selected.requestBodies().get(0));
            assertThat(request.path("appkey").asText())
                    .isEqualTo(useMock ? "mock-app-key" : "production-app-key");
            assertThat(request.path("secretkey").asText())
                    .isEqualTo(useMock ? "mock-secret-key" : "production-secret-key");
        } finally {
            production.server().stop(0);
            mock.server().stop(0);
        }
    }

    private KiwoomProperties properties(
            TestTokenServer production,
            TestTokenServer mock,
            boolean useMock
    ) {
        KiwoomProperties properties = new KiwoomProperties();
        properties.setUseMock(useMock);
        properties.setBaseUrl(baseUrl(production));
        properties.setMockBaseUrl(baseUrl(mock));
        properties.setAppKey("production-app-key");
        properties.setSecretKey("production-secret-key");
        properties.setMockAppKey("mock-app-key");
        properties.setMockSecretKey("mock-secret-key");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        properties.setTokenRefreshMargin(Duration.ZERO);
        return properties;
    }

    private TestTokenServer startTokenServer(String token) throws Exception {
        List<String> requestBodies = new ArrayList<>();
        byte[] response = ("""
                {
                  "expires_dt": "20991231235959",
                  "token_type": "bearer",
                  "token": "%s",
                  "return_code": 0,
                  "return_msg": "정상처리"
                }
                """).formatted(token).getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/token", exchange -> {
            try {
                requestBodies.add(new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                ));
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return new TestTokenServer(server, requestBodies);
    }

    private String baseUrl(TestTokenServer server) {
        return "http://127.0.0.1:" + server.server().getAddress().getPort();
    }

    private record TestTokenServer(HttpServer server, List<String> requestBodies) {
    }
}
