package com.example.stockanalysis.engine.factor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.StockSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SwingDirectionalFactorCalculatorsTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 9, 3);

    @Test
    void scoresHigherHighHigherLowStructureAboveFallingStructure() {
        var calculator = new TrendFactorCalculator();

        var rising = calculator.calculate(input(candles(80, 100, 1, 2), Map.of()));
        var falling = calculator.calculate(input(candles(80, 300, -1, 2), Map.of()));

        assertThat(rising.score()).isGreaterThan(falling.score());
        assertThat(rising.reason()).contains("고점·저점 상승");
        assertThat(falling.reason()).contains("고점·저점 하락");
    }

    @Test
    void confirmsUpMoveWithExpandingVolumeAndPenalizesDownMove() {
        var calculator = new VolumeFactorCalculator();
        List<DailyCandle> up = candles(30, 100, 1, 1);
        List<DailyCandle> down = candles(30, 200, -1, 1);
        up = withLatestVolume(up, 4_000);
        down = withLatestVolume(down, 4_000);

        assertThat(calculator.calculate(input(up, Map.of())).score())
                .isGreaterThan(calculator.calculate(input(down, Map.of())).score());
    }

    @Test
    void usesMarketAndIndustryReturnsForRelativeStrength() {
        Map<String, BigDecimal> observations = Map.of(
                RelativeStrengthFactorCalculator.MARKET_RETURN_20, amount("5"),
                RelativeStrengthFactorCalculator.INDUSTRY_RETURN_20, amount("7")
        );

        var result = new RelativeStrengthFactorCalculator()
                .calculate(input(candles(30, 100, 1, 1), observations));

        assertThat(result.score()).isGreaterThan(BigDecimal.valueOf(50));
        assertThat(result.reason()).contains("시장·업종 평균 대비");
    }

    @Test
    void penalizesLargeAtrAndDrawdown() {
        List<DailyCandle> stable = candles(60, 100, 1, 1);
        List<DailyCandle> volatileSeries = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            long close = index < 30 ? 200 + index : 230 - (index - 29) * 4L;
            close = Math.max(close, 50);
            BigDecimal price = BigDecimal.valueOf(close);
            volatileSeries.add(new DailyCandle(
                    BASE_DATE.minusDays(59L - index),
                    price,
                    price.add(BigDecimal.TEN),
                    price.subtract(BigDecimal.TEN).max(BigDecimal.ONE),
                    price,
                    1_000L
            ));
        }

        var calculator = new RiskStabilityFactorCalculator();
        assertThat(calculator.calculate(input(stable, Map.of())).score())
                .isGreaterThan(calculator.calculate(input(volatileSeries, Map.of())).score());
    }

    private List<DailyCandle> candles(int count, long start, long step, long range) {
        List<DailyCandle> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BigDecimal close = BigDecimal.valueOf(start + step * index);
            result.add(new DailyCandle(
                    BASE_DATE.minusDays(count - index - 1L),
                    close,
                    close.add(BigDecimal.valueOf(range)),
                    close.subtract(BigDecimal.valueOf(range)).max(BigDecimal.ONE),
                    close,
                    1_000L + index
            ));
        }
        return result;
    }

    private List<DailyCandle> withLatestVolume(List<DailyCandle> source, long volume) {
        List<DailyCandle> result = new ArrayList<>(source);
        DailyCandle latest = result.remove(result.size() - 1);
        result.add(new DailyCandle(
                latest.businessDate(), latest.open(), latest.high(), latest.low(),
                latest.close(), volume
        ));
        return result;
    }

    private AnalysisInput input(
            List<DailyCandle> candles,
            Map<String, BigDecimal> observations
    ) {
        DailyCandle latest = candles.get(candles.size() - 1);
        return new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KOSPI", latest.close()),
                AnalysisMode.SWING,
                BASE_DATE,
                Instant.parse("2026-09-03T07:00:00Z"),
                candles,
                new LinkedHashMap<>(observations)
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
