package com.example.stockanalysis.unified.performance;

import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.TradeScenario;
import java.math.*;

/** Shared daily-bar exit facts and cost arithmetic. Ambiguous paths remain explicitly marked. */
public final class TradeOutcomeRules {
    private TradeOutcomeRules() {}
    public record Exit(String reason, BigDecimal price, boolean ambiguous) {}
    public static Exit exit(DailyCandle bar, TradeScenario scenario, boolean afterEntryDay, boolean lastDay) {
        if(bar.volume()<=0 || bar.high().compareTo(bar.low())<=0)return null;
        var target=scenario.targets().get(0);
        if(afterEntryDay && bar.open().compareTo(scenario.stopLoss())<=0)return new Exit("GAP_STOP",bar.open(),false);
        if(afterEntryDay && bar.open().compareTo(target)>=0)return new Exit("TARGET",target,false);
        if(bar.low().compareTo(scenario.stopLoss())<=0)return new Exit("STOP",scenario.stopLoss(),bar.high().compareTo(target)>=0);
        if(bar.high().compareTo(target)>=0)return new Exit("TARGET",target,false);
        return lastDay?new Exit("TIME_EXIT",bar.close(),false):null;
    }
    public static BigDecimal entry(BigDecimal open,BigDecimal slipBps){return open.multiply(BigDecimal.ONE.add(slipBps.movePointLeft(4)));}
    public static BigDecimal exitPrice(BigDecimal price,BigDecimal slipBps){return price.multiply(BigDecimal.ONE.subtract(slipBps.movePointLeft(4)));}
    public static BigDecimal change(BigDecimal end,BigDecimal start){
        return end==null||start==null||start.signum()<=0?null:end.divide(start,MathContext.DECIMAL128).subtract(BigDecimal.ONE).movePointRight(2).setScale(6,RoundingMode.HALF_UP);
    }
    public static BigDecimal net(BigDecimal entry,BigDecimal exit,BigDecimal feeBps){
        var fee=feeBps.movePointLeft(4).divide(BigDecimal.valueOf(2));
        return change(exit.multiply(BigDecimal.ONE.subtract(fee)),entry.multiply(BigDecimal.ONE.add(fee)));
    }
}
