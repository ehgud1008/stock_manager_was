package com.example.stockanalysis.analysis.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AnalysisFactorResponseTest {

    @Test
    void roundsPersistedPercentAndMultipleValuesToOneDecimal() {
        AnalysisFactorResponse percent = AnalysisFactorResponse.from(factor(
                "TREND",
                "18.60465116",
                "현재가의 20일 이동평균 대비 괴리율은 18.60465116%입니다."
        ));
        AnalysisFactorResponse multiple = AnalysisFactorResponse.from(factor(
                "VOLUME",
                "1.42387654",
                "최근 거래량은 20일 평균의 1.42387654배입니다."
        ));

        assertThat(percent.rawValue()).isEqualByComparingTo("18.6");
        assertThat(percent.reason()).contains("18.6%입니다.");
        assertThat(multiple.rawValue()).isEqualByComparingTo("1.4");
        assertThat(multiple.reason()).contains("1.4배입니다.");
    }

    private AnalysisFactorView factor(String code, String rawValue, String reason) {
        return new AnalysisFactorView(
                code,
                new BigDecimal(rawValue),
                new BigDecimal("80.0000"),
                new BigDecimal("0.20"),
                new BigDecimal("16.0000"),
                reason
        );
    }
}
