package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;

import com.example.stockanalysis.screener.domain.ScreenerItem.Stock;
import com.example.stockanalysis.screener.domain.ScreenerRunRequest;
import com.example.stockanalysis.screener.infrastructure.KiwoomScreenerDataProvider;
import com.example.stockanalysis.screener.infrastructure.KiwoomScreenerDataProvider.HistoricalBar;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
class KiwoomMarketArchive {
    private record Universe(List<Stock> stocks) {}
    private record History(List<HistoricalBar> bars) {}
    private final ObjectMapper json;
    private final KiwoomScreenerDataProvider provider;
    private final TradingCalendar calendar;

    KiwoomMarketArchive(ObjectMapper json, KiwoomScreenerDataProvider provider, TradingCalendar calendar) {
        this.json = json;
        this.provider = provider;
        this.calendar = calendar;
    }

    List<Stock> collect(Path path, Spec spec, LocalDate from, Consumer<String> progress) throws IOException {
        Path manifest = path.resolve("universe.json.gz");
        if (!Files.exists(manifest)) {
            var stocks = provider.universe(ScreenerRunRequest.Market.valueOf(spec.market().name())).stream()
                    .filter(stock -> "ST".equals(stock.stockType()))
                    .filter(stock -> stock.listedDate() == null || !stock.listedDate().isAfter(spec.to()))
                    .toList();
            if (stocks.isEmpty()) throw invalid("선택한 시장과 기간에서 조회할 상장 주식이 없습니다.");
            MarketFiles.write(json, manifest, new Universe(stocks));
        }
        var stocks = MarketFiles.read(json, manifest, Universe.class).stocks();
        for (int i = 0; i < stocks.size(); i++) {
            var stock = stocks.get(i);
            progress.accept("키움 일봉 수집 " + (i + 1) + " / " + stocks.size() + " · " + stock.stockCode());
            if (Files.exists(complete(path, stock))) continue;
            var start = stock.listedDate() != null && stock.listedDate().isAfter(from) ? stock.listedDate() : from;
            Map<Integer, List<HistoricalBar>> years;
            try {
                years = provider.historicalDaily(stock.stockCode(), start, spec.to()).stream()
                        .collect(java.util.stream.Collectors.groupingBy(bar -> bar.date().getYear()));
            } catch (java.util.concurrent.CancellationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new IOException("키움 일봉 조회 실패 · " + stock.stockCode() + " · " + e.getMessage(), e);
            }
            if (years.isEmpty()) throw new IOException("조회 기간의 키움 일봉이 비었습니다 · " + stock.stockCode());
            for (var year : years.entrySet()) {
                MarketFiles.write(json, slice(path, stock, year.getKey()), new History(year.getValue()));
            }
            MarketFiles.write(json, complete(path, stock), List.copyOf(years.keySet()));
        }
        return stocks;
    }

    Map<LocalDate, Map<String, Quote>> sessions(Path path, List<Stock> stocks, int year, Runnable check) throws IOException {
        var sessions = new TreeMap<LocalDate, Map<String, Quote>>();
        for (var stock : stocks) {
            check.run();
            var slice = slice(path, stock, year);
            if (!Files.exists(slice)) continue;
            for (var bar : MarketFiles.read(json, slice, History.class).bars()) {
                sessions.computeIfAbsent(bar.date(), ignored -> new TreeMap<>()).put(stock.stockCode(),
                        new Quote(stock.stockCode(), stock.stockName(), bar.open(), bar.high(), bar.low(),
                                bar.close(), bar.volume(), bar.turnover(), bar.previousClose()));
            }
        }
        return sessions;
    }

    TradingCalendar.Status status(LocalDate day) {
        return calendar.day(day).status();
    }

    private static Path slice(Path path, Stock stock, int year) {
        return path.resolve(Integer.toString(year)).resolve(stock.stockCode() + ".json.gz");
    }

    private static Path complete(Path path, Stock stock) {
        return path.resolve("collected").resolve(stock.stockCode() + ".json");
    }
}
