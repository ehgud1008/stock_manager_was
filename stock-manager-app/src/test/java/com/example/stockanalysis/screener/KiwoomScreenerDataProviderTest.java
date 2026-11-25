package com.example.stockanalysis.screener;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.contract.tr.ka10099.Ka10099Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10081.Ka10081Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10080.Ka10080Response;
import com.example.stockanalysis.kiwoom.tr.api.*;
import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import com.example.stockanalysis.screener.config.ScreenerProperties;
import com.example.stockanalysis.screener.domain.*;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest.Market;
import com.example.stockanalysis.screener.infrastructure.KiwoomScreenerDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KiwoomScreenerDataProviderTest {
    private final KiwoomTrExecutionService client = mock(KiwoomTrExecutionService.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T01:00:00Z"), ZoneOffset.UTC);
    private KiwoomScreenerDataProvider provider(int pages, int retries) {
        return new KiwoomScreenerDataProvider(client, mapper, new ScreenerProperties(0L, pages, retries, 2, null), clock,
                new com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog(new org.springframework.core.io.ClassPathResource("stocks/stock_master.txt")));
    }
    private ScreenerRun run(Mode mode) {
        var request = new ScreenerRunRequest(mode, null, null, null, List.of("005930"));
        return new ScreenerRun("r", "k", request, LocalDateTime.of(2026,9,13,10,0), ScreenerConfig.defaults(), DefaultScreenerEngine.VERSION, clock.instant(), null, ScreenerRun.Status.RUNNING,1,0,0,0,null);
    }
    private KiwoomTrExecutionResponse universe(String code, String key) throws Exception {
        var body = mapper.readValue("{\"return_code\":0,\"list\":[{\"code\":\"" + code + "\",\"name\":\"테스트\",\"regDay\":\"20000101\",\"state\":\"정상\"}]}", Ka10099Response.class);
        return new KiwoomTrExecutionResponse(new KiwoomTrResponseHeader("ka10099", key.isEmpty() ? "N" : "Y", key), body);
    }
    private KiwoomTrExecutionResponse daily(String date, String key, String close) throws Exception {
        var body = mapper.readValue("{\"return_code\":0,\"stk_dt_pole_chart_qry\":[{\"dt\":\"" + date + "\",\"open_pric\":\"100\",\"high_pric\":\"110\",\"low_pric\":\"90\",\"cur_prc\":\"" + close + "\",\"trde_qty\":\"1000\",\"trde_prica\":\"10\"}]}", Ka10081Response.class);
        return new KiwoomTrExecutionResponse(new KiwoomTrResponseHeader("ka10081", key.isEmpty() ? "N" : "Y", key), body);
    }
    @Test void universeUsesBothMarketsAndFollowsContinuationWithoutRankFiltering() throws Exception {
        when(client.executeWithServerAuthorization(eq("ka10099"), anyString(), anyString(), eq(Map.of("mrkt_tp", "0"))))
                .thenReturn(universe("000001", "next"), universe("000002", ""));
        when(client.executeWithServerAuthorization(eq("ka10099"), anyString(), anyString(), eq(Map.of("mrkt_tp", "10"))))
                .thenReturn(universe("000003", ""));
        assertThat(provider(5,0).universe(Market.ALL)).extracting(ScreenerItem.Stock::stockCode).containsExactly("000001", "000002", "000003");
        verify(client).executeWithServerAuthorization("ka10099", "Y", "next", Map.of("mrkt_tp", "0"));
    }
    @Test void repeatingContinuationAndUniverseLimitFailInsteadOfReturningPartialUniverse() throws Exception {
        when(client.executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap())).thenReturn(universe("000001", "next"));
        assertThatThrownBy(() -> provider(5,0).universe(Market.KOSPI)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider(1,0).universe(Market.KOSPI)).isInstanceOf(IllegalStateException.class);
    }
    @Test void chartPaginationExcludesUnfinishedDailyAndNormalizesSignedPrice() throws Exception {
        when(client.executeWithServerAuthorization(eq("ka10081"), anyString(), anyString(), anyMap()))
                .thenReturn(daily("20260913", "next", "100"), daily("20260912", "", "-100"));
        var result = provider(5,0).collect("005930", run(Mode.SWING));
        assertThat(result.sourceComplete()).isTrue();
        assertThat(result.input().bars()).hasSize(1);
        assertThat(result.input().bars().get(0).close()).isEqualByComparingTo("100");
        assertThat(result.input().bars().get(0).closedAt()).isEqualTo(LocalDateTime.of(2026,9,13,0,0));
    }
    @Test void pageLimitIsVisibleAndMalformedOhlcDoesNotDisappearSilently() throws Exception {
        when(client.executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap())).thenReturn(daily("20260912", "next", "100"));
        var result = provider(1,0).collect("005930", run(Mode.SWING));
        assertThat(result.sourceComplete()).isFalse();
        assertThat(result.warnings()).contains("SOURCE_WINDOW_TRUNCATED");
        when(client.executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap())).thenReturn(daily("20260912", "", "200"));
        assertThatThrownBy(() -> provider(5,0).collect("005930", run(Mode.SWING))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void minuteCutoffUsesIntervalAndOptionalDailyFailureDoesNotKillStageAnalysis() throws Exception {
        var body = mapper.readValue("{\"return_code\":0,\"stk_min_pole_chart_qry\":[{\"cntr_tm\":\"20260913095500\",\"open_pric\":\"100\",\"high_pric\":\"110\",\"low_pric\":\"90\",\"cur_prc\":\"100\",\"trde_qty\":\"1000\"},{\"cntr_tm\":\"20260913100000\"}]}", Ka10080Response.class);
        when(client.executeWithServerAuthorization(eq("ka10080"), anyString(), anyString(), anyMap())).thenReturn(new KiwoomTrExecutionResponse(new KiwoomTrResponseHeader("ka10080", "N", ""), body));
        when(client.executeWithServerAuthorization(eq("ka10081"), anyString(), anyString(), anyMap())).thenThrow(new KiwoomApiException("unauthorized", 401, "AUTH"));
        var result = provider(5,0).collect("005930", run(Mode.SHORT_TERM));
        assertThat(result.input().bars()).hasSize(1);
        assertThat(result.input().bars().get(0).tradingAmountMillionWon()).isNull();
        assertThat(result.warnings()).contains("DAILY_FEATURE_SOURCE_UNAVAILABLE");
    }
    @Test void onlyTransientExternalFailuresAreRetried() throws Exception {
        when(client.executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new KiwoomApiException("busy", 429, "RATE_LIMIT")).thenReturn(universe("000001", ""));
        assertThat(provider(5,1).universe(Market.KOSPI)).hasSize(1);
        verify(client, times(2)).executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap());
        reset(client);
        when(client.executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap())).thenThrow(new KiwoomApiException("no", 401, "AUTH"));
        assertThatThrownBy(() -> provider(5,2).universe(Market.KOSPI)).isInstanceOf(KiwoomApiException.class);
        verify(client, times(1)).executeWithServerAuthorization(anyString(), anyString(), anyString(), anyMap());
    }
    @Test void explicitOrResumedNonStStockNeverCallsChartApi() {
        assertThatThrownBy(() -> provider(5,0).collect("069500", run(Mode.SWING)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("STOCK_TYPE_NOT_ALLOWED");
        assertThatThrownBy(() -> provider(5,0).collect("ZZZZZZ", run(Mode.SHORT_TERM)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }
}
