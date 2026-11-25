package com.example.stockanalysis.analysis.application;

import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.port.out.*;
import com.example.stockanalysis.analysis.config.StockAnalysisEngineConfig;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.analysis.infrastructure.memory.InMemoryAnalysisStore;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockAnalysisSignalServiceTest {
    @Test void monthlyReferenceWarningDoesNotDeferJudgmentOrDowngradeSavedQuality() {
        var engine = mock(com.example.stockanalysis.engine.api.StockAnalysisEngine.class);
        var testService = new StockAnalysisService(provider, new InMemoryAnalysisStore(), mock(StockChartDataProvider.class), engine, clock);
        var warning = DataQualityStatus.MONTHLY_REFERENCE_SHORTAGE;
        var bar = new DailyCandle(DATE, BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(90), BigDecimal.valueOf(100), 1000);
        var input = new AnalysisInput(new StockSnapshot("005930", "test", "KOSPI", BigDecimal.valueOf(100)),
                AnalysisMode.SWING, DATE, clock.instant(), List.of(bar), Map.of());
        when(provider.collect("005930", AnalysisMode.SWING, DATE))
                .thenReturn(new CollectedAnalysisData(input, List.of(), List.of(warning), DataQualityStatus.COMPLETE));
        var analysis = new AnalysisResult("analysis-v6-swing", AnalysisMode.SWING, BigDecimal.valueOf(85),
                List.of(new FactorResult("TREND", BigDecimal.valueOf(70), BigDecimal.valueOf(70), new BigDecimal("0.18"), "추세")),
                new StrategyDecision("SWING", "매수", StrategyAction.BUY_CANDIDATE, List.of()),
                new TradeScenario(BigDecimal.valueOf(95), BigDecimal.valueOf(105), List.of(BigDecimal.valueOf(140)), BigDecimal.valueOf(90)), List.of(),
                new SignalReport("test", clock.instant(), "NOT_HELD", "READY", List.of(), List.of()));
        when(engine.analyze(any())).thenReturn(analysis);
        var view = testService.execute("005930", new AnalysisExecutionCommand(AnalysisMode.SWING, DATE, true, null));
        assertThat(view.dataQualityStatus()).isEqualTo(DataQualityStatus.COMPLETE);
        assertThat(view.warnings()).contains(warning);
        assertThat(view.tradeJudgment().notHeld().action()).isEqualTo(com.example.stockanalysis.judgment.TradeJudgment.Action.WATCH);
        assertThat(view.tradeJudgment().held().action()).isEqualTo(com.example.stockanalysis.judgment.TradeJudgment.Action.HOLD);
    }
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-19T02:00:00Z"), ZoneOffset.UTC);
    private final AnalysisDataProvider provider = mock(AnalysisDataProvider.class);
    private final StockAnalysisService service = new StockAnalysisService(provider, new InMemoryAnalysisStore(),
            mock(StockChartDataProvider.class), new StockAnalysisEngineConfig().stockAnalysisEngine(), clock);
    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);
    private PositionContext held(Instant opened) {
        return new PositionContext(true, opened, BigDecimal.valueOf(120), BigDecimal.valueOf(115),
                List.of(BigDecimal.valueOf(150)), PositionContext.StopPolicy.CLOSE);
    }
    @Test void recalculatesSameDayAndPreservesPositionScopeThroughMemoryStore() {
        List<DailyCandle> bars = new ArrayList<>();
        for (int i=0; i<80; i++) {
            var close = BigDecimal.valueOf(100 + i);
            bars.add(new DailyCandle(DATE.minusDays(79-i), close, close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close, 1000));
        }
        var input = new AnalysisInput(new StockSnapshot("005930", "test", "KOSPI", BigDecimal.valueOf(179)),
                AnalysisMode.SWING, DATE, clock.instant(), bars, bars, bars, Map.of());
        when(provider.collect("005930", AnalysisMode.SWING, DATE))
                .thenReturn(new CollectedAnalysisData(input, List.of(), List.of(), DataQualityStatus.COMPLETE));
        var unknown = service.execute("005930", new AnalysisExecutionCommand(AnalysisMode.SWING, DATE, false, null));
        var owned = service.execute("005930", new AnalysisExecutionCommand(AnalysisMode.SWING, DATE, false, null,
                held(Instant.parse("2026-09-01T01:00:00Z"))));
        assertThat(unknown.signalReport().positionScope()).isEqualTo("UNKNOWN");
        assertThat(owned.signalReport().positionScope()).isEqualTo("HELD");
        assertThat(unknown.tradeJudgment().positionScope()).isEqualTo("UNKNOWN");
        assertThat(owned.tradeJudgment().positionScope()).isEqualTo("HELD");
        assertThat(service.getRun(owned.analysisRunId()).tradeJudgment()).isEqualTo(owned.tradeJudgment());
        assertThat(com.example.stockanalysis.analysis.api.StockAnalysisResponse.from(owned).tradeJudgment()).isEqualTo(owned.tradeJudgment());
        assertThat(owned.analysisRunId()).isNotEqualTo(unknown.analysisRunId());
        verify(provider, times(2)).collect("005930", AnalysisMode.SWING, DATE);
        assertThat(service.getLatest("005930", AnalysisMode.SWING).signalReport()).isEqualTo(owned.signalReport());
    }
    @Test void resolvesBalanceFirstAndReturnsFrozenCalculationWithoutRereadingIt() {
        var holdings=mock(com.example.stockanalysis.holdings.HoldingService.class);service.setHoldings(holdings);
        var h=new com.example.stockanalysis.holdings.HoldingModels.Holding("005930","test",BigDecimal.TEN,BigDecimal.valueOf(100),null,DATE,null,null,1);
        when(holdings.single("005930")).thenReturn(new com.example.stockanalysis.holdings.HoldingModels.Snapshot(clock.instant(),"AVAILABLE",Map.of("005930",h)));
        List<DailyCandle> bars=new ArrayList<>();
        for(int i=0;i<80;i++){var close=BigDecimal.valueOf(100+i);bars.add(new DailyCandle(DATE.minusDays(79-i),close,close.add(BigDecimal.ONE),close.subtract(BigDecimal.ONE),close,1000));}
        var input=new AnalysisInput(new StockSnapshot("005930","test","KOSPI",BigDecimal.valueOf(999)),AnalysisMode.SWING,DATE,clock.instant(),bars,bars,bars,Map.of());
        when(provider.collect("005930",AnalysisMode.SWING,DATE)).thenReturn(new CollectedAnalysisData(input,List.of(),List.of(),DataQualityStatus.COMPLETE));
        var view=service.execute("005930",new AnalysisExecutionCommand(AnalysisMode.SWING,DATE,false,null));
        var order=inOrder(holdings,provider);order.verify(holdings).single("005930");order.verify(provider).collect("005930",AnalysisMode.SWING,DATE);
        assertThat(view.holdingAnalysis().referencePrice()).isEqualByComparingTo("179");
        assertThat(view.holdingAnalysis().pnlAmount()).isEqualByComparingTo("790");
        verify(holdings,times(1)).single("005930");verify(holdings).save("STOCK:"+view.analysisRunId(),view.holdingAnalysis());
        verify(holdings,never()).evaluation(anyString());
    }
    @Test void unregisteredStockUsesNotHeldSignalScopeAndEntryDecision() {
        var holdings=mock(com.example.stockanalysis.holdings.HoldingService.class);service.setHoldings(holdings);
        when(holdings.single("005930")).thenReturn(new com.example.stockanalysis.holdings.HoldingModels.Snapshot(clock.instant(),"AVAILABLE",Map.of()));
        List<DailyCandle> bars=new ArrayList<>();
        for(int i=0;i<80;i++){var close=BigDecimal.valueOf(100+i);bars.add(new DailyCandle(DATE.minusDays(79-i),close,close.add(BigDecimal.ONE),close.subtract(BigDecimal.ONE),close,1000));}
        var input=new AnalysisInput(new StockSnapshot("005930","test","KOSPI",BigDecimal.valueOf(179)),AnalysisMode.SWING,DATE,clock.instant(),bars,bars,bars,Map.of());
        when(provider.collect("005930",AnalysisMode.SWING,DATE)).thenReturn(new CollectedAnalysisData(input,List.of(),List.of(),DataQualityStatus.COMPLETE));
        var view=service.execute("005930",new AnalysisExecutionCommand(AnalysisMode.SWING,DATE,false,null));
        assertThat(view.signalReport().positionScope()).isEqualTo("NOT_HELD");
        assertThat(view.tradeJudgment().positionScope()).isEqualTo("NOT_HELD");
        assertThat(view.holdingAnalysis().positionStatus()).isEqualTo("NOT_HELD");
        assertThat(view.holdingAnalysis().action()).isEqualTo(view.tradeJudgment().notHeld().action().name());
    }
    @Test void rejectsPositionOpeningAfterAnalysisDateBeforeCollecting() {
        assertThatThrownBy(() -> service.execute("005930", new AnalysisExecutionCommand(AnalysisMode.SWING, DATE,
                false, null, held(Instant.parse("2026-09-19T01:00:00Z"))))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(provider);
    }
    @Test void rejectsPositionContextForShortTermMode() {
        assertThatThrownBy(() -> service.execute("005930", new AnalysisExecutionCommand(AnalysisMode.SHORT_TERM, DATE,
                false, null, held(Instant.parse("2026-09-01T01:00:00Z"))))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(provider);
    }
}
