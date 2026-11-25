package com.example.stockanalysis.kiwoom.config;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

final class KiwoomResponseValidationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException exception) {
            throw new KiwoomApiException(
                    "키움증권 API에 연결하지 못했습니다.",
                    null,
                    "IO_ERROR",
                    exception
            );
        }

        try {
            validate(response);
            return response;
        } catch (RuntimeException exception) {
            response.close();
            throw exception;
        }
    }

    static void validate(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        int statusCode = status.value();

        if (status.is3xxRedirection()) {
            throw new KiwoomApiException(
                    "키움증권 API가 리다이렉트 응답을 반환했습니다. HTTP " + statusCode,
                    statusCode,
                    "KIWOOM_REDIRECT"
            );
        }

        if (status.isError()) {
            throw new KiwoomApiException(
                    "키움증권 API 통신에 실패했습니다. HTTP " + statusCode,
                    statusCode,
                    "HTTP_" + statusCode
            );
        }

        if (statusCode == 204 || statusCode == 205) {
            return;
        }

        MediaType contentType = response.getHeaders().getContentType();
        if (!isJson(contentType)) {
            throw new KiwoomApiException(
                    "키움증권 API가 JSON이 아닌 응답을 반환했습니다.",
                    statusCode,
                    "UNEXPECTED_CONTENT_TYPE"
            );
        }
    }

    private static boolean isJson(MediaType contentType) {
        return contentType != null
                && (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || contentType.getSubtype().endsWith("+json"));
    }
}
