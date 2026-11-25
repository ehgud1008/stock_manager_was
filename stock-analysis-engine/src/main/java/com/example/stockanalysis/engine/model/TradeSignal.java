package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TradeSignal(String id, String type, Side side, State state, LocalDate occurredOn,
                          LocalDate evaluatedOn, int ageBars, int validForBars, BigDecimal triggerPrice,
                          BigDecimal invalidationPrice, BigDecimal entryFrom, BigDecimal entryTo,
                          String entryStatus, LocalDate endedOn, List<String> reasons) {
    public enum Side { BUY, SELL, TAKE_PROFIT }
    public enum State { PRELIMINARY, CONFIRMED, ACTIVE, INVALIDATED, EXPIRED }
    public TradeSignal { reasons = List.copyOf(reasons); }
}
