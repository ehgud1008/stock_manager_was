package com.example.stockanalysis.engine.scoring;

import com.example.stockanalysis.engine.model.FactorResult;
import java.math.BigDecimal;
import java.util.List;

public interface ScorePolicy {

    BigDecimal calculate(List<FactorResult> factors);
}
