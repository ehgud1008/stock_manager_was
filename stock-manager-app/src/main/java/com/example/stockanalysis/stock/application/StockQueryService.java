package com.example.stockanalysis.stock.application;

import com.example.stockanalysis.stock.application.port.out.StockInformationPort;
import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class StockQueryService {

    private static final Logger log = LoggerFactory.getLogger(StockQueryService.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 2_000L;

    private final StockInformationPort stockInformationPort;

    public StockQueryService(StockInformationPort stockInformationPort) {
        this.stockInformationPort = stockInformationPort;
    }

    public StockProfile getStockProfile(String stockCode) {
        StockCode code = StockCode.of(stockCode);
        long startedAt = System.currentTimeMillis();
        boolean success = false;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("stockCode", code.value())) {
            log.info("종목 기본정보 조회 시작. stockCode={}", code.value());
            StockProfile profile = stockInformationPort.getStockProfile(code);
            success = true;
            return profile;
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            if (elapsedMs >= SLOW_OPERATION_THRESHOLD_MS) {
                log.warn(
                        "종목 기본정보 조회 종료. stockCode={}, result={}, elapsedMs={}, thresholdMs={}",
                        code.value(),
                        success ? "SUCCESS" : "FAILURE",
                        elapsedMs,
                        SLOW_OPERATION_THRESHOLD_MS
                );
            } else {
                log.info(
                        "종목 기본정보 조회 종료. stockCode={}, result={}, elapsedMs={}",
                        code.value(),
                        success ? "SUCCESS" : "FAILURE",
                        elapsedMs
                );
            }
        }
    }
}
