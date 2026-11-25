package com.example.stockanalysis.kiwoom.client.account;

import com.example.stockanalysis.kiwoom.client.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Fixed account inquiry operations using the existing server OAuth client. */
@Component
public class KiwoomAccountReadClient {
    public enum Query {
        CASH("kt00001"), HOLDINGS("kt00018"), FILLS("kt00009"), REALIZED("ka10074");
        private final String apiId;
        Query(String apiId) { this.apiId=apiId; }
    }
    public record Page(JsonNode body, String next) {}
    private final RestClient rest;
    private final KiwoomErrorDecoder errors;
    private final Map<Query, KiwoomRequestPacer> accountPacing = new EnumMap<>(Query.class);

    public KiwoomAccountReadClient(@Qualifier("kiwoomAuthorizedRestClient") RestClient rest, KiwoomErrorDecoder errors) {
        this.rest=rest;
        this.errors=errors;
        for (var query : Query.values()) accountPacing.put(query,new KiwoomRequestPacer(1000));
    }

    public Page read(Query query, Map<String,String> parameters, String cursor) {
        accountPacing.get(query).acquire();
        var response=rest.post().uri("/api/dostk/acnt").contentType(MediaType.APPLICATION_JSON)
                .header(KiwoomHeaders.API_ID,query.apiId)
                .header(KiwoomHeaders.CONTINUATION,cursor==null?"N":"Y")
                .header(KiwoomHeaders.NEXT_KEY,cursor==null?"":cursor)
                .body(parameters).retrieve().toEntity(JsonNode.class);
        var body=response.getBody();
        if (body==null || !body.isObject() || !body.path("return_code").isIntegralNumber()) {
            throw new KiwoomApiException("키움 계좌 조회 응답 형식이 올바르지 않습니다.");
        }
        errors.verify(query.apiId,body.path("return_code").asInt(),null);
        var continuation=KiwoomContinuation.from(response.getHeaders());
        if (continuation.hasNext() && (continuation.nextKey().isBlank() || continuation.nextKey().equals(cursor))) {
            throw new KiwoomApiException("키움 계좌 조회의 다음 페이지를 확인할 수 없습니다.");
        }
        return new Page(body,continuation.hasNext()?continuation.nextKey():null);
    }
}
