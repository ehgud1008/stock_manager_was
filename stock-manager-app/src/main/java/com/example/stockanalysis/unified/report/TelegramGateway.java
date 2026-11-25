package com.example.stockanalysis.unified.report;

public interface TelegramGateway {
    record Result(String state,String messageId,String errorCode,long retryAfterSeconds) {}
    Result send(String destination,String text);
}
