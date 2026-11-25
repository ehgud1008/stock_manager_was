package com.example.stockanalysis.engine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Optional user-supplied position, never inferred from a chart or a generated scenario. */
public record PositionContext(boolean held, Instant openedAt, BigDecimal entryPrice,
                              BigDecimal stopPrice, List<BigDecimal> targets, StopPolicy stopPolicy) {
    public enum StopPolicy { CLOSE, TOUCH }
    public PositionContext {
        targets = targets == null ? List.of() : List.copyOf(targets);
        if (targets.size() > 5) throw new IllegalArgumentException("At most 5 position targets");
        if (held) {
            Objects.requireNonNull(openedAt, "openedAt");
            Objects.requireNonNull(entryPrice, "entryPrice");
            Objects.requireNonNull(stopPolicy, "stopPolicy");
            if (entryPrice.signum() <= 0 || stopPrice != null && stopPrice.signum() <= 0
                    || targets.stream().anyMatch(t -> t == null || t.compareTo(entryPrice) <= 0))
                throw new IllegalArgumentException("Invalid position prices");
        } else if (openedAt != null || entryPrice != null || stopPrice != null || !targets.isEmpty() || stopPolicy != null) {
            throw new IllegalArgumentException("Unheld position must not contain trade parameters");
        }
    }
}
