package com.example.stockanalysis.decision;
import static com.example.stockanalysis.decision.DecisionModels.*;
import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;

class HorizonPlanCalculatorTest {
    @Test void bufferedUpperEntryIsUsedForUnheldRiskMetrics() {
        var tree=(ObjectNode)json.tree(data);((ObjectNode)tree.path("result").path("stock")).put("currentPrice",10000);
        var bars=data.priceHistory().stream().map(b->new DailyCandle(b.businessDate(),b.open().multiply(n(100)),b.high().multiply(n(100)),b.low().multiply(n(100)),b.close().multiply(n(100)),b.volume())).toList();
        tree.set("priceHistory",json.tree(bars));
        var scaled=json.convert(tree,UnifiedAnalysisResponse.class);
        var p=calculator.calculate(scaled,new ReviewRequest(Position.NOT_HELD,10));
        var upper=p.path("plan").path("entryTo").decimalValue();
        var stop=p.path("plan").path("stopLoss").decimalValue();
        var target=p.path("plan").path("targets").get(0).decimalValue();
        assertThat(upper).isGreaterThan(n(10000)).isLessThanOrEqualTo(n(10150));
        assertThat(p.path("metrics").path("RR_ONE").path("value").decimalValue()).isEqualByComparingTo(target.subtract(upper).divide(upper.subtract(stop),2,java.math.RoundingMode.HALF_UP));
    }
    DecisionWorkflowTest fixture;DecisionJson json;UnifiedAnalysisResponse data;HorizonPlanCalculator calculator;
    @BeforeEach void setup() {
        fixture=new DecisionWorkflowTest();fixture.setup();json=fixture.json;calculator=new HorizonPlanCalculator(json);
        var tree=(ObjectNode)json.tree(fixture.data);((ObjectNode)tree.path("result").path("stock")).put("currentPrice",100);
        var end=fixture.data.result().priceDate();var bars=new ArrayList<DailyCandle>();
        for(int i=0;i<300;i++) {int ago=299-i;bars.add(new DailyCandle(end.minusDays(ago),n(100),n(ago==5?104:ago==50?102:101),n(ago==8?96:99),n(100),10000));}
        tree.set("priceHistory",json.tree(bars));data=json.convert(tree,UnifiedAnalysisResponse.class);
    }
    BigDecimal n(int x){return BigDecimal.valueOf(x);}
    @Test void holdingQuantityChangesMoneyButNotPricePlan() {
        var small=calculator.calculate(data,new ReviewRequest(Position.HELD,10,n(90),null,null,n(10),null));
        var large=calculator.calculate(data,new ReviewRequest(Position.HELD,10,n(90),null,null,n(100),null));
        assertThat(small.path("plan")).isEqualTo(large.path("plan"));
        assertThat(large.path("metrics").path("PNL_AMOUNT").path("value").decimalValue()).isEqualByComparingTo("1000");
        assertThat(large.path("metrics").path("ADDITIONAL_RISK_AMOUNT").path("value").decimalValue())
            .isEqualByComparingTo(small.path("metrics").path("ADDITIONAL_RISK_AMOUNT").path("value").decimalValue().multiply(n(10)));
        assertThatThrownBy(()->DecisionService.validateContext(new ReviewRequest(Position.HELD,10,n(90),null,null,n(10),data.result().priceDate().plusDays(1)),data))
            .isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
    }
    @Test void periodSelectsObservedTargetsWithoutArtificiallyWideningSameSupport() {
        var shortPlan=calculator.calculate(data,new ReviewRequest(Position.NOT_HELD,5));
        var longPlan=calculator.calculate(data,new ReviewRequest(Position.NOT_HELD,60));
        assertThat(shortPlan.path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(shortPlan.path("plan").path("targets").get(0).decimalValue()).isEqualByComparingTo(n(104));
        assertThat(longPlan.path("plan").path("targets").get(0).decimalValue()).isEqualByComparingTo(n(102));
        // Same confirmed structure may yield identical prices; only the observation window/reference changes.
        assertThat(longPlan.path("plan").path("stopLoss")).isEqualTo(shortPlan.path("plan").path("stopLoss"));
        assertThat(longPlan.path("lookbackBars").asInt()).isGreaterThan(shortPlan.path("lookbackBars").asInt());
        assertThat(longPlan.path("metrics").path("VOLATILITY_SCREEN").path("value").asInt()).isGreaterThan(shortPlan.path("metrics").path("VOLATILITY_SCREEN").path("value").asInt());
        assertThat(calculator.calculate(data,new ReviewRequest(Position.NOT_HELD,5))).isEqualTo(shortPlan);
    }
    @Test void ignoresFutureBarsAndRejectsDuplicateOrMismatchedHistory() {
        var expected=calculator.calculate(data,new ReviewRequest(Position.NOT_HELD,10));
        var bars=new ArrayList<>(data.priceHistory());bars.add(new DailyCandle(data.result().priceDate().plusDays(1),n(900),n(1000),n(800),n(900),10000));
        assertThat(calculator.calculate(withBars(bars),new ReviewRequest(Position.NOT_HELD,10))).isEqualTo(expected);
        bars.add(data.priceHistory().get(0));assertThat(calculator.calculate(withBars(bars),new ReviewRequest(Position.NOT_HELD,10)).path("status").asText()).isEqualTo("HISTORY_INVALID");
    }
    @Test void oldAndShortHistoryRequireReanalysisWithoutReusingEngineTargets() {
        assertThat(calculator.calculate(withBars(List.of()),new ReviewRequest(Position.NOT_HELD,5)).path("status").asText()).isEqualTo("HISTORY_REQUIRED");
        var shortHistory=withBars(data.priceHistory().subList(260,300));
        assertThat(calculator.calculate(shortHistory,new ReviewRequest(Position.NOT_HELD,60)).path("status").asText()).isEqualTo("HISTORY_INSUFFICIENT");
    }
    @Test void noObservedOverheadResistanceDoesNotInventATarget() {
        var bars=data.priceHistory().stream().map(b->new DailyCandle(b.businessDate(),n(100),n(100),b.low(),n(100),b.volume())).toList();
        var p=calculator.calculate(withBars(bars),new ReviewRequest(Position.NOT_HELD,10));
        assertThat(p.path("status").asText()).isEqualTo("NO_RESISTANCE");assertThat(p.path("plan").isNull()).isTrue();
    }
    @Test void holdingUsesCurrentPriceRiskAndSeparatesCostBasisAndElapsedBars() {
        var p=calculator.calculate(data,new ReviewRequest(Position.HELD,10,n(80),data.result().priceDate().minusDays(7)));
        assertThat(p.path("metrics").path("PNL_PCT").path("value").decimalValue()).isEqualByComparingTo("25");
        assertThat(p.path("riskReference").asText()).isEqualTo("CURRENT_PRICE");
        assertThat(p.path("metrics").path("HELD_BARS").path("value").asInt()).isEqualTo(7);
        var unheld=calculator.calculate(data,new ReviewRequest(Position.NOT_HELD,10));
        assertThat(p.path("metrics").path("RR_ONE").path("value")).isEqualTo(unheld.path("metrics").path("RR_ONE").path("value"));
        assertThatThrownBy(()->DecisionService.validateContext(new ReviewRequest(Position.HELD,10,n(80),data.result().priceDate().plusDays(1)),data)).isInstanceOf(com.example.stockanalysis.shared.exception.BusinessException.class);
    }
    UnifiedAnalysisResponse withBars(List<DailyCandle> bars){return new UnifiedAnalysisResponse(data.result(),data.collectionQuality(),data.collectionWarnings(),data.volume(),data.tradingAmountMillionWon(),bars);}
}
