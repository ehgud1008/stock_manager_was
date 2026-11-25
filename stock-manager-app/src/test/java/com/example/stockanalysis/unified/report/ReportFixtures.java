package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.engine.screener.ScreenerBar;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.unified.model.UnifiedAnalysisInput;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class ReportFixtures {
    static final LocalDate DAY=LocalDate.of(2026,10,13);
    static final Instant NOW=Instant.parse("2026-10-13T11:30:00Z");
    static final ObjectMapper MAPPER=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static ReportProperties settings(){return new ReportProperties(true,new BigDecimal("0.9"),new BigDecimal("1000"),new BigDecimal("500"),new BigDecimal("1.5"),new BigDecimal("40"),new BigDecimal("12"),20);}
    static List<LocalDate> dates(){var result=new ArrayList<LocalDate>();for(var d=DAY;result.size()<100;d=d.minusDays(1))if(d.getDayOfWeek().getValue()<6)result.add(d);Collections.reverse(result);return result;}
    record Bundle(UnifiedAnalysisResponse detail,JsonNode input) {}
    static Bundle bundle(String code,boolean qualifying) throws Exception {
        var dates=dates();var candles=new ArrayList<DailyCandle>();
        for(int i=0;i<dates.size();i++){var close=BigDecimal.valueOf((100+i)*100L);candles.add(new DailyCandle(dates.get(i),close,close.add(BigDecimal.valueOf(200)),close.subtract(BigDecimal.valueOf(400)),close,i==99?2000000:1000000));}
        var stock=new StockSnapshot(code,"종목"+code,"KRX",BigDecimal.valueOf(19900));
        var input=new AnalysisInput(stock,AnalysisMode.SWING,DAY,NOW,candles,Map.of());
        var bars=candles.stream().map(c->new ScreenerBar(c.businessDate().atStartOfDay(),c.businessDate().equals(DAY)?DAY.atTime(20,0):c.businessDate().plusDays(1).atStartOfDay(),c.open(),c.high(),c.low(),c.close(),c.volume(),null)).toList();
        var result=new DefaultUnifiedStockAnalysisEngine().analyze(new UnifiedAnalysisInput(code,stock.stockName(),"KRX",DAY.atTime(20,30),bars,Map.of(),null,DAY));
        var detail=new UnifiedAnalysisResponse(result,DataQualityStatus.COMPLETE,List.of(),2000000L,BigDecimal.valueOf(10000),candles,
                new UnifiedAnalysisResponse.EodMetadata("KIWOOM_KRX_DAILY_ADJUSTED",DAY,Instant.parse("2026-10-13T11:00:00Z"),NOW,NOW,NOW,"TEST","KRX-2026-v1"));
        var tree=(ObjectNode)MAPPER.valueToTree(detail);
        var screen=(ObjectNode)tree.path("result").path("screener");screen.put("stage",1);screen.put("previousDistinctStage",qualifying?6:1);screen.put("barsSinceTransition",qualifying?0:20);
        var analysis=(ObjectNode)tree.path("result").path("analysis");
        analysis.put("totalScore",85);
        ((ObjectNode)analysis.path("strategy")).put("action","BUY_CANDIDATE");
        var scenario=new TradeScenario(new BigDecimal("19800"),new BigDecimal("20000"),List.of(new BigDecimal("23000")),new BigDecimal("19000"));
        analysis.set("scenario",MAPPER.valueToTree(scenario));
        var signal=new TradeSignal("test","BREAKOUT",TradeSignal.Side.BUY,TradeSignal.State.CONFIRMED,DAY,DAY,0,3,new BigDecimal("19800"),new BigDecimal("19000"),new BigDecimal("19800"),new BigDecimal("20000"),"IN_RANGE",null,List.of("test"));
        analysis.set("signalReport",MAPPER.valueToTree(new SignalReport("v1",NOW,"UNKNOWN","READY",qualifying?List.of(signal):List.of(),List.of())));
        detail=MAPPER.treeToValue(tree,UnifiedAnalysisResponse.class);
        var records=dates.stream().map(d->Map.of("stockCode",code,"businessDate",d,"values",Map.of("tradingAmount","10000"))).toList();
        return new Bundle(detail,MAPPER.valueToTree(Map.of("input",input,"sources",List.of(Map.of("apiId","ka10081","records",records)))));
    }
    static MarketSnapshot market(){
        var days=dates();var indices=new ArrayList<IndexSnapshot>();
        for(var market:List.of("KOSPI","KOSDAQ")){
            var bars=new ArrayList<IndexBar>();for(int i=0;i<days.size();i++){var p=new BigDecimal("2500").add(BigDecimal.valueOf(i));bars.add(new IndexBar(days.get(i),p,p.add(BigDecimal.TEN),p.subtract(BigDecimal.TEN),p));}
            indices.add(new IndexSnapshot(market,market.equals("KOSPI")?"001":"101","INDEX_POINTS",DAY,NOW,bars,List.of(),List.of()));
        }return new MarketSnapshot(indices,NOW);
    }
}
