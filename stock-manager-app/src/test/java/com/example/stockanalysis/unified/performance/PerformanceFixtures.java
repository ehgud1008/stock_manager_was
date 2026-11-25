package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class PerformanceFixtures {
    static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static final LocalDate DAY=LocalDate.of(2026,10,13);
    static final Instant NOW=DAY.atTime(20,30).atZone(ZoneId.of("Asia/Seoul")).toInstant();
    static BigDecimal bd(String s){return new BigDecimal(s);}
    static DailyCandle bar(LocalDate day,String open,String high,String low,String close,long volume){return new DailyCandle(day,bd(open),bd(high),bd(low),bd(close),volume);}
    static DailyCandle base(){return bar(DAY,"100","102","98","100",1000000);}
    static List<LocalDate> dates(){var dates=new ArrayList<LocalDate>();for(var day=DAY.plusDays(1);dates.size()<20;day=day.plusDays(1))if(day.getDayOfWeek().getValue()<6)dates.add(day);return dates;}
    static Instant collected(LocalDate date){return date.atTime(20,30).atZone(ZoneId.of("Asia/Seoul")).toInstant();}
    static Policy policy(){return new Policy(PerformanceEngine.VERSION,bd("30"),bd("5"),bd("0.3"),20,"KRX-2026-v1");}
    static StockDecision candidate(String code,String disposition){return new StockDecision(code,"종목"+code,"KOSPI",disposition,"BREAKOUT",List.of(),List.of(),new TradeScenario(bd("95"),bd("105"),List.of(bd("120")),bd("90")),bd("2"),bd("1"),bd("5000"),bd("1"),true,1,false);}
    static Input input(String id,StockDecision...stocks){
        var market=new MarketView("KOSPI",bd("1"),bd("1"),bd("60"),bd("60"),100,100,Map.of(),1,"상승 우세","base","up","down",bd("100"),bd("90"),bd("120"),List.of());
        var report=new DailyReport(id,"READY",DAY,dates().get(0),NOW,NOW,"report-v1","UNVALIDATED_RULES","ALL",List.of(market),List.of(stocks),100,100,List.of());
        return new Input(report,policy(),dates(),Arrays.stream(stocks).map(s->new Anchor(s.code(),s.market(),"KIWOOM_KRX_DAILY_ADJUSTED",base())).toList(),"FORWARD_CAPTURE",NOW);
    }
    static List<Observation> observations(Input input){
        var values=new ArrayList<Observation>();
        for(var stock:input.report().candidates())for(var day:dates())values.add(observation(stock.code(),bar(day,"100","105","95","102",1000000)));
        for(int horizon:List.of(1,3,5,10,20)) {var date=dates().get(horizon-1);
            values.add(new Observation("INDEX","KOSPI",date,"VALID",bar(date,"100","102","99","101",0),"market-source",collected(date),"test"));}
        return values;
    }
    static Observation observation(String code,DailyCandle candle){return new Observation("STOCK",code,candle.businessDate(),"VALID",candle,"stock-source",collected(candle.businessDate()),"test");}
    static void replace(List<Observation> all,String code,DailyCandle candle){all.removeIf(o->o.kind().equals("STOCK")&&o.code().equals(code)&&o.date().equals(candle.businessDate()));all.add(observation(code,candle));}
    static Instant finalTime(){return collected(dates().get(19)).plusSeconds(60);}
}
