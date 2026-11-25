package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.stockanalysis.analysis.application.port.out.StockChartDataProvider;
import com.example.stockanalysis.unified.eod.EodProperties;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class SwingBacktestServiceTest {
    @Test
    void registersExactlyTwentyFiveExchangeSessionsAndReusesTheSameRunOnRetry() {
        var properties = new EodProperties("classpath:unified-calendar-2026.csv",
                Duration.ofHours(8), Duration.ofMinutes(10), Duration.ofSeconds(30),
                Duration.ofMinutes(90), Duration.ofHours(12), Duration.ofMinutes(1), 3, Duration.ofSeconds(90));
        var calendar = new TradingCalendar(properties, new DefaultResourceLoader());
        var store = mock(SwingBacktestStore.class);
        var saved = new AtomicReference<Run>();
        doAnswer(call -> { saved.set(call.getArgument(0)); return null; }).when(store).insert(any());
        var id = UUID.randomUUID();
        when(store.find(id)).thenAnswer(call -> saved.get());
        var clock = Clock.fixed(Instant.parse("2026-11-19T11:00:00Z"), ZoneOffset.UTC);
        var service = new SwingBacktestService(calendar, mock(StockChartDataProvider.class), store, clock, true);
        var request = new Request(List.of("005930"), LocalDate.parse("2026-11-19"), null,
                5, new BigDecimal("10000000"), 1, 15, 5);

        var result = service.start(id, request);
        assertThat(result.run().sessions()).hasSize(25).allSatisfy(date ->
                assertThat(calendar.day(date).status()).isEqualTo(TradingCalendar.Status.OPEN));
        assertThat(result.run().followUp()).hasSize(4);
        assertThat(result.run().warmup()).hasSize(21);
        assertThat(result.settledSessions()).isZero();
        assertThat(service.start(id, request)).isEqualTo(result);
        verify(store, times(1)).insert(any());
    }
}
