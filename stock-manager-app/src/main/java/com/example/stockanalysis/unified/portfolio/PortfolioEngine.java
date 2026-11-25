package com.example.stockanalysis.unified.portfolio;

import static com.example.stockanalysis.unified.portfolio.PortfolioModels.*;
import com.example.stockanalysis.unified.performance.PerformanceModels.Observation;
import com.example.stockanalysis.unified.report.ReportModels.StockDecision;
import com.example.stockanalysis.engine.model.DailyCandle;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Replays one cash account per variant. No live quotes, future rankings or implied fills. */
@Component
public class PortfolioEngine {
    public static final String VERSION="portfolio-replay-v3-tier";
    public static boolean supported(String version){return VERSION.equals(version)||"portfolio-replay-v2".equals(version)||"portfolio-replay-v1".equals(version);}
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final BigDecimal ONE=BigDecimal.ONE, ZERO=BigDecimal.ZERO, HUNDRED=new BigDecimal("100");
    private static final Set<String> TAGS=Set.of("EMA5_ACCELERATION","EMA5_20_EXPANSION","FOREIGN_FLOW5","INSTITUTION_FLOW5","STAGE_6_TO_1");
    private static final class Position {
        Cohort cohort; StockDecision stock; LocalDate entered,exited; int entryIndex; long quantity;
        BigDecimal entry,exit,cost,profit,marked; String state="OPEN",reason; boolean ambiguous;
    }

    public Result evaluate(Input input,List<Observed> observations,Instant now,boolean expired) {
        if(!supported(input.policy().version()))throw new IllegalArgumentException("UNSUPPORTED_REPLAY_VERSION");
        var known=new HashMap<String,Observation>();
        for(var item:observations) {
            var o=item.observation();
            if(o.collectedAt().isAfter(now) || known.put(item.key(),o)!=null)throw new IllegalArgumentException("FUTURE_OR_DUPLICATE_OBSERVATION");
        }
        var today=now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        var dates=input.dates().stream().filter(d->!d.isAfter(today)).toList();
        var variants=new ArrayList<Variant>();
        for(boolean filter:List.of(true,false))for(boolean stress:List.of(false,true))
            variants.add(replay(input,known,dates,filter,stress,null));
        if(!"portfolio-replay-v1".equals(input.policy().version()))for(var name:ResearchSelections.VARIANTS)
            if((!name.startsWith("TIER_")||VERSION.equals(input.policy().version()))&&input.cohorts().stream().allMatch(c->c.comparisons().containsKey(name)))for(boolean stress:List.of(false,true))
                variants.add(replay(input,known,dates,true,stress,name));
        var forecasts=new ArrayList<Forecast>();
        for(var cohort:input.cohorts())for(var market:cohort.report().markets()) {
            var report=cohort.report();var date=report.forecastDate();
            var o=known.get(report.runId()+":INDEX:"+market.market()+":"+date);
            var change=o!=null&&o.quality().equals("VALID")?pct(o.candle().close(),market.referenceClose()):null;
            var actual=change==null?null:change.compareTo(input.policy().neutralBandPercent())>0?"상승 우세":change.compareTo(input.policy().neutralBandPercent().negate())<0?"하락 우세":"중립";
            forecasts.add(new Forecast(report.runId(),market.market(),date,market.bias(),actual,round(change),actual==null?null:actual.equals(market.bias()),
                    date.isAfter(today)?"PENDING":o==null?"MISSING":o.quality()));
        }
        boolean complete=dates.size()==input.dates().size() && variants.stream().allMatch(v->v.state().equals("EVALUATED"))
                && forecasts.stream().allMatch(f->f.state().equals("VALID"));
        return new Result(complete?"COMPLETE":expired?"DATA_INCOMPLETE":"WAITING_DATA",now,dates.isEmpty()?null:dates.get(dates.size()-1),
                variants,forecasts,List.of(
                "저장 보고서 재생에 의한 모의계좌입니다. 실제 주문·체결, 과거 전체 시장 백테스트, 검증된 수익성이 아닙니다.",
                "보고서 목록·비용·순위·달력·자금 정책은 등록 시 고정됩니다. 사후 등록 결과는 독립 검증이 아닙니다.",
                "같은 종목 보유 중 추가 매수와 같은 날 청산 후 재진입을 금지합니다. 매도대금은 다음 거래일부터 매수에 사용합니다.",
                "이전 거래일 평가액의 설정 비율 한도와 가용 현금 내에서 정수 주식을 매수합니다. 신용·레버리지는 없습니다.",
                "다음 거래일 시가만 진입합니다. 동일 일봉 목표·손절 동시 도달은 손절 우선입니다. 기간 종료 후 체결 불명확 시 계속 보유로 기록합니다.",
                "필요한 진입 또는 보유 가격이 누락되면 그 지점부터 계좌 재생을 보류합니다. 미확인 손실을 제외한 수익률은 만들지 않습니다.",
                "COST_STRESS는 선정 조건을 고정하고 편도 체결 차이만 두 배로 적용한 민감도 비교입니다.",
                "근거별 통계는 실제 진입 표본의 연관성 집계이며 해당 근거의 인과적 효과나 상승 확률이 아닙니다.",
                "시장 필터 비교는 보고서 버전별 시장 보류 조건만 해제합니다. 상대강도 비교와 별개이며 순위·상한·비용·자금 조건은 같습니다.",
                "추가 비교는 v4 저장 자료에서 한 조건씩 변경합니다. 어떤 보고서에서든 비교 자료가 없으면 해당 비교안 전체를 제외하고 입력의 보류 사유를 표시합니다.",
                "신호 기간 비교는 0~3봉 대 0~4봉이며 신호 자체 만료는 연장하지 않습니다. 패턴별 거래량은 검증 전 가설입니다.",
                "표본 충분성·독립적인 이후 기간 성과는 자동 판정하지 않습니다. 수익률이나 후보 수만으로 운영 정책을 자동 변경하지 않습니다.",
                "관찰 후보는 매매하지 않습니다. 배당 현금·기업행사·실제 호가와 체결 대기열은 재현하지 않습니다."));
    }

    private Variant replay(Input input,Map<String,Observation> known,List<LocalDate> dates,boolean filter,boolean stress,String comparison) {
        var p=input.policy();var slip=p.slippageBps().multiply(stress?new BigDecimal("2"):ONE).movePointLeft(4);
        var fee=p.roundTripFeeBps().movePointLeft(4).divide(new BigDecimal("2"),MC);
        var cash=p.initialCash();var priorEquity=cash;var peak=cash;var worst=ZERO;
        var open=new LinkedHashMap<String,Position>();var all=new ArrayList<Position>();var curve=new ArrayList<Point>();
        var scheduled=new TreeMap<LocalDate,Cohort>();input.cohorts().forEach(c->scheduled.put(c.report().forecastDate(),c));
        boolean blocked=false;
        for(int dayIndex=0;dayIndex<dates.size();dayIndex++) {
            var day=dates.get(dayIndex);
            if(blocked){curve.add(new Point(day,round(cash),null,null,open.size()));continue;}
            var heldAtOpen=new HashSet<>(open.keySet());var buyBudget=cash;
            // Validate all overnight positions before spending any new cash on this date.
            for(var position:open.values())if(!valid(bar(known,position.cohort,position.stock.code(),day))) {
                position.state="DATA_GAP";position.reason="MISSING_HOLDING_PATH";position.marked=null;blocked=true;
            }
            if(blocked){curve.add(new Point(day,round(cash),null,null,open.size()));continue;}
            // Overnight gap exits are processed first, but proceeds are not spendable today.
            for(var position:new ArrayList<>(open.values())) {
                var candle=bar(known,position.cohort,position.stock.code(),day).candle();var scenario=position.stock.scenario();
                if(tradable(candle) && (candle.open().compareTo(scenario.stopLoss())<=0 || candle.open().compareTo(scenario.targets().get(0))>=0)) {
                    boolean stop=candle.open().compareTo(scenario.stopLoss())<=0;
                    cash=cash.add(close(position,day,stop?candle.open():scenario.targets().get(0),stop?"GAP_STOP":"TARGET",false,slip,fee));open.remove(position.stock.code());
                }
            }
            var cohort=scheduled.get(day);
            if(cohort!=null)for(var stock:comparison!=null?cohort.comparisons().get(comparison):filter?cohort.baseline():cohort.withoutMarketFilter()) {
                if(!stock.disposition().equals("ENTRY_CONDITIONAL"))continue;
                var order=new Position();order.cohort=cohort;order.stock=stock;all.add(order);
                if(heldAtOpen.contains(stock.code())||open.containsKey(stock.code())) {skip(order,"DUPLICATE_HOLDING");continue;}
                if(open.size()>=p.maximumPositions()){skip(order,"POSITION_LIMIT");continue;}
                var budget=priorEquity.multiply(p.positionPercent(),MC).movePointLeft(2).min(buyBudget).min(cash);
                if(budget.signum()<=0){skip(order,"INSUFFICIENT_CASH");continue;}
                var o=bar(known,cohort,stock.code(),day);
                if(!valid(o)){order.state="DATA_GAP";order.reason="MISSING_ENTRY_PRICE";blocked=true;break;}
                var candle=o.candle();var scenario=stock.scenario();
                if(scenario==null || scenario.targets().isEmpty() || scenario.stopLoss().signum()<=0 || scenario.stopLoss().compareTo(scenario.entryFrom())>=0
                        || scenario.entryFrom().compareTo(scenario.entryTo())>0 || scenario.targets().get(0).compareTo(scenario.entryTo())<=0) {
                    order.state="DATA_GAP";order.reason="INVALID_SCENARIO";blocked=true;break;
                }
                if(!tradable(candle)){skip(order,"UNCERTAIN_FILL");continue;}
                var entry=candle.open().multiply(ONE.add(slip),MC);
                if(candle.open().compareTo(scenario.entryFrom())<0 || entry.compareTo(scenario.entryTo())>0){skip(order,"ENTRY_GAP");continue;}
                var unitCost=entry.multiply(ONE.add(fee),MC);
                long quantity=budget.divide(unitCost,0,RoundingMode.DOWN).longValueExact();
                if(quantity==0){skip(order,"INSUFFICIENT_CASH");continue;}
                order.quantity=quantity;order.entry=entry;order.entered=day;order.entryIndex=dayIndex;order.cost=unitCost.multiply(BigDecimal.valueOf(quantity),MC);
                cash=cash.subtract(order.cost);buyBudget=buyBudget.subtract(order.cost);open.put(stock.code(),order);
            }
            if(blocked){curve.add(new Point(day,round(cash),null,null,open.size()));continue;}
            for(var position:new ArrayList<>(open.values())) {
                var candle=bar(known,position.cohort,position.stock.code(),day).candle();var scenario=position.stock.scenario();BigDecimal raw=null;String reason=null;boolean ambiguous=false;
                if(tradable(candle)) {
                    if(candle.low().compareTo(scenario.stopLoss())<=0){raw=scenario.stopLoss();reason="STOP";ambiguous=candle.high().compareTo(scenario.targets().get(0))>=0;}
                    else if(candle.high().compareTo(scenario.targets().get(0))>=0){raw=scenario.targets().get(0);reason="TARGET";}
                    else if(dayIndex-position.entryIndex+1>=p.holdingDays()){raw=candle.close();reason="TIME_EXIT";}
                }
                if(raw!=null){cash=cash.add(close(position,day,raw,reason,ambiguous,slip,fee));open.remove(position.stock.code());}
                else {position.marked=liquidation(candle.close(),position.quantity,slip,fee).subtract(position.cost);
                    if(dayIndex-position.entryIndex+1>=p.holdingDays())position.state="OPEN_UNLIQUIDATED";}
            }
            var equity=cash;
            for(var position:open.values())equity=equity.add(position.cost).add(position.marked);
            peak=peak.max(equity);var dd=ONE.subtract(equity.divide(peak,MC)).multiply(HUNDRED);worst=worst.max(dd);
            curve.add(new Point(day,round(cash),round(equity),round(dd),open.size()));priorEquity=equity;
        }
        if(blocked)for(var position:open.values()) {
            position.marked=null;position.state="DATA_GAP";
            if(position.reason==null)position.reason="ACCOUNT_PATH_INCOMPLETE";
        }
        var trades=all.stream().map(this::trade).toList();var grouped=new TreeMap<String,List<Trade>>();
        for(var trade:trades) {
            add(grouped,"STRATEGY/"+trade.strategy(),trade);add(grouped,"MARKET/"+trade.marketBias(),trade);
            var cohort=input.cohorts().stream().filter(c->c.report().runId().equals(trade.reportId())).findFirst().orElseThrow();
            add(grouped,"VERSION/"+cohort.report().version(),trade);
            add(grouped,"TIER/"+trade.evidenceGroups().getOrDefault("TIER","UNSCORED"),trade);
            trade.evidenceGroups().forEach((key,value)->add(grouped,"EVIDENCE/"+key+"/"+value,trade));
        }
        var groups=new TreeMap<String,Metrics>();grouped.forEach((key,value)->groups.put(key,metrics(value)));
        boolean unliquidated=open.values().stream().anyMatch(pos->pos.state.equals("OPEN_UNLIQUIDATED"));
        var finalEquity=dates.isEmpty()?null:blocked?null:priorEquity;
        return new Variant((comparison!=null?comparison:filter?"MARKET_FILTER":"NO_MARKET_FILTER")+(stress?"_COST_STRESS":""),filter,slip.movePointRight(4),
                blocked?"DATA_GAP":unliquidated?"OPEN_UNLIQUIDATED":"EVALUATED",round(finalEquity),finalEquity==null?null:round(pct(finalEquity,p.initialCash())),
                finalEquity==null?null:round(worst),curve,trades,metrics(trades),groups);
    }
    private static void add(Map<String,List<Trade>> groups,String key,Trade trade){groups.computeIfAbsent(key,k->new ArrayList<>()).add(trade);}
    private Observation bar(Map<String,Observation> known,Cohort c,String code,LocalDate date){return known.get(c.report().runId()+":STOCK:"+code+":"+date);}
    private boolean valid(Observation o){return o!=null&&o.quality().equals("VALID")&&o.candle()!=null;}
    private boolean tradable(DailyCandle c){return c.volume()>0&&c.high().compareTo(c.low())>0;}
    private void skip(Position p,String reason){p.state="SKIPPED";p.reason=reason;}
    private BigDecimal liquidation(BigDecimal price,long quantity,BigDecimal slip,BigDecimal fee){return price.multiply(ONE.subtract(slip),MC).multiply(ONE.subtract(fee),MC).multiply(BigDecimal.valueOf(quantity),MC);}
    private BigDecimal close(Position p,LocalDate day,BigDecimal raw,String reason,boolean ambiguous,BigDecimal slip,BigDecimal fee){
        var proceeds=liquidation(raw,p.quantity,slip,fee);p.exit=raw.multiply(ONE.subtract(slip),MC);p.exited=day;p.profit=proceeds.subtract(p.cost);p.marked=p.profit;p.state="CLOSED";p.reason=reason;p.ambiguous=ambiguous;return proceeds;
    }
    private Trade trade(Position p){
        var tags=new TreeMap<String,String>();for(var code:TAGS)tags.put(code,p.stock.evidence().stream().filter(e->e.code().equals(code)).findFirst().map(e->e.passed()==null?"UNKNOWN":e.passed()?"TRUE":"FALSE").orElse("UNKNOWN"));
        tags.put("SIGNAL_AGE",p.stock.evidence().stream().filter(e->e.code().equals("BUY_PATTERN")&&e.value()!=null).findFirst().map(e->e.value().stripTrailingZeros().toPlainString()).orElse("UNKNOWN"));
        if(p.stock.newEntry()!=null&&p.stock.newEntry().suitability()!=null)tags.put("TIER",p.stock.newEntry().suitability().tier());
        var bias=p.cohort.report().markets().stream().filter(m->m.market().equals(p.stock.market())).findFirst().map(m->m.bias()).orElse("UNKNOWN");
        return new Trade(p.cohort.report().runId(),p.stock.code(),p.stock.name(),p.stock.strategy(),p.stock.market(),bias,p.state,p.reason,p.entered,p.exited,p.quantity,
                round(p.entry),round(p.exit),round(p.cost),round(p.profit),p.profit==null?null:round(p.profit.divide(p.cost,MC).multiply(HUNDRED)),round(p.marked),p.ambiguous,tags);
    }
    private Metrics metrics(List<Trade> trades){
        var closed=trades.stream().filter(t->t.state().equals("CLOSED")).toList();var wins=closed.stream().filter(t->t.netProfit().signum()>0).toList();var losses=closed.stream().filter(t->t.netProfit().signum()<0).toList();
        int entered=(int)trades.stream().filter(t->t.entryDate()!=null).count(),unresolved=(int)trades.stream().filter(t->t.state().equals("DATA_GAP")).count();
        var positive=wins.stream().map(Trade::netProfit).reduce(ZERO,BigDecimal::add);var negative=losses.stream().map(Trade::netProfit).reduce(ZERO,BigDecimal::add);
        var exits=new TreeMap<LocalDate,BigDecimal>();closed.forEach(t->exits.merge(t.exitDate(),t.netProfit(),BigDecimal::add));int streak=0,max=0;
        for(var v:exits.values()){streak=v.signum()<0?streak+1:0;max=Math.max(max,streak);}
        return new Metrics(trades.size(),entered,closed.size(),wins.size(),losses.size(),closed.size()-wins.size()-losses.size(),
                (int)trades.stream().filter(t->t.state().startsWith("OPEN")).count(),unresolved,(int)trades.stream().filter(t->t.state().equals("SKIPPED")).count(),
                ratio(wins.size(),closed.size()),unresolved>0?null:ratio(entered,trades.size()),mean(closed),mean(wins),mean(losses),round(positive.add(negative)),negative.signum()==0?null:round(positive.divide(negative.abs(),MC)),max,
                ratio((int)closed.stream().filter(t->Set.of("STOP","GAP_STOP").contains(t.reason())).count(),closed.size()));
    }
    private BigDecimal mean(List<Trade> list){return list.isEmpty()?null:round(list.stream().map(Trade::netReturnPercent).reduce(ZERO,BigDecimal::add).divide(BigDecimal.valueOf(list.size()),MC));}
    private BigDecimal ratio(int n,int d){return d==0?null:round(BigDecimal.valueOf(n).multiply(HUNDRED).divide(BigDecimal.valueOf(d),MC));}
    private static BigDecimal pct(BigDecimal value,BigDecimal base){return value.divide(base,MC).subtract(ONE).multiply(HUNDRED);}
    private static BigDecimal round(BigDecimal value){return value==null?null:value.setScale(6,RoundingMode.HALF_UP);}
}
