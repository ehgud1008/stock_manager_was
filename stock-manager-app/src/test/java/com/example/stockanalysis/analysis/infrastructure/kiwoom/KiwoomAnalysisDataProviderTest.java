package com.example.stockanalysis.analysis.infrastructure.kiwoom;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCallResult;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataClient;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataRecordMapper;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataTrDefinition;
import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.kiwoom.client.KiwoomContinuation;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KiwoomAnalysisDataProviderTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"80, 25, false, COMPLETE", "59, 25, false, PARTIAL", "80, 19, false, PARTIAL", "80, 25, true, PARTIAL"})
    void monthlyShortageIsAdvisoryButCoreAndOtherCollectionFailuresStillBlock(int daily, int weekly, boolean missingFlow, String quality) {
        LocalDate date = LocalDate.of(2026, 8, 28);
        Instant now = Instant.parse("2026-08-29T07:00:00Z");
        var client = mock(KiwoomBackdataClient.class);
        var mapper = mock(KiwoomBackdataRecordMapper.class);
        var response = mock(KiwoomResult.class);
        when(client.execute(any(), any(), anyString(), anyString()))
                .thenReturn(new KiwoomBackdataCallResult(response, KiwoomContinuation.none()));
        if (missingFlow) when(client.execute(eq(new KiwoomBackdataCatalog().getRequired("ka10059")), any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("flow unavailable"));
        when(mapper.map(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            KiwoomBackdataTrDefinition definition = invocation.getArgument(0);
            return switch (definition.apiId()) {
                case "ka10001" -> List.of(basicRecord(date, now));
                case "ka10081" -> java.util.stream.IntStream.range(0, daily).mapToObj(i -> candleRecord("ka10081", BackdataDatasetType.DAILY_CANDLE, date.minusDays(i), now)).toList();
                case "ka10082" -> java.util.stream.IntStream.range(0, weekly).mapToObj(i -> candleRecord("ka10082", BackdataDatasetType.WEEKLY_CANDLE, date.minusWeeks(i), now)).toList();
                case "ka10083" -> List.of(candleRecord("ka10083", BackdataDatasetType.MONTHLY_CANDLE, date, now));
                case "ka10059" -> missingFlow ? List.of() : List.of(basicRecord(date, now));
                default -> List.of(basicRecord(date, now));
            };
        });
        var provider = new KiwoomAnalysisDataProvider(new KiwoomBackdataCatalog(), client, mapper, Clock.fixed(now, ZoneOffset.UTC));
        var result = provider.collect("005930", AnalysisMode.SWING, date);
        assertThat(result.warnings()).contains(com.example.stockanalysis.analysis.domain.DataQualityStatus.MONTHLY_REFERENCE_SHORTAGE);
        assertThat(result.dataQualityStatus().name()).isEqualTo(quality);
        var completed = provider.completeDaily(provider.collectDaily("005930", date));
        assertThat(completed.dataQualityStatus()).isEqualTo(result.dataQualityStatus());
        assertThat(completed.warnings()).contains(com.example.stockanalysis.analysis.domain.DataQualityStatus.MONTHLY_REFERENCE_SHORTAGE);
    }

    @Test
    void alignsQuantityFlowsToCandleDatesAndDoesNotInventMissingValues() {
        LocalDate date = LocalDate.of(2026, 9, 19);
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        var provider = new KiwoomAnalysisDataProvider(new KiwoomBackdataCatalog(),
                mock(KiwoomBackdataClient.class), mock(KiwoomBackdataRecordMapper.class), Clock.fixed(now, ZoneOffset.UTC));
        var candles = new java.util.ArrayList<com.example.stockanalysis.engine.model.DailyCandle>();
        var rows = new java.util.ArrayList<BackdataRecord>();
        for (int i = 0; i < 20; i++) {
            candles.add(new com.example.stockanalysis.engine.model.DailyCandle(date.minusDays(i),
                    java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, 100));
            rows.add(new BackdataRecord("ka10059", BackdataDatasetType.INVESTOR_TRADING, "005930", null, "",
                    date.minusDays(i), now, Map.of("foreigner", "2", "institution", "3")));
        }
        rows.add(new BackdataRecord("ka10059", BackdataDatasetType.INVESTOR_TRADING, "005930", null, "",
                date.plusDays(1), now, Map.of("foreigner", "999999", "institution", "999999")));
        var response = mock(KiwoomResult.class);
        var values = new java.util.LinkedHashMap<String, java.math.BigDecimal>();
        provider.addSwingFlow(values, List.of(new com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload(
                "ka10059", "test", "INVESTOR_FLOW", Map.of(), response, KiwoomContinuation.none(), rows, now, now)), candles);
        assertThat(values.get("swingNetBuy20")).isEqualByComparingTo("100");
        assertThat(values.get("swingFlowVolume20")).isEqualByComparingTo("2000");
        assertThat(values.get("swingPositiveFlowDays20")).isEqualByComparingTo("20");
        rows.remove(0);
        values.clear();
        provider.addSwingFlow(values, List.of(new com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload(
                "ka10059", "test", "INVESTOR_FLOW", Map.of(), response, KiwoomContinuation.none(), rows, now, now)), candles);
        assertThat(values).doesNotContainKey("swingNetBuy20");
    }

    @Test
    void collectsDailyWeeklyAndMonthlyCandlesForSwingAnalysis() {
        LocalDate baseDate = LocalDate.of(2026, 8, 28);
        Instant now = Instant.parse("2026-08-28T07:00:00Z");
        KiwoomBackdataCatalog catalog = new KiwoomBackdataCatalog();
        KiwoomBackdataClient client = mock(KiwoomBackdataClient.class);
        KiwoomBackdataRecordMapper mapper = mock(KiwoomBackdataRecordMapper.class);
        KiwoomResult response = mock(KiwoomResult.class);
        when(client.execute(any(), any(), anyString(), anyString()))
                .thenReturn(new KiwoomBackdataCallResult(response, KiwoomContinuation.none()));
        when(mapper.map(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            KiwoomBackdataTrDefinition definition = invocation.getArgument(0);
            return switch (definition.apiId()) {
                case "ka10001" -> List.of(basicRecord(baseDate, now));
                case "ka10081" -> List.of(candleRecord(
                        "ka10081", BackdataDatasetType.DAILY_CANDLE, baseDate, now
                ));
                case "ka10082" -> List.of(candleRecord(
                        "ka10082", BackdataDatasetType.WEEKLY_CANDLE, baseDate, now
                ));
                case "ka10083" -> List.of(candleRecord(
                        "ka10083", BackdataDatasetType.MONTHLY_CANDLE, baseDate, now
                ));
                default -> List.of();
            };
        });
        var provider = new KiwoomAnalysisDataProvider(
                catalog,
                client,
                mapper,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        var result = provider.collect("005930", AnalysisMode.SWING, baseDate);

        assertThat(result.input().dailyCandles()).hasSize(1);
        assertThat(result.input().weeklyCandles()).hasSize(1);
        assertThat(result.input().monthlyCandles()).hasSize(1);
        assertThat(result.sources()).extracting(source -> source.sourceRole())
                .contains("DAILY_CANDLE", "WEEKLY_CANDLE", "MONTHLY_CANDLE");
        verify(client).execute(
                eq(catalog.getRequired("ka10082")),
                argThat(request -> "20260828".equals(request.get("base_dt"))),
                eq("N"),
                eq("")
        );
        verify(client).execute(
                eq(catalog.getRequired("ka10083")),
                argThat(request -> "20260828".equals(request.get("base_dt"))),
                eq("N"),
                eq("")
        );

        // EOD: two daily-only probes, then all five supplementary sources exactly once.
        clearInvocations(client);
        provider.collectDaily("005930", baseDate);
        verify(client, times(1)).execute(any(), any(), anyString(), anyString());
        var verified = provider.collectDaily("005930", baseDate);
        var completed = provider.completeDaily(verified);
        verify(client, times(7)).execute(any(), any(), anyString(), anyString());
        verify(client, times(2)).execute(eq(catalog.getRequired("ka10081")), any(), anyString(), anyString());
        assertThat(completed.input()).isEqualTo(result.input());
        assertThat(completed.warnings()).isEqualTo(result.warnings());
        assertThat(completed.sources()).filteredOn(s -> "ka10081".equals(s.apiId())).singleElement()
                .isSameAs(verified.sources().get(0));
        assertThat(completed.sources()).extracting(s -> s.apiId())
                .containsExactly("ka10001", "ka10081", "ka10082", "ka10083", "ka10086", "ka10059");
    }

    private BackdataRecord basicRecord(LocalDate date, Instant observedAt) {
        return new BackdataRecord(
                "ka10001",
                BackdataDatasetType.STOCK_FUNDAMENTAL_SNAPSHOT,
                "005930",
                null,
                "",
                date,
                observedAt,
                Map.of(
                        "name", "삼성전자",
                        "currentPrice", "71000",
                        "roe", "9.2",
                        "per", "15.3",
                        "pbr", "1.4"
                )
        );
    }

    private BackdataRecord candleRecord(
            String apiId,
            BackdataDatasetType datasetType,
            LocalDate date,
            Instant observedAt
    ) {
        return new BackdataRecord(
                apiId,
                datasetType,
                "005930",
                null,
                "upd_stkpc_tp=1",
                date,
                observedAt,
                Map.of(
                        "open", "70000",
                        "high", "72000",
                        "low", "69500",
                        "close", "71000",
                        "volume", "10000000"
                )
        );
    }
}
