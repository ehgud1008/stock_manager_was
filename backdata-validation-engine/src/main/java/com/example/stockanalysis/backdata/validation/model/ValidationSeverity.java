package com.example.stockanalysis.backdata.validation.model;

public enum ValidationSeverity {
    INFO,
    WARNING,
    ERROR,
    FATAL;

    public boolean blocksRecord() {
        return this == ERROR || this == FATAL;
    }
}
