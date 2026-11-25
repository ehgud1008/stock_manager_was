package com.example.stockanalysis.unified;

import com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload;
import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnifiedAnalysisServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-18T15:30:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);
    private final AnalysisDataProvider provider = mock(AnalysisDataProvider.class);
    private final ScreenerStockTypeCatalog types = mock(ScreenerStockTypeCatalog.class);
    private final UnifiedAnalysisService service = new UnifiedAnalysisService(provider,
            new DefaultUnifiedStockAnalysisEngine(), types, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void readsHoldingBeforeMarketCollectionAndPreservesPublicScore() {
        var holdings=mock(com.example.stockanalysis.holdings.HoldingService.class);
        var h=new com.example.stockanalysis.holdings.HoldingModels.Holding("005930","삼성전자",BigDecimal.TEN,new BigDecimal("100"),null,DATE,null,null,1);
        var snapshot=new com.example.stockanalysis.holdings.HoldingModels.Snapshot(NOW,"AVAILABLE",Map.of("005930",h));
        when(holdings.single("005930")).thenReturn(snapshot);service.setHoldings(holdings);
        when(types.typeOf("005930")).thenReturn("ST");
        when(provider.collect("005930",AnalysisMode.SWING,DATE)).thenReturn(collected("005930"));
        var result=service.execute("005930");
        var order=inOrder(holdings,provider);order.verify(holdings).single("005930");order.verify(provider).collect("005930",AnalysisMode.SWING,DATE);
        verify(holdings,times(1)).single("005930");
        assertThat(result.holdingAnalysis().positionStatus()).isEqualTo("HELD");
        assertThat(result.holdingAnalysis().pnlAmount()).isEqualByComparingTo("50");
        assertThat(service.executeAt("005930",result.result().asOf()).result()).isEqualTo(result.result());
        verifyNoMoreInteractions(holdings);
    }
    private CollectedAnalysisData collected(String code) {
        var input = new AnalysisInput(new StockSnapshot(code, "삼성전자", "KOSPI", new BigDecimal("999999")),
                AnalysisMode.SWING, DATE, NOW, List.of(new DailyCandle(DATE, new BigDecimal("100"),
                new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"), 1000)), Map.of());
        return new CollectedAnalysisData(input, List.of(), List.of("수급 일부 누락"), DataQualityStatus.PARTIAL);
    }

    @Test void usesKoreanPreviousDateAndUnifiedEngineWithoutLivePriceOrLegacyStorage() {
        when(types.typeOf("005930")).thenReturn("ST");
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(collected("005930"));
        var response = service.execute("005930");
        assertThat(response.result().engineVersion()).isEqualTo("unified-swing-v1");
        assertThat(response.result().stock().currentPrice()).isEqualByComparingTo("105");
        assertThat(response.result().priceDate()).isEqualTo(DATE);
        assertThat(response.priceHistory()).hasSize(1);
        assertThat(response.priceHistory().get(0).close()).isEqualByComparingTo(response.result().stock().currentPrice());
        assertThat(response.result().asOf()).isEqualTo(LocalDateTime.of(2026, 9, 19, 0, 30));
        assertThat(response.result().analysis().signalReport()).isNotNull();
        assertThat(response.tradeJudgment()).isNotNull();
        assertThat(response.tradeJudgment().priceDate()).isEqualTo(response.result().priceDate());
        assertThat(response.tradeJudgment().notHeld().action()).isEqualTo(com.example.stockanalysis.judgment.TradeJudgment.Action.AVOID);
        assertThat(response.tradeJudgment().held().action()).isEqualTo(com.example.stockanalysis.judgment.TradeJudgment.Action.DEFER);
        assertThat(response.collectionQuality()).isEqualTo(DataQualityStatus.PARTIAL);
        assertThat(response.collectionWarnings()).contains("수급 일부 누락");
        assertThat(response.volume()).isEqualTo(1000L);
        assertThat(response.tradingAmountMillionWon()).isNull();
        verify(provider, times(1)).collect("005930", AnalysisMode.SWING, DATE);
    }

    private BackdataRecord amountRow(String code, LocalDate date, String amount) {
        return new BackdataRecord("ka10081", BackdataDatasetType.DAILY_CANDLE, code, null, null, date, NOW,
                Map.of("tradingAmount", amount));
    }
    @Test void savesOnlyLatestBoundedCompletedHistory() {
        when(types.typeOf("005930")).thenReturn("ST");
        var bars=new java.util.ArrayList<DailyCandle>();
        for(int i=0;i<400;i++) bars.add(new DailyCandle(DATE.minusDays(399-i),BigDecimal.valueOf(100),BigDecimal.valueOf(110),BigDecimal.valueOf(90),BigDecimal.valueOf(105),1000));
        var input=new AnalysisInput(new StockSnapshot("005930","삼성전자","KOSPI",BigDecimal.valueOf(999999)),AnalysisMode.SWING,DATE,NOW,bars,Map.of());
        when(provider.collect("005930",AnalysisMode.SWING,DATE)).thenReturn(new CollectedAnalysisData(input,List.of(),List.of(),DataQualityStatus.COMPLETE));
        var response=service.execute("005930");assertThat(response.priceHistory()).hasSize(300);
        assertThat(response.priceHistory().get(0).businessDate()).isEqualTo(DATE.minusDays(299));
        assertThat(response.priceHistory().get(299).businessDate()).isEqualTo(response.result().priceDate());
    }

    @Test void capturesActualAmountForPriceDateOnlyWithoutEstimatingFromClose() {
        when(types.typeOf("005930")).thenReturn("ST");
        var data = collected("005930");
        var source = new AnalysisSourcePayload("ka10081", "test", "DAILY_CANDLE", Map.of(), null, null,
                List.of(amountRow("005930", DATE.plusDays(1), "99999"), amountRow("000660", DATE, "99999"),
                        amountRow("005930", DATE, "12,345.5")), NOW, NOW);
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(new CollectedAnalysisData(
                data.input(), List.of(source), List.of(), DataQualityStatus.COMPLETE));
        var response = service.execute("005930");
        assertThat(response.volume()).isEqualTo(1000L);
        assertThat(response.tradingAmountMillionWon()).isEqualByComparingTo("12345.5");
    }

    @Test void treatsInvalidOrConflictingAmountsAsUnknownAndPreservesRealZero() {
        when(types.typeOf("005930")).thenReturn("ST");
        var data = collected("005930");
        for (String value : List.of("", "broken", "-5", "1.12345", "0")) {
            var source = new AnalysisSourcePayload("ka10081", "test", "DAILY_CANDLE", Map.of(), null, null,
                    List.of(amountRow("005930", DATE, value)), NOW, NOW);
            when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(new CollectedAnalysisData(
                    data.input(), List.of(source), List.of(), DataQualityStatus.COMPLETE));
            var result = service.execute("005930");
            if (value.equals("0")) assertThat(result.tradingAmountMillionWon()).isEqualByComparingTo("0");
            else assertThat(result.tradingAmountMillionWon()).isNull();
        }
        var source = new AnalysisSourcePayload("ka10081", "test", "DAILY_CANDLE", Map.of(), null, null,
                List.of(amountRow("005930", DATE, "100"), amountRow("005930", DATE, "200")), NOW, NOW);
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(new CollectedAnalysisData(
                data.input(), List.of(source), List.of(), DataQualityStatus.COMPLETE));
        assertThat(service.execute("005930").tradingAmountMillionWon()).isNull();
    }

    @Test void rejectsUnsupportedStocksBeforeExternalCollection() {
        when(types.typeOf("069500")).thenReturn("ETF");
        assertThatThrownBy(() -> service.execute("069500")).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(provider);
    }

    @Test void rejectsResponseForADifferentStock() {
        when(types.typeOf("005930")).thenReturn("ST");
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(collected("000660"));
        assertThatThrownBy(() -> service.execute("005930")).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
    }

    @Test void preservesCollectionFailureInsteadOfReturningDemoData() {
        when(types.typeOf("005930")).thenReturn("ST");
        var failure = new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "수집 실패");
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenThrow(failure);
        assertThatThrownBy(() -> service.execute("005930")).isSameAs(failure);
    }

    @Test void keepsAdditionalHistoryAsCoverageWarning() {
        when(types.typeOf("005930")).thenReturn("ST");
        var data = collected("005930");
        var source = new AnalysisSourcePayload("ka10081", "test", "DAILY_CANDLE", Map.of(), null,
                new KiwoomContinuation(true, "next"), List.of(), NOW, NOW);
        when(provider.collect("005930", AnalysisMode.SWING, DATE)).thenReturn(new CollectedAnalysisData(
                data.input(), List.of(source), List.of(), DataQualityStatus.COMPLETE));
        var response = service.execute("005930");
        assertThat(response.collectionQuality()).isEqualTo(DataQualityStatus.COMPLETE);
        assertThat(response.collectionWarnings()).anyMatch(w -> w.contains("추가 이력"));
    }
}
