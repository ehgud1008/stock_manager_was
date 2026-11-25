package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.unified.report.ReportModels.StockDecision;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Deterministic forward observation and conservative daily-bar simulation, never an actual fill. */
@Component
public class PerformanceEngine {
    public static final String VERSION="forward-evaluation-v2";
    public static boolean supported(String version){return VERSION.equals(version)||"forward-evaluation-v1".equals(version);}
    public static List<Integer> horizons(String version){return VERSION.equals(version)?List.of(1,3,5,10,20):List.of(5,10,20);}
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final BigDecimal ONE=BigDecimal.ONE, HUNDRED=new BigDecimal("100");

    public Result evaluate(Input input,List<Observation> observations,Instant now,boolean expired) {
        var known=new HashMap<String,Observation>();
        for(var observation:observations) {
            if(observation.collectedAt().isAfter(now))throw new IllegalArgumentException("FUTURE_OBSERVATION");
            if(known.put(observation.key(),observation)!=null)throw new IllegalArgumentException("DUPLICATE_OBSERVATION");
        }
        var anchors=new HashMap<String,Anchor>();input.anchors().forEach(a->anchors.put(a.code(),a));
        var today=now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        var due=input.dates().stream().filter(d->!d.isAfter(today)).toList();
        var stocks=new ArrayList<StockResult>();
        for(var candidate:input.report().candidates()) {
            var anchor=anchors.get(candidate.code());var horizons=new ArrayList<Horizon>();
            for(int days:horizons(input.policy().version())) {
                var date=input.dates().get(days-1);var o=known.get("STOCK:"+candidate.code()+":"+date);
                String state=date.isAfter(today)?"PENDING":o==null?"MISSING":o.quality();
                var change="VALID".equals(state)?percent(o.candle().close(),anchor.candle().close()):null;
                if(!VERSION.equals(input.policy().version())) {horizons.add(new Horizon(days,state,change));continue;}
                var market=input.report().markets().stream().filter(m->m.market().equals(candidate.market())).findFirst().orElseThrow();
                var index=known.get("INDEX:"+candidate.market()+":"+date);
                String marketState=date.isAfter(today)?"PENDING":index==null?"MISSING":index.quality();
                var marketChange="VALID".equals(marketState)?percent(index.candle().close(),market.referenceClose()):null;
                String pathState=date.isAfter(today)?"PENDING":"VALID";
                BigDecimal high=anchor.candle().close(),low=high;
                if(pathState.equals("VALID"))for(var day:input.dates().subList(0,days)) {
                    var bar=known.get("STOCK:"+candidate.code()+":"+day);
                    if(bar==null||!bar.quality().equals("VALID")){pathState=bar==null?"MISSING":bar.quality();break;}
                    high=high.max(bar.candle().high());low=low.min(bar.candle().low());
                }
                horizons.add(new Horizon(days,state,change,marketState,marketChange,
                        change==null||marketChange==null?null:round(change.subtract(marketChange)),pathState,
                        pathState.equals("VALID")?percent(high,anchor.candle().close()):null,
                        pathState.equals("VALID")?percent(low,anchor.candle().close()):null));
            }
            stocks.add(new StockResult(candidate.code(),candidate.name(),candidate.strategy(),candidate.disposition(),horizons,
                    simulate(candidate,input,known,due)));
        }
        var forecasts=new ArrayList<ForecastResult>();
        for(var market:input.report().markets()) {
            var date=input.dates().get(0);var o=known.get("INDEX:"+market.market()+":"+date);
            String state=date.isAfter(today)?"PENDING":o==null?"MISSING":o.quality();
            var change="VALID".equals(state)?percent(o.candle().close(),market.referenceClose()):null;
            String actual=change==null?null:change.compareTo(input.policy().neutralBandPercent())>0?"상승 우세"
                    :change.compareTo(input.policy().neutralBandPercent().negate())<0?"하락 우세":"중립";
            forecasts.add(new ForecastResult(market.market(),market.bias(),date,state,change,actual,
                    actual==null||!Set.of("상승 우세","하락 우세","중립").contains(market.bias())?null:actual.equals(market.bias())));
        }
        boolean complete=input.dates().get(19).compareTo(today)<=0 && forecasts.stream().allMatch(f->f.state().equals("VALID"))
                && stocks.stream().allMatch(s->s.horizons().stream().allMatch(h->h.state().equals("VALID")
                        && (!VERSION.equals(input.policy().version()) || "VALID".equals(h.marketState())&&"VALID".equals(h.pathState())))
                && !Set.of("PENDING","DATA_GAP","OPEN","OPEN_UNLIQUIDATED").contains(s.trade().state()));
        var grouped=new TreeMap<String,Metrics>();
        stocks.stream().map(StockResult::strategy).distinct().forEach(strategy->grouped.put(strategy,metrics(stocks.stream().filter(s->s.strategy().equals(strategy)).toList(),due.size())));
        return new Result(input.report().runId(),complete?"COMPLETE":expired?"DATA_INCOMPLETE":"WAITING_DATA",input.report().version(),input.policy(),input.captureMode(),now,
                due.isEmpty()?null:due.get(due.size()-1),stocks,forecasts,metrics(stocks,due.size()),grouped,List.of(
                "실제 주문·체결이 아닌 일봉 기반 모의 검증입니다. 과거 전체 시장 백테스트나 독립 검증 통과를 뜻하지 않습니다.",
                "선정일 종가 대비 기간별 가격 변화는 매매 수익률이 아닙니다. 관찰 후보는 모의 진입하지 않습니다.",
                "시장 대비 성과는 같은 기간 종목 수익률에서 소속 시장 지수 수익률을 뺀 %p입니다. 최대 상승·하락폭은 선정일 종가 대비 이후 기간의 고가·저가 기준이며 실제 체결 수익이나 고점 대비 낙폭이 아닙니다.",
                "다음 거래일 시가만 진입 대상으로 삼습니다. 시가가 구간 밖이거나 거래량 0·단일 가격 봉이면 미진입입니다.",
                "같은 봉에서 목표·손절을 모두 만나면 손절 우선, 손절 아래 갭은 시가 청산을 가정합니다. 체결 가격은 보장되지 않습니다.",
                "비용은 고정 가정이며 해당 시점의 실제 세금·수수료를 검증한 값이 아닙니다. 배당 현금은 포함하지 않습니다.",
                "수정주가 기준 변경·정정으로 기준일 가격이 달라지면 미확인으로 남깁니다. 누락·정지·상장폐지를 수익 0으로 처리하지 않습니다.",
                "동일 보고서의 진입 대상에 균등한 가상 자금을 배정하고 미진입분은 현금으로 둡니다. 미청산분을 청산비용 차감 종가로 평가합니다.",
                "동일 보고서 자금 수익률·낙폭은 전체 기간 계좌 성과가 아닙니다. 여러 보고서의 중복 보유나 자금 한도를 합산하지 않습니다.",
                "전망 평가는 전일 대비 다음 거래일 종가 방향만 평가하며 시나리오 전체의 정확도나 예측 확률이 아닙니다.",
                "규칙 변경 시 새 평가 정책을 적용할 신규 보고서가 필요합니다. 기존 평가의 비용·기간·버전·입력은 고정됩니다."));
    }

    private Trade simulate(StockDecision stock,Input input,Map<String,Observation> known,List<LocalDate> due) {
        if(!stock.disposition().equals("ENTRY_CONDITIONAL"))return empty("WATCH_ONLY");
        if(due.isEmpty())return empty("PENDING");
        var scenario=stock.scenario();
        if(scenario==null)return empty("DATA_GAP");
        var first=known.get("STOCK:"+stock.code()+":"+input.dates().get(0));
        if(first==null || !first.quality().equals("VALID"))return empty("DATA_GAP");
        var bar=first.candle();var slip=input.policy().slippageBps().movePointLeft(4);
        var fee=input.policy().roundTripFeeBps().movePointLeft(4).divide(new BigDecimal("2"),MC);
        var entry=bar.open().multiply(ONE.add(slip),MC);
        if(bar.volume()<=0 || bar.high().compareTo(bar.low())==0)return empty("NO_ENTRY_UNCERTAIN_FILL");
        if(bar.open().compareTo(scenario.entryFrom())<0 || entry.compareTo(scenario.entryTo())>0)return empty("NO_ENTRY_GAP");
        var cost=entry.multiply(ONE.add(fee),MC);var path=new ArrayList<BigDecimal>();
        BigDecimal exit=null,marked=null;LocalDate exitDate=null;String reason=null;boolean ambiguous=false,missing=false;
        for(int i=0;i<due.size();i++) {
            var date=due.get(i);
            if(exit!=null){path.add(ONE.add(marked.movePointLeft(2)));continue;}
            var o=known.get("STOCK:"+stock.code()+":"+date);
            if(o==null || !o.quality().equals("VALID") || missing){missing=true;path.add(null);continue;}
            bar=o.candle();BigDecimal rawExit=null;
            var event=TradeOutcomeRules.exit(bar,scenario,i>0,i==input.policy().holdingDays()-1);
            if(event!=null){rawExit=event.price();reason=event.reason();ambiguous=event.ambiguous();}
            var liquidation=(rawExit==null?bar.close():rawExit).multiply(ONE.subtract(slip),MC);
            marked=percent(liquidation.multiply(ONE.subtract(fee),MC),cost);
            path.add(ONE.add(marked.movePointLeft(2)));
            if(rawExit!=null){exit=liquidation;exitDate=date;}
        }
        String state=missing?"DATA_GAP":exit!=null?"CLOSED":due.size()==input.policy().holdingDays()?"OPEN_UNLIQUIDATED":"OPEN";
        return new Trade(state,input.dates().get(0),round(entry),exitDate,round(exit),reason,exit==null?null:marked,missing?null:marked,ambiguous,Collections.unmodifiableList(path));
    }

    private Trade empty(String state){return new Trade(state,null,null,null,null,null,null,null,false,List.of());}
    private Metrics metrics(List<StockResult> stocks,int due) {
        var eligible=stocks.stream().filter(s->s.disposition().equals("ENTRY_CONDITIONAL")).toList();
        var trades=eligible.stream().map(StockResult::trade).toList();
        var closed=trades.stream().filter(t->t.state().equals("CLOSED")).toList();
        var wins=closed.stream().filter(t->t.netReturnPercent().signum()>0).toList();
        var losses=closed.stream().filter(t->t.netReturnPercent().signum()<0).toList();
        int entered=(int)trades.stream().filter(t->t.entryDate()!=null).count();
        int open=(int)trades.stream().filter(t->t.state().startsWith("OPEN")).count();
        int unresolved=(int)trades.stream().filter(t->Set.of("PENDING","DATA_GAP").contains(t.state())).count();
        int noEntry=(int)trades.stream().filter(t->t.state().startsWith("NO_ENTRY")).count();
        BigDecimal cohort=null,drawdown=null;
        if(!eligible.isEmpty() && due>0 && unresolved==0) {
            BigDecimal peak=ONE,worst=BigDecimal.ZERO,level=ONE;
            for(int day=0;day<due;day++) {
                var sum=BigDecimal.ZERO;
                for(var trade:trades)sum=sum.add(trade.state().startsWith("NO_ENTRY")?ONE:trade.equityPath().get(day));
                level=sum.divide(BigDecimal.valueOf(eligible.size()),MC);peak=peak.max(level);
                worst=worst.max(ONE.subtract(level.divide(peak,MC)).multiply(HUNDRED));
            }
            cohort=percent(level,ONE);drawdown=round(worst);
        }
        int streak=0,maxStreak=0;
        // Same-date trades share capital; code order is not an economic sequence. Count losing exit dates instead.
        var byExit=new TreeMap<LocalDate,BigDecimal>();closed.forEach(t->byExit.merge(t.exitDate(),t.netReturnPercent(),BigDecimal::add));
        for(var value:byExit.values()){streak=value.signum()<0?streak+1:0;maxStreak=Math.max(maxStreak,streak);}
        var grossWins=sum(wins);var grossLosses=sum(losses).abs();
        return new Metrics(stocks.size(),eligible.size(),entered,closed.size(),wins.size(),losses.size(),closed.size()-wins.size()-losses.size(),open,unresolved,noEntry,
                ratio(wins.size(),closed.size()),unresolved==0?ratio(entered,eligible.size()):null,mean(closed),mean(wins),mean(losses),grossLosses.signum()==0?null:round(grossWins.divide(grossLosses,MC)),maxStreak,cohort,drawdown);
    }
    private BigDecimal sum(List<Trade> trades){return trades.stream().map(Trade::netReturnPercent).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal mean(List<Trade> trades){return trades.isEmpty()?null:round(sum(trades).divide(BigDecimal.valueOf(trades.size()),MC));}
    private BigDecimal ratio(int n,int d){return d==0?null:round(BigDecimal.valueOf(n).multiply(HUNDRED).divide(BigDecimal.valueOf(d),MC));}
    private static BigDecimal percent(BigDecimal end,BigDecimal start){return round(end.divide(start,MC).subtract(ONE).multiply(HUNDRED));}
    private static BigDecimal round(BigDecimal value){return value==null?null:value.setScale(6,RoundingMode.HALF_UP);}
}
