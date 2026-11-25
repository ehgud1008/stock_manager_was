package com.example.stockanalysis.kiwoom.tr.application;

import com.example.stockanalysis.kiwoom.auth.KiwoomTokenProvider;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionRequest;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionResponse;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrRequestHeader;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrResponseHeader;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrCallResult;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrCatalog;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrClient;
import com.example.stockanalysis.kiwoom.tr.infrastructure.KiwoomTrDefinition;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KiwoomTrExecutionService {

    private final KiwoomTrCatalog catalog;
    private final KiwoomTrClient client;
    private final KiwoomTokenProvider tokenProvider;

    public KiwoomTrExecutionService(
            KiwoomTrCatalog catalog,
            KiwoomTrClient client,
            KiwoomTokenProvider tokenProvider
    ) {
        this.catalog = catalog;
        this.client = client;
        this.tokenProvider = tokenProvider;
    }

    public KiwoomTrExecutionResponse executeWithServerAuthorization(
            String apiId,
            String contYn,
            String nextKey,
            Map<String, Object> body
    ) {
        return execute(new KiwoomTrExecutionRequest(
                new KiwoomTrRequestHeader(
                        apiId,
                        tokenProvider.getAccessToken(),
                        contYn,
                        nextKey
                ),
                body
        ));
    }

    public KiwoomTrExecutionResponse execute(KiwoomTrExecutionRequest request) {
        KiwoomTrRequestHeader header = request.header();
        validateContinuation(header);
        KiwoomTrDefinition definition = catalog.getRequired(header.apiId());
        KiwoomTrCallResult result = client.execute(
                definition,
                header.authorization(),
                header.contYn(),
                header.nextKey(),
                request.body()
        );
        KiwoomContinuation continuation = result.continuation();
        return new KiwoomTrExecutionResponse(
                new KiwoomTrResponseHeader(
                        definition.apiId(),
                        continuation.hasNext() ? "Y" : "N",
                        continuation.nextKey()
                ),
                result.body()
        );
    }

    private void validateContinuation(KiwoomTrRequestHeader header) {
        if ("Y".equals(header.contYn()) && header.nextKey().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "연속조회 요청에는 next-key가 필요합니다."
            );
        }
    }
}
