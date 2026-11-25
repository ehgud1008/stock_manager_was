package com.example.stockanalysis.unified.report;

import com.example.stockanalysis.unified.report.ReportModels.StockDecision;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;

/** Shared by publication and saved-snapshot research. */
public final class CandidateRanking {
    private CandidateRanking() {}
    public static int preference(StockDecision stock) {
        String pattern=stock.newEntry()!=null && stock.newEntry().selectedSignal()!=null
                ?stock.newEntry().selectedSignal().type():stock.strategy();
        return stock.transition61() || Set.of("PULLBACK_RECOVERY","BREAKOUT_RETEST").contains(pattern) ? 1 : 0;
    }
    public static BigDecimal score(StockDecision stock) {
        return stock.newEntry()==null || stock.newEntry().suitability()==null ? null : stock.newEntry().suitability().score();
    }
    public static Comparator<StockDecision> comparator() {
        return Comparator.comparingInt((StockDecision s)->"ENTRY_CONDITIONAL".equals(s.disposition())?0:1)
                .thenComparing(Comparator.comparingInt(CandidateRanking::preference).reversed())
                .thenComparing(CandidateRanking::score,Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StockDecision::liquidity,Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StockDecision::code);
    }
}
