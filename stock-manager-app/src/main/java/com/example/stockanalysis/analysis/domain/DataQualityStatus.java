package com.example.stockanalysis.analysis.domain;

public enum DataQualityStatus {
    PENDING,
    COMPLETE,
    PARTIAL,
    INVALID;

    public static final String MONTHLY_REFERENCE_SHORTAGE = "월봉 참고 점수 표본 부족: 20개 필요";

    /** Only this known SWING reference warning is advisory. Other missing data stays blocking. */
    public static boolean hasBlockingWarnings(com.example.stockanalysis.engine.model.AnalysisMode mode,
                                              java.util.List<String> warnings) {
        return warnings.stream().anyMatch(warning -> mode != com.example.stockanalysis.engine.model.AnalysisMode.SWING
                || !MONTHLY_REFERENCE_SHORTAGE.equals(warning));
    }
}
