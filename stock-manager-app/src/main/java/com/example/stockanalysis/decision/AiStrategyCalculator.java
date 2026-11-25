package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.util.*;

/** Derives comparable distances from already validated AI prices. No probability estimates. */
public final class AiStrategyCalculator {
    private final DecisionJson json;
    public AiStrategyCalculator(DecisionJson json) {this.json=json;}
    public JsonNode calculate(Review review,JsonNode input) {
        boolean held="HELD".equals(input.path("context").path("positionStatus").asText());
        BigDecimal current=input.path("policy").path("metrics").path("CURRENT").path("value").decimalValue();
        var out=new ArrayList<Object>();
        for(var s:review.independent().strategies()) {
            BigDecimal reference=held?current:s.entryHigh().value(),risk=reference.subtract(s.stop().value());
            var targets=new ArrayList<Object>();
            for(var target:s.targets()) targets.add(Map.of("price",target.value(),"upsidePct",pct(target.value().subtract(reference),reference),
                    "rewardRisk",target.value().subtract(reference).divide(risk,2,RoundingMode.HALF_UP)));
            var row=new LinkedHashMap<String,Object>();row.put("strategyId",s.id());row.put("referencePrice",reference);
            row.put("referenceKind",held?"CURRENT_PRICE":"ENTRY_UPPER");row.put("lossPct",pct(risk,reference));row.put("targets",targets);
            JsonNode buy=input.path("context").path("averageBuyPrice");
            if(held && buy.isNumber() && buy.decimalValue().signum()>0) row.put("costBasisReturnPct",pct(current.subtract(buy.decimalValue()),buy.decimalValue()));
            var quantity=input.path("context").path("quantity");
            if(held && quantity.isNumber() && buy.isNumber()) {
                row.put("quantity",quantity.decimalValue());
                row.put("pnlAmount",current.subtract(buy.decimalValue()).multiply(quantity.decimalValue()));
                row.put("additionalRiskAmount",risk.max(BigDecimal.ZERO).multiply(quantity.decimalValue()));
                row.put("stopPnlAmount",s.stop().value().subtract(buy.decimalValue()).multiply(quantity.decimalValue()));
            }
            row.put("costsIncluded",false);out.add(row);
        }
        return json.tree(out);
    }
    private BigDecimal pct(BigDecimal delta,BigDecimal reference) {return delta.multiply(BigDecimal.valueOf(100)).divide(reference,2,RoundingMode.HALF_UP);}
}
