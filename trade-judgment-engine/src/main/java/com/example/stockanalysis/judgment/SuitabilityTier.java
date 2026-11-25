package com.example.stockanalysis.judgment;

import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.util.*;

/** Versioned score bands, not probabilities or permission to enter. */
public record SuitabilityTier(String policyVersion, String tier, BigDecimal score, List<String> weaknesses) {
    public static final String VERSION="suitability-tier-v1";
    public static String grade(BigDecimal score) {
        if(score==null || score.signum()<0 || score.compareTo(new BigDecimal("100"))>0)return "UNSCORED";
        return score.compareTo(new BigDecimal("80"))>=0?"A":score.compareTo(new BigDecimal("70"))>=0?"B":score.compareTo(new BigDecimal("65"))>=0?"C":"D";
    }
    public static SuitabilityTier from(AnalysisResult analysis) {
        var score=analysis==null?null:analysis.totalScore();
        var weaknesses=analysis==null?List.of("분석 자료 없음"):analysis.factors().stream()
                .filter(f->f.score()!=null&&f.weight()!=null&&f.weight().signum()>0)
                .sorted(Comparator.comparing((FactorResult f)->new BigDecimal("100").subtract(f.score()).multiply(f.weight())).reversed())
                .limit(2).map(f->f.factorCode()+" "+f.score()+"점: "+f.reason()).toList();
        if("UNSCORED".equals(grade(score)))weaknesses=List.of("점수 미산정: 자료·가격 시나리오를 확인하세요.");
        return new SuitabilityTier(VERSION,grade(score),score,weaknesses);
    }
    public SuitabilityTier { weaknesses=List.copyOf(weaknesses); }
}
