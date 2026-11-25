package com.example.stockanalysis.watchlist.application;

import com.example.stockanalysis.watchlist.application.port.out.WatchlistQueryPort;
import com.example.stockanalysis.watchlist.domain.WatchlistGroup;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WatchlistQueryService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistQueryService.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 2_000L;

    private final WatchlistQueryPort watchlistQueryPort;

    public WatchlistQueryService(WatchlistQueryPort watchlistQueryPort) {
        this.watchlistQueryPort = watchlistQueryPort;
    }

    public List<WatchlistGroup> getGroups() {
        long startedAt = System.currentTimeMillis();
        List<WatchlistGroup> groups = null;
        log.info("관심종목 그룹 조회 시작.");
        try {
            groups = watchlistQueryPort.findGroups();
            if (groups.isEmpty()) {
                log.warn("관심종목 그룹 조회 결과 없음.");
            }
            return groups;
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            String result = groups == null ? "FAILURE" : "SUCCESS";
            int groupCount = groups == null ? 0 : groups.size();
            if (elapsedMs >= SLOW_OPERATION_THRESHOLD_MS) {
                log.warn(
                        "관심종목 그룹 조회 종료. result={}, groupCount={}, elapsedMs={}, thresholdMs={}",
                        result,
                        groupCount,
                        elapsedMs,
                        SLOW_OPERATION_THRESHOLD_MS
                );
            } else {
                log.info(
                        "관심종목 그룹 조회 종료. result={}, groupCount={}, elapsedMs={}",
                        result,
                        groupCount,
                        elapsedMs
                );
            }
        }
    }
}
