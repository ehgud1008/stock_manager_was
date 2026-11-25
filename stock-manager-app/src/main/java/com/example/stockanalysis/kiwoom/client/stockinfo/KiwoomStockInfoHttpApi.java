package com.example.stockanalysis.kiwoom.client.stockinfo;

import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        url = "/api/dostk/stkinfo",
        accept = MediaType.APPLICATION_JSON_VALUE,
        contentType = MediaType.APPLICATION_JSON_VALUE
)
public interface KiwoomStockInfoHttpApi {

    @PostExchange
    ResponseEntity<Ka10001Response> getStockBasicInformation(
            @RequestHeader("api-id") String apiId,
            @RequestHeader("cont-yn") String continuation,
            @RequestHeader("next-key") String nextKey,
            @RequestBody Ka10001Request request
    );
}
