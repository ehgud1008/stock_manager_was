package com.example.stockanalysis.backtest;

import com.example.stockanalysis.shared.exception.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class MarketStudy {
    enum Market { KOSPI, KOSDAQ, ALL }
    enum Strategy { BREAKOUT, VOLUME_BREAKOUT }
    enum MissingSettlement { UNRESOLVED, WRITE_OFF }
    record Spec(Market market, Strategy strategy, LocalDate from, LocalDate to, long capital, int positions,
                int hold, int feeBps, int slipBps, long minTurnover, MissingSettlement missingSettlement) {
        Spec {
            if (missingSettlement == null) missingSettlement = MissingSettlement.UNRESOLVED;
        }
        void validate(LocalDate today) {
            if (market == null || strategy == null || from == null || to == null || to.isBefore(from)
                    || from.isBefore(LocalDate.of(2010, 3, 1)) || !to.isBefore(today)
                    || capital < 10000 || capital > 1_000_000_000_000L || positions < 1 || positions > 100
                    || hold < 1 || hold > 25 || feeBps < 0 || feeBps > 500 || slipBps < 0 || slipBps > 500
                    || minTurnover < 0) throw invalid("시장·전략·기간 또는 계좌 설정을 확인하세요. 종료일은 어제까지 선택할 수 있습니다.");
        }
    }
    record Quote(String code, String name, long open, long high, long low, long close, long volume,
                 long turnover, long previousClose) {}
    record Pick(String code, String name, LocalDate signalDate, long close, long resistance, long turnover) {}
    record Trade(Pick pick, LocalDate entryDate, BigDecimal entry, long quantity, LocalDate exitDate,
                 BigDecimal exit, BigDecimal net, String status) {}
    record Day(LocalDate date, int universe, int eligible, int signals, BigDecimal equity) {}
    record Metrics(int sessions, int signals, int entered, int closed, int wins, BigDecimal winRate,
                   BigDecimal averageNet, BigDecimal returnPct, BigDecimal maxDrawdown, int open,
                   BigDecimal averageWin, BigDecimal averageLoss, BigDecimal profitFactor, BigDecimal meanTradeReturn,
                   int unconfirmedWeekdays, int writeOffs) {}
    record Result(String version, Spec spec, Metrics metrics, List<Day> days, List<Trade> trades,
                  List<LocalDate> unconfirmedEmptyDates, String inputHash) {}
    record Job(UUID id, Spec spec, Instant created, String state, LocalDate progress, String message, Metrics metrics) {}
    static BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_REQUEST, message); }
}
