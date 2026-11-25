package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.unified.performance.PerformanceModels.Anchor;
import com.example.stockanalysis.unified.performance.PerformanceModels.Observation;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.unified.eod.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.core.io.DefaultResourceLoader;

final class PortfolioFixtures {
    static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static final LocalDate DAY=LocalDate.of(2026,10,13);
    static BigDecimal bd(String s){return new BigDecimal(s);}
    static Instant time(LocalDate d){return d.atTime(20,30).atZone(ZoneId.of("Asia/Seoul")).toInstant();}
    static List<LocalDate> dates(){var list=new ArrayList<LocalDate>();for(var d=DAY.plusDays(1);list.size()<21;d=d.plusDays(1))if(d.getDayOfWeek().getValue()<6)list.add(d);return list;}
    static DailyCandle bar(LocalDate d,String o,String h,String l,String c,long volume){return new DailyCandle(d,bd(o),bd(h),bd(l),bd(c),volume);}
    static StockDecision stock(String code){return stock(code,"ENTRY_CONDITIONAL","BREAKOUT");}
    static StockDecision stock(String code,String disposition,String strategy){
        var evidence=new ArrayList<Evidence>();
        for(var key:List.of("PRICE_EMA20_DISTANCE","AMOUNT_TODAY","AMOUNT_AVERAGE20","RELATIVE_RETURN20","NO_SELL_CONFLICT","EMA5_SLOPE_3","EMA20_SLOPE_3","VOLUME_RATIO20","ENTRY_RANGE","BUY_PATTERN","NET_REWARD_RISK","EMA5_ACCELERATION"))
            evidence.add(new Evidence(key,key,bd("1"),"number",">=",bd("1"),true,"POSITIVE","ENTRY","VALID","test",DAY,time(DAY),"v1"));
        return new StockDecision(code,code,"KOSPI",disposition,strategy,evidence,List.of(),new TradeScenario(bd("95"),bd("105"),List.of(bd("120")),bd("90")),bd("2"),bd("1"),bd("5000"),bd("1"),true,1,false);
    }
    static Cohort cohort(String id,LocalDate date,StockDecision...stocks){
        var market=new MarketView("KOSPI",bd("1"),bd("1"),bd("60"),bd("60"),stocks.length,stocks.length,Map.of(),1,"상승 우세","base","up","down",bd("100"),bd("90"),bd("120"),List.of());
        var forecast=date.plusDays(1);while(forecast.getDayOfWeek().getValue()>5)forecast=forecast.plusDays(1);
        var report=new DailyReport(id,"READY",date,forecast,time(date),time(date),"candidate-market-v1/test","UNVALIDATED_RULES","ALL",List.of(market),List.of(stocks),stocks.length,stocks.length,List.of());
        return new Cohort(report,List.of(stocks),List.of(stocks),Arrays.stream(stocks).map(s->new Anchor(s.code(),s.market(),"KIWOOM_KRX_DAILY_ADJUSTED",bar(date,"100","102","98","100",1000))).toList());
    }
    static Input input(int positions,String percent,Cohort...cohorts){return new Input(new Policy(PortfolioEngine.VERSION,bd("10000"),positions,bd(percent),bd("30"),bd("5"),bd("0.3"),20,"KRX-2026-v1"),List.of(cohorts),dates(),time(DAY),"FORWARD_CAPTURE");}
    static List<Observed> observations(Input input){
        var values=new ArrayList<Observed>();
        for(var c:input.cohorts()){
            for(var a:c.anchors())for(var d:input.dates())if(!d.isBefore(c.report().forecastDate()))values.add(observed(c.report().runId(),a.code(),bar(d,"100","105","95","102",1000)));
            var d=c.report().forecastDate();values.add(new Observed(c.report().runId(),new Observation("INDEX","KOSPI",d,"VALID",bar(d,"100","102","99","101",0),"source",time(d),"test")));
        }return values;
    }
    static Observed observed(String id,String code,DailyCandle candle){return new Observed(id,new Observation("STOCK",code,candle.businessDate(),"VALID",candle,"source",time(candle.businessDate()),"test"));}
    static void replace(List<Observed> data,String id,String code,DailyCandle candle){var o=observed(id,code,candle);data.removeIf(x->x.key().equals(o.key()));data.add(o);}
    static Instant end(){return time(dates().get(20)).plusSeconds(60);}
    static TradingCalendar calendar(){return new TradingCalendar(new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofMinutes(1),3,Duration.ofMinutes(2)),new DefaultResourceLoader());}
}
