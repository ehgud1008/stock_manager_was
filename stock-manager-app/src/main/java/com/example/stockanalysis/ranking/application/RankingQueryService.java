package com.example.stockanalysis.ranking.application;

import com.example.stockanalysis.ranking.application.port.out.TradingVolumeRankingPort;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingCriteria;
import com.example.stockanalysis.ranking.domain.TradingVolumeRankingPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RankingQueryService {

    private static final Logger log = LoggerFactory.getLogger(RankingQueryService.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 2_000L;

    private final TradingVolumeRankingPort tradingVolumeRankingPort;

    public RankingQueryService(TradingVolumeRankingPort tradingVolumeRankingPort) {
        this.tradingVolumeRankingPort = tradingVolumeRankingPort;
    }

    public TradingVolumeRankingPage getTradingVolumeRanking(
            TradingVolumeRankingCriteria criteria
    ) {
        long startedAt = System.currentTimeMillis();
        TradingVolumeRankingPage result = null;
        log.info(
                "거래량 순위 조회 시작. market={}, sort={}, includeManagedStocks={}, exchange={}, "
                        + "continuation={}",
                criteria.market(),
                criteria.sort(),
                criteria.includeManagedStocks(),
                criteria.exchange(),
                criteria.pageToken() != null && !criteria.pageToken().isBlank()
        );
        try {
            result = tradingVolumeRankingPort.findTradingVolumeRanking(criteria);
            if (result.items().isEmpty()) {
                log.warn(
                        "거래량 순위 조회 결과 없음. market={}, sort={}, exchange={}",
                        criteria.market(),
                        criteria.sort(),
                        criteria.exchange()
                );
            }
            return result;
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            String operationResult = result == null ? "FAILURE" : "SUCCESS";
            int itemCount = result == null ? 0 : result.items().size();
            boolean hasNext = result != null && result.hasNext();
            if (elapsedMs >= SLOW_OPERATION_THRESHOLD_MS) {
                log.warn(
                        "거래량 순위 조회 종료. result={}, itemCount={}, hasNext={}, elapsedMs={}, "
                                + "thresholdMs={}",
                        operationResult,
                        itemCount,
                        hasNext,
                        elapsedMs,
                        SLOW_OPERATION_THRESHOLD_MS
                );
            } else {
                log.info(
                        "거래량 순위 조회 종료. result={}, itemCount={}, hasNext={}, elapsedMs={}",
                        operationResult,
                        itemCount,
                        hasNext,
                        elapsedMs
                );
            }
        }
    }
}
