package com.example.stockanalysis.kiwoom.client.watchlist;

import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        url = "/api/dostk/watchlist",
        accept = MediaType.APPLICATION_JSON_VALUE,
        contentType = MediaType.APPLICATION_JSON_VALUE
)
public interface KiwoomWatchlistHttpApi {

    @PostExchange
    ResponseEntity<Ka01300Response> getWatchlistGroups(
            @RequestHeader("api-id") String apiId,
            @RequestHeader("cont-yn") String continuation,
            @RequestHeader("next-key") String nextKey,
            @RequestBody Ka01300Request request
    );
}
