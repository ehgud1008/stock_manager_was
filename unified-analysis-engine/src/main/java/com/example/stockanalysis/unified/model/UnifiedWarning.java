package com.example.stockanalysis.unified.model;

import java.util.Objects;

/** Retains the origin of a warning without treating every informational message as a failure. */
public record UnifiedWarning(Source source, String message) {
    public enum Source { INPUT, ANALYSIS, SIGNAL, SCREENER }

    public UnifiedWarning {
        Objects.requireNonNull(source, "source");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }
}
