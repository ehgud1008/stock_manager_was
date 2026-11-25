package com.example.stockanalysis.health.api;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String application,
        String profile,
        LocalDateTime timestamp
) {
}
