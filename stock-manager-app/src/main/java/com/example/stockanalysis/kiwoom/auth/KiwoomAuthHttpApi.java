package com.example.stockanalysis.kiwoom.auth;

import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Request;
import com.example.stockanalysis.kiwoom.contract.tr.au10001.Au10001Response;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        accept = MediaType.APPLICATION_JSON_VALUE,
        contentType = MediaType.APPLICATION_JSON_VALUE
)
public interface KiwoomAuthHttpApi {

    @PostExchange("/oauth2/token")
    Au10001Response issueToken(@RequestBody Au10001Request request);
}
