package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest;
import com.example.stockanalysis.screener.infrastructure.KiwoomScreenerDataProvider;
import com.example.stockanalysis.screener.infrastructure.KiwoomScreenerDataProvider.HistoricalBar;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarketStudyIntegrationTest {
    @TempDir Path dir;
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    final Clock clock = Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC);
    final LocalDate first = LocalDate.of(2024,1,1);

    // The Kiwoom fixture exercises collection, persistence, replay and the HTTP API.
    @Test void usesCurrentlyListedStocksAndPersistsMultiYearResultsAcrossRestart() throws Exception {
        LocalDate from = LocalDate.of(2024,2,5), to = LocalDate.of(2026,3,2);
        var calendar = mock(TradingCalendar.class);
        var provider = mock(KiwoomScreenerDataProvider.class);
        var kospi = new Stock("000001", "시장1", "KOSPI", "", first, "ST");
        var kosdaq = new Stock("000002", "시장2", "KOSDAQ", "", first, "ST");
        var fund = new Stock("000003", "ETF", "KOSDAQ", "", first, "ETF");
        when(provider.universe(ScreenerRunRequest.Market.ALL)).thenReturn(List.of(kospi, kosdaq, fund));
        var members = new LinkedHashMap<String, List<String>>();
        for (var stock : List.of(kospi, kosdaq)) {
            var history = new ArrayList<HistoricalBar>();
            var dates = new ArrayList<String>();
            int session = 0;
            for (var date = first; !date.isAfter(to); date = date.plusDays(1)) {
                if (date.getDayOfWeek().getValue() >= 6) continue;
                when(calendar.day(date)).thenReturn(new TradingCalendar.Day(date, TradingCalendar.Status.OPEN, LocalTime.NOON, "TEST"));
                int phase = session++ % 30;
                long price = phase == 25 ? 120 : 100;
                long previous = phase == 26 ? 120 : 100;
                long turnover = stock == kosdaq ? 200000000 : 100000000;
                history.add(new HistoricalBar(date, 100, price + 1, 99, price, 1000, turnover, previous));
                dates.add(date.toString());
            }
            members.put(stock.stockCode(), dates);
            when(provider.historicalDaily(stock.stockCode(), first, to)).thenReturn(history);
        }
        when(calendar.day(any())).thenAnswer(call -> new TradingCalendar.Day(call.getArgument(0),
                TradingCalendar.Status.UNKNOWN, null, "TEST"));
        var archive = new KiwoomMarketArchive(json, provider, calendar);
        var service = new MarketStudyService(json,archive,clock,dir.toString());
        var baseline = new Spec(Market.ALL,Strategy.BREAKOUT,from,to,1000000,1,2,10,5,0,null);
        var id = UUID.randomUUID();
        var api = MockMvcBuilders.standaloneSetup(new MarketStudyController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
        String endpoint = "/api/v1/swing-backtests/market-runs";
        api.perform(put(endpoint + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(baseline)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.state").value("QUEUED"));
        service.execute(id);
        assertThat(service.job(id).state()).as(service.job(id).message()).isEqualTo("COMPLETE");
        var saved = service.result(id);
        assertThat(saved.version()).isEqualTo("KIWOOM_BREAKOUT_1");
        verify(provider, never()).historicalDaily(eq(fund.stockCode()), any(), any());
        assertThat(saved.metrics().sessions()).isGreaterThan(500);
        assertThat(saved.metrics().closed()).isPositive();
        assertThat(saved.metrics().averageNet()).isNegative();
        assertThat(saved.trades().stream().filter(t -> t.entryDate()!=null).findFirst().orElseThrow().pick().code()).isEqualTo("000002");
        assertThat(saved.trades()).filteredOn(t -> t.entryDate()!=null).allSatisfy(t -> {
            assertThat(members.get(t.pick().code())).contains(t.pick().signalDate().toString());
            assertThat(t.entryDate()).isAfter(t.pick().signalDate());
        });
        var second = new MarketStudyService(json,archive,clock,dir.toString());
        assertThat(second.result(id)).isEqualTo(saved);
        var variant = new Spec(Market.ALL,Strategy.VOLUME_BREAKOUT,from,to,1000000,1,2,10,5,0,null);
        var other = UUID.randomUUID();
        second.create(other,variant);
        second.execute(other);
        assertThat(second.result(other).metrics().signals()).isZero();
        assertThat(second.result(other).inputHash()).isEqualTo(saved.inputHash());
        assertThat(second.jobs()).extracting(Job::id).containsExactlyInAnyOrder(id,other);
        api.perform(get(endpoint)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        api.perform(get(endpoint + "/" + id + "/result")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inputHash").value(saved.inputHash()))
                .andExpect(jsonPath("$.data.metrics.closed").value(saved.metrics().closed()));
    }

    @Test void missingUniverseCannotPublishSuccessAndCanRetrySameRequest() throws Exception {
        var calendar = mock(TradingCalendar.class);
        when(calendar.day(any())).thenReturn(new TradingCalendar.Day(first,TradingCalendar.Status.OPEN,LocalTime.NOON,"TEST"));
        var archive = new KiwoomMarketArchive(json, mock(KiwoomScreenerDataProvider.class), calendar);
        var service = new MarketStudyService(json,archive,clock,dir.toString());
        var day = LocalDate.of(2025,3,3);
        var id = UUID.randomUUID();
        var spec = new Spec(Market.KOSPI,Strategy.BREAKOUT,day,day.plusDays(90),1000000,1,3,0,0,0,null);
        service.create(id,spec);
        service.execute(id);
        assertThat(service.job(id).state()).isEqualTo("FAILED");
        assertThatThrownBy(() -> service.result(id)).hasMessageContaining("완료");
        assertThat(service.control(id,true).state()).isEqualTo("QUEUED");
        assertThat(service.control(id,false).state()).isEqualTo("CANCELLED");
        service.execute(id);
        assertThat(service.job(id).metrics()).isNull();
    }
}
