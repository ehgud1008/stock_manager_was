package com.example.stockanalysis.kiwoom.client.ranking;

import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10030.Ka10030Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        url = "/api/dostk/rkinfo",
        accept = MediaType.APPLICATION_JSON_VALUE,
        contentType = MediaType.APPLICATION_JSON_VALUE
)
public interface KiwoomRankingHttpApi {

    @PostExchange
    ResponseEntity<Ka10030Response> getTradingVolumeRanking(
            @RequestHeader("api-id") String apiId,
            @RequestHeader("cont-yn") String continuation,
            @RequestHeader("next-key") String nextKey,
            @RequestBody Ka10030Request request
    );
}
