package com.example.stockanalysis.kiwoom.config;

import com.example.stockanalysis.kiwoom.auth.KiwoomAuthHttpApi;
import com.example.stockanalysis.kiwoom.auth.KiwoomTokenProvider;
import com.example.stockanalysis.kiwoom.client.ranking.KiwoomRankingHttpApi;
import com.example.stockanalysis.kiwoom.client.stockinfo.KiwoomStockInfoHttpApi;
import com.example.stockanalysis.kiwoom.client.watchlist.KiwoomWatchlistHttpApi;
import java.net.URI;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KiwoomProperties.class)
public class KiwoomHttpClientConfig {

    private static final String USER_AGENT = "stock-analysis/0.0.1";

    @Bean
    public KiwoomAuthHttpApi kiwoomAuthHttpApi(
            RestClient.Builder builder,
            KiwoomProperties properties,
            @Qualifier("kiwoomRequestFactory") ClientHttpRequestFactory requestFactory
    ) {
        RestClient restClient = baseBuilder(builder, properties, requestFactory)
                .build();
        return createHttpService(restClient, KiwoomAuthHttpApi.class);
    }

    @Bean
    public KiwoomStockInfoHttpApi kiwoomStockInfoHttpApi(
            @Qualifier("kiwoomAuthorizedRestClient") RestClient restClient
    ) {
        return createHttpService(restClient, KiwoomStockInfoHttpApi.class);
    }

    @Bean
    public KiwoomRankingHttpApi kiwoomRankingHttpApi(
            @Qualifier("kiwoomAuthorizedRestClient") RestClient restClient
    ) {
        return createHttpService(restClient, KiwoomRankingHttpApi.class);
    }

    @Bean
    public KiwoomWatchlistHttpApi kiwoomWatchlistHttpApi(
            @Qualifier("kiwoomAuthorizedRestClient") RestClient restClient
    ) {
        return createHttpService(restClient, KiwoomWatchlistHttpApi.class);
    }

    @Bean("kiwoomAuthorizedRestClient")
    public RestClient kiwoomAuthorizedRestClient(
            RestClient.Builder builder,
            KiwoomProperties properties,
            KiwoomTokenProvider tokenProvider,
            com.example.stockanalysis.kiwoom.client.KiwoomRequestPacer pacer,
            @Qualifier("kiwoomRequestFactory") ClientHttpRequestFactory requestFactory
    ) {
        return baseBuilder(builder, properties, requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
                    pacer.acquire();
                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean("kiwoomManualAuthorizationRestClient")
    public RestClient kiwoomManualAuthorizationRestClient(
            RestClient.Builder builder,
            KiwoomProperties properties,
            com.example.stockanalysis.kiwoom.client.KiwoomRequestPacer pacer,
            @Qualifier("kiwoomRequestFactory") ClientHttpRequestFactory requestFactory
    ) {
        return baseBuilder(builder, properties, requestFactory)
                .requestInterceptor((request, body, execution) -> { pacer.acquire(); return execution.execute(request, body); }).build();
    }

    @Bean("kiwoomRequestFactory")
    public ClientHttpRequestFactory kiwoomRequestFactory(KiwoomProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }

    private RestClient.Builder baseBuilder(
            RestClient.Builder builder,
            KiwoomProperties properties,
            ClientHttpRequestFactory requestFactory
    ) {
        validateBaseUrl(properties.activeBaseUrl());

        return builder.clone()
                .baseUrl(properties.activeBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .requestInterceptor(new KiwoomResponseValidationInterceptor());
    }

    private void validateBaseUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("유효한 키움증권 API 주소가 필요합니다.", exception);
        }
    }

    private <T> T createHttpService(RestClient restClient, Class<T> serviceType) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(serviceType);
    }
}
