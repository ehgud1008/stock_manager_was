package com.example.stockanalysis.backdata.validation.model;

public enum ValidationProfile {
    QUICK,
    BACKTEST_READY,
    AUDIT;

    public boolean includesSeriesRules() {
        return this != QUICK;
    }
}
