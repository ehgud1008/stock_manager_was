package com.example.stockanalysis.unified.application;

import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class UnifiedTradeEligibilityTest {
    @Test void excludesOpeningPriceThrough2000ButPreservesMarketDataAndSellSignals() {
        var date=LocalDate.of(2026,10,13); var asOf=date.atTime(20,30);
        for(var price:List.of("1999","2000","2000.01")) {
            var open=new BigDecimal(price);
            var candle=new DailyCandle(date,open,new BigDecimal("4000"),new BigDecimal("1000"),new BigDecimal("3000"),100000);
            var bar=new ScreenerBar(date.atStartOfDay(),date.atTime(20,0),candle.open(),candle.high(),candle.low(),candle.close(),candle.volume(),null);
            var base=new DefaultUnifiedStockAnalysisEngine().analyze(new UnifiedAnalysisInput("005930","종목","KRX",asOf,List.of(bar),Map.of(),null,date));
            var signals=new ArrayList<TradeSignal>();
            for(var side:TradeSignal.Side.values()) signals.add(new TradeSignal("v1",side==TradeSignal.Side.BUY?"BREAKOUT":"TREND_BREAKDOWN",side,
                    TradeSignal.State.CONFIRMED,date,date,0,3,new BigDecimal("3000"),new BigDecimal("2500"),new BigDecimal("2900"),new BigDecimal("3100"),"IN_RANGE",null,List.of("test")));
            var analysis=new AnalysisResult("test",AnalysisMode.SWING,new BigDecimal("85"),List.of(),
                    new StrategyDecision("BUY","매수",StrategyAction.BUY_CANDIDATE,List.of("test")),
                    new TradeScenario(new BigDecimal("2900"),new BigDecimal("3100"),List.of(new BigDecimal("4000")),new BigDecimal("2500")),List.of(),
                    new SignalReport("test",Instant.parse("2026-10-13T11:30:00Z"),"UNHELD","COMPLETE",signals,List.of()));
            var original=new UnifiedAnalysisResult(base.engineVersion(),base.stock(),base.asOf(),base.priceDate(),base.dailyBarCount(),base.weeklyBarCount(),base.monthlyBarCount(),analysis,base.screener(),base.warnings());
            var result=UnifiedTradeEligibility.apply(original,List.of(candle));
            if(open.compareTo(new BigDecimal("2000"))>0) { assertThat(result).isSameAs(original); continue; }
            assertThat(result.analysis().strategy().action()).isEqualTo(StrategyAction.AVOID);
            assertThat(result.analysis().scenario()).isNull();
            assertThat(result.analysis().signalReport().signals()).extracting(TradeSignal::side).containsExactly(TradeSignal.Side.SELL,TradeSignal.Side.TAKE_PROFIT);
            assertThat(result.analysis().totalScore()).isEqualByComparingTo("85");
            assertThat(result.screener()).isSameAs(original.screener());
            assertThat(result.stock().currentPrice()).isEqualByComparingTo("3000");
            assertThat(result.analysis().warnings()).contains(UnifiedTradeEligibility.EXCLUSION_REASON);
        }
    }
}
