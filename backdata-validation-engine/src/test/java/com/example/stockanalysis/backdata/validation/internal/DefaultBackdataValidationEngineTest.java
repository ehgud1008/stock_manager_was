package com.example.stockanalysis.backdata.validation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.backdata.validation.model.BackdataSchema;
import com.example.stockanalysis.backdata.validation.model.MapBackdataSchemaRegistry;
import com.example.stockanalysis.backdata.validation.model.ValidationCommand;
import com.example.stockanalysis.backdata.validation.model.ValidationProfile;
import com.example.stockanalysis.backdata.validation.model.ValidationSeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultBackdataValidationEngineTest {

    private static final Instant OBSERVED_AT = Instant.parse("2025-01-03T08:00:00Z");
    private static final Instant VALIDATION_TIME = Instant.parse("2025-01-03T08:01:00Z");

    @Test
    void acceptsValidDailyCandle() {
        var engine = engine(candleSchema("ka10081", BackdataDatasetType.DAILY_CANDLE));
        BackdataRecord record = candle(
                "ka10081",
                BackdataDatasetType.DAILY_CANDLE,
                "005930",
                "2025-01-03",
                "55000",
                "57000",
                "54500",
                "56500",
                "1000000"
        );

        var result = engine.validate(new ValidationCommand(
                ValidationProfile.BACKTEST_READY,
                VALIDATION_TIME,
                List.of(record)
        ));

        assertThat(result.isBacktestReady()).isTrue();
        assertThat(result.validRecords()).containsExactly(record);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void quarantinesInvalidOhlcRelationship() {
        var engine = engine(candleSchema("ka10081", BackdataDatasetType.DAILY_CANDLE));
        BackdataRecord record = candle(
                "ka10081",
                BackdataDatasetType.DAILY_CANDLE,
                "005930",
                "2025-01-03",
                "55000",
                "54000",
                "53000",
                "56500",
                "1000000"
        );

        var result = engine.validate(new ValidationCommand(
                ValidationProfile.BACKTEST_READY,
                VALIDATION_TIME,
                List.of(record)
        ));

        assertThat(result.isBacktestReady()).isFalse();
        assertThat(result.quarantinedRecords()).containsExactly(record);
        assertThat(result.issues())
                .extracting(issue -> issue.code())
                .contains("HIGH_PRICE_RELATION_INVALID");
    }

    @Test
    void detectsLargeCrossSourceDifference() {
        var engine = engine(
                candleSchema("ka10081", BackdataDatasetType.DAILY_CANDLE),
                candleSchema("ka10086", BackdataDatasetType.DAILY_PRICE)
        );
        BackdataRecord chart = candle(
                "ka10081", BackdataDatasetType.DAILY_CANDLE, "005930",
                "2025-01-03", "55000", "57000", "54500", "56500", "1000000"
        );
        BackdataRecord dailyPrice = candle(
                "ka10086", BackdataDatasetType.DAILY_PRICE, "005930",
                "2025-01-03", "55000", "62000", "54500", "62000", "1000000"
        );

        var result = engine.validate(new ValidationCommand(
                ValidationProfile.BACKTEST_READY,
                VALIDATION_TIME,
                List.of(chart, dailyPrice)
        ));

        assertThat(result.issues())
                .anySatisfy(issue -> {
                    assertThat(issue.code()).isEqualTo("CROSS_SOURCE_VALUE_MISMATCH");
                    assertThat(issue.severity()).isEqualTo(ValidationSeverity.ERROR);
                    assertThat(issue.apiId()).isEqualTo("ka10086");
                });
        assertThat(result.quarantinedRecords()).containsExactly(dailyPrice);
    }

    @Test
    void rejectsShortVolumeGreaterThanTotalVolume() {
        BackdataSchema schema = new BackdataSchema(
                "ka10014",
                BackdataDatasetType.SHORT_SELLING,
                true,
                false,
                true,
                true,
                Set.of("volume", "shortVolume"),
                Set.of("volume", "shortVolume", "shortRatio"),
                Set.of("volume", "shortVolume", "shortRatio")
        );
        var engine = engine(schema);
        BackdataRecord record = new BackdataRecord(
                "ka10014",
                BackdataDatasetType.SHORT_SELLING,
                "005930",
                null,
                null,
                LocalDate.parse("2025-01-03"),
                OBSERVED_AT,
                Map.of("volume", "100", "shortVolume", "110", "shortRatio", "110")
        );

        var result = engine.validate(new ValidationCommand(
                ValidationProfile.BACKTEST_READY,
                VALIDATION_TIME,
                List.of(record)
        ));

        assertThat(result.issues())
                .extracting(issue -> issue.code())
                .contains("SHORT_VOLUME_EXCEEDS_VOLUME", "SHORT_RATIO_OUT_OF_RANGE");
    }

    @Test
    void rejectsEmptyDataset() {
        var engine = engine(candleSchema("ka10081", BackdataDatasetType.DAILY_CANDLE));

        var result = engine.validate(new ValidationCommand(
                ValidationProfile.BACKTEST_READY,
                VALIDATION_TIME,
                List.of()
        ));

        assertThat(result.isBacktestReady()).isFalse();
        assertThat(result.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.code()).isEqualTo("EMPTY_DATASET");
                    assertThat(issue.severity()).isEqualTo(ValidationSeverity.FATAL);
                });
    }

    private DefaultBackdataValidationEngine engine(BackdataSchema... schemas) {
        return new DefaultBackdataValidationEngine(
                new MapBackdataSchemaRegistry(List.of(schemas))
        );
    }

    private BackdataSchema candleSchema(String apiId, BackdataDatasetType type) {
        return new BackdataSchema(
                apiId,
                type,
                true,
                false,
                true,
                true,
                Set.of("open", "high", "low", "close", "volume"),
                Set.of("open", "high", "low", "close", "volume", "tradingAmount"),
                Set.of("open", "high", "low", "close", "volume", "tradingAmount")
        );
    }

    private BackdataRecord candle(
            String apiId,
            BackdataDatasetType type,
            String stockCode,
            String date,
            String open,
            String high,
            String low,
            String close,
            String volume
    ) {
        return new BackdataRecord(
                apiId,
                type,
                stockCode,
                null,
                null,
                LocalDate.parse(date),
                OBSERVED_AT,
                Map.of(
                        "open", open,
                        "high", high,
                        "low", low,
                        "close", close,
                        "volume", volume,
                        "tradingAmount", "500000000"
                )
        );
    }
}
