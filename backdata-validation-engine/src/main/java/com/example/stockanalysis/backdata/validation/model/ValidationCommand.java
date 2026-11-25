package com.example.stockanalysis.backdata.validation.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ValidationCommand(
        ValidationProfile profile,
        Instant validationTime,
        List<BackdataRecord> records
) {

    public ValidationCommand {
        profile = Objects.requireNonNullElse(profile, ValidationProfile.BACKTEST_READY);
        validationTime = Objects.requireNonNullElseGet(validationTime, Instant::now);
        records = List.copyOf(Objects.requireNonNullElse(records, List.of()));
    }
}
