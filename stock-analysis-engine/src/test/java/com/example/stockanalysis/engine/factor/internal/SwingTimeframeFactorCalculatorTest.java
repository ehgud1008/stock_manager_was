package com.example.stockanalysis.engine.factor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.FactorResult;
import com.example.stockanalysis.engine.model.StockSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SwingTimeframeFactorCalculatorTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 8, 28);

    @Test
    void calculatesIndependentDailyWeeklyAndMonthlyTechnicalScores() {
        List<DailyCandle> uptrend = candles(220, true);
        AnalysisInput input = input(uptrend, uptrend, uptrend);

        FactorResult daily = SwingTimeframeFactorCalculator.daily().calculate(input);
        FactorResult weekly = SwingTimeframeFactorCalculator.weekly().calculate(input);
        FactorResult monthly = SwingTimeframeFactorCalculator.monthly().calculate(input);

        assertThat(daily.factorCode()).isEqualTo("DAILY_TECHNICAL");
        assertThat(weekly.factorCode()).isEqualTo("WEEKLY_TECHNICAL");
        assertThat(monthly.factorCode()).isEqualTo("MONTHLY_TECHNICAL");
        assertThat(List.of(daily, weekly, monthly))
                .allSatisfy(result -> {
                    assertThat(result.score()).isGreaterThan(BigDecimal.valueOf(65));
                    assertThat(result.rawValue().scale()).isEqualTo(1);
                    assertThat(result.reason()).contains("RSI=", "MACD", "일목", "거래량");
                });
        assertThat(daily.weight()).isEqualByComparingTo("0.10");
        assertThat(weekly.weight()).isEqualByComparingTo("0.025");
        assertThat(monthly.weight()).isEqualByComparingTo("0.025");
    }

    @Test
    void appliesNeutralScoreWhenMonthlySamplesAreInsufficient() {
        List<DailyCandle> daily = candles(220, true);
        FactorResult result = SwingTimeframeFactorCalculator.monthly()
                .calculate(input(daily, daily, candles(12, true)));

        assertThat(result.score()).isEqualByComparingTo("50");
        assertThat(result.reason()).contains("표본이 12개로 부족");
    }

    private AnalysisInput input(
            List<DailyCandle> daily,
            List<DailyCandle> weekly,
            List<DailyCandle> monthly
    ) {
        return new AnalysisInput(
                new StockSnapshot("005930", "삼성전자", "KRX", amount("200")),
                AnalysisMode.SWING,
                BASE_DATE,
                Instant.parse("2026-08-28T07:00:00Z"),
                daily,
                weekly,
                monthly,
                Map.of()
        );
    }

    private List<DailyCandle> candles(int count, boolean rising) {
        List<DailyCandle> candles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            long close = rising ? 100L + index : 100L + count - index;
            BigDecimal closePrice = BigDecimal.valueOf(close);
            candles.add(new DailyCandle(
                    BASE_DATE.minusDays(count - index - 1L),
                    closePrice,
                    closePrice.add(BigDecimal.ONE),
                    closePrice.subtract(BigDecimal.ONE),
                    closePrice,
                    1_000L + index
            ));
        }
        return candles;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
