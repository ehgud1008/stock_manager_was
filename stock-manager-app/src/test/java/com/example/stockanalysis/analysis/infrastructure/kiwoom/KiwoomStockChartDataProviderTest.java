package com.example.stockanalysis.analysis.infrastructure.kiwoom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.stockanalysis.analysis.domain.StockChartPoint;
import com.example.stockanalysis.analysis.domain.StockPricePeriod;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataRecordMapper;
import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionResponse;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrResponseHeader;
import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class KiwoomStockChartDataProviderTest {

    @Test
    void callsMinuteChartTrAndMapsExecutionTimestamp() {
        KiwoomTrExecutionService trExecutionService = mock(KiwoomTrExecutionService.class);
        KiwoomBackdataRecordMapper mapper = mock(KiwoomBackdataRecordMapper.class);
        KiwoomResult response = mock(KiwoomResult.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-22T06:30:00Z"), ZoneOffset.UTC);
        var provider = new KiwoomStockChartDataProvider(
                new KiwoomBackdataCatalog(), trExecutionService, mapper, clock
        );
        when(trExecutionService.executeWithServerAuthorization(
                anyString(), anyString(), anyString(), any()
        )).thenReturn(new KiwoomTrExecutionResponse(
                new KiwoomTrResponseHeader("ka10080", "N", ""),
                response
        ));
        when(mapper.map(any(), any(), any(), any(), any())).thenReturn(List.of(
                record(Map.of(
                        "timestamp", "20260822152500",
                        "open", "71000",
                        "high", "71600",
                        "low", "70800",
                        "close", "71500",
                        "volume", "320000"
                ))
        ));

        List<StockChartPoint> result = provider.fetch(
                "005930", StockPricePeriod.MINUTE, 5, LocalDate.of(2026, 8, 22)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trExecutionService).executeWithServerAuthorization(
                eq("ka10080"),
                eq("N"),
                eq(""),
                requestCaptor.capture()
        );
        assertThat(requestCaptor.getValue()).containsEntry("tic_scope", "5");
        assertThat(requestCaptor.getValue()).containsEntry("base_dt", "20260822");
        assertThat(result).singleElement().satisfies(point -> {
            assertThat(point.timestamp().toString()).isEqualTo("2026-08-22T15:25+09:00");
            assertThat(point.close()).isEqualByComparingTo("71500");
            assertThat(point.volume()).isEqualTo(320_000L);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "WEEK, ka10082",
            "MONTH, ka10083"
    })
    void callsWeekAndMonthChartTrThroughUnifiedExecutionService(
            StockPricePeriod period,
            String apiId
    ) {
        KiwoomTrExecutionService trExecutionService = mock(KiwoomTrExecutionService.class);
        KiwoomBackdataRecordMapper mapper = mock(KiwoomBackdataRecordMapper.class);
        KiwoomResult response = mock(KiwoomResult.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-30T06:30:00Z"), ZoneOffset.UTC);
        var provider = new KiwoomStockChartDataProvider(
                new KiwoomBackdataCatalog(), trExecutionService, mapper, clock
        );
        when(trExecutionService.executeWithServerAuthorization(
                anyString(), anyString(), anyString(), any()
        )).thenReturn(new KiwoomTrExecutionResponse(
                new KiwoomTrResponseHeader(apiId, "N", ""),
                response
        ));
        when(mapper.map(any(), any(), any(), any(), any())).thenReturn(List.of(
                record(Map.of(
                        "open", "71000",
                        "high", "71600",
                        "low", "70800",
                        "close", "71500",
                        "volume", "320000"
                ))
        ));

        List<StockChartPoint> result = provider.fetch(
                "432720", period, 5, LocalDate.of(2026, 4, 30)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trExecutionService).executeWithServerAuthorization(
                eq(apiId),
                eq("N"),
                eq(""),
                requestCaptor.capture()
        );
        assertThat(requestCaptor.getValue())
                .containsEntry("stk_cd", "432720")
                .containsEntry("base_dt", "20260430")
                .containsEntry("upd_stkpc_tp", "1")
                .doesNotContainKey("tic_scope");
        assertThat(result).hasSize(1);
    }

    private BackdataRecord record(Map<String, String> values) {
        return new BackdataRecord(
                "ka10080",
                BackdataDatasetType.MINUTE_CANDLE,
                "005930",
                null,
                "tic_scope=5|upd_stkpc_tp=1",
                LocalDate.of(2026, 8, 22),
                Instant.parse("2026-08-22T06:30:00Z"),
                values
        );
    }
}
