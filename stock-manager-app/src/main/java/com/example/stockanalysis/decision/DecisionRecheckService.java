package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.HoldingService;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.stock.application.StockQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Read-only, indicative comparison. Never submits jobs or updates a historical plan. */
@Service
public class DecisionRecheckService {
    private final DecisionStore store;
    private final DecisionSettings settings;
    private final DecisionJson json;
    private final HoldingService holdings;
    private final AccountRiskService accounts;
    private final StockQueryService stocks;
    private final DecisionValidityService validity;
    private final Clock clock;

    public DecisionRecheckService(DecisionStore store, DecisionSettings settings, DecisionJson json,
            HoldingService holdings, AccountRiskService accounts, StockQueryService stocks,
            DecisionValidityService validity, Clock clock) {
        this.store=store; this.settings=settings; this.json=json; this.holdings=holdings;
        this.accounts=accounts; this.stocks=stocks; this.validity=validity; this.clock=clock;
    }

    public JsonNode get(String id) {
        if(!settings.enabled())throw new BusinessException(ErrorCode.DECISION_DISABLED);
        var job=store.find(settings.ownerScope(),id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!"DECISION".equals(job.kind()) || !"INTEGRATED_DECISION".equals(job.input().path("mode").asText())
                || !"SUCCEEDED".equals(job.status()))
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"완료된 통합 판단만 재확인할 수 있습니다.");
        var saved=job.input().path("policy");
        ObjectNode out=JsonNodeFactory.instance.objectNode();
        out.put("version","decision-recheck-v1"); out.put("decisionId",id);
        out.put("status","PARTIAL"); out.put("executionStatus","PRE_EXECUTION_ONLY");
        out.put("startedAt",clock.instant().toString());
        var reasons=out.putArray("reasons");
        // ka10001 supplies a price but no exchange timestamp in the current domain contract.
        var quote=out.putObject("price"); quote.put("source","KIWOOM_KA10001");
        quote.putNull("sourceAsOf"); quote.put("freshness","UNVERIFIABLE");
        quote.put("basisCompatibility","UNVERIFIABLE");quote.set("savedPriceBasis",saved.path("priceBasis"));
        quote.set("savedReference",saved.path("stock").path("currentPrice"));
        quote.set("savedPriceDate",saved.path("priceDate")); quote.set("entryFrom",saved.path("plan").path("entryFrom"));
        quote.set("entryTo",saved.path("plan").path("entryTo"));
        BigDecimal price=null;
        try {
            var profile=stocks.getStockProfile(job.stockCode());
            if(profile==null || profile.stockCode()==null || !job.stockCode().equals(profile.stockCode().value())) {
                quote.put("status","SYMBOL_MISMATCH"); reasons.add("PRICE_SYMBOL_MISMATCH");
            } else if(profile.currentPrice()==null || profile.currentPrice().signum()<=0) {
                quote.put("status","INVALID"); reasons.add("PRICE_INVALID");
            } else {price=profile.currentPrice();quote.put("status","AVAILABLE");quote.put("value",price);}
        } catch(RuntimeException e) {quote.put("status","UNAVAILABLE");reasons.add("PRICE_LOOKUP_FAILED");}
        quote.put("checkedAt",clock.instant().toString()); reasons.add("PRICE_TIME_UNAVAILABLE");
        reasons.add("PRICE_BASIS_UNVERIFIABLE");
        boolean planValid=new DecisionValidityPolicy().validPlan(saved.path("plan"));
        String range="UNVERIFIABLE";
        if(price!=null && planValid)range=price.compareTo(saved.path("plan").path("entryFrom").decimalValue())<0?"BELOW":
                price.compareTo(saved.path("plan").path("entryTo").decimalValue())>0?"ABOVE":"WITHIN";
        quote.put("entryRangeStatus",range);
        if(Set.of("BELOW","ABOVE").contains(range))reasons.add("PRICE_OUTSIDE_ENTRY");
        if(!planValid)reasons.add("PLAN_INVALID");
        var tick=out.putObject("tickValidation");tick.put("status","UNVERIFIABLE");
        tick.put("reason","MARKET_PRODUCT_RULE_UNAVAILABLE");reasons.add("TICK_RULE_UNAVAILABLE");

        var balance=holdings.single(job.stockCode());
        var h=balance.items().get(job.stockCode());
        var holding=out.putObject("holding"); holding.put("source","MANUAL_REGISTERED");
        holding.put("status",!"AVAILABLE".equals(balance.status())?"UNAVAILABLE":h==null?"NOT_REGISTERED":"AVAILABLE");
        holding.put("checkedAt",balance.checkedAt().toString());
        holding.set("saved",saved.path("holding").path("holding")); holding.set("current",json.tree(h));
        String hc="UNAVAILABLE".equals(holding.path("status").asText())?"UNKNOWN":
                Objects.equals(holding.path("saved"),holding.path("current"))?"UNCHANGED":"CHANGED";
        holding.put("comparison",hc);
        if(!"UNCHANGED".equals(hc))reasons.add("UNKNOWN".equals(hc)?"HOLDING_UNAVAILABLE":"HOLDING_CHANGED");
        BigDecimal stop=planValid?saved.path("plan").path("stopLoss").decimalValue():null;
        if(h!=null && h.stopPrice()!=null && h.stopPrice().signum()>0)stop=stop==null?h.stopPrice():stop.max(h.stopPrice());
        boolean stopBreached=price!=null && stop!=null && price.compareTo(stop)<=0;
        quote.set("stopReference",json.tree(stop));
        quote.put("stopReferenceStatus",price==null || stop==null?"UNVERIFIABLE":stopBreached?"BREACHED":"ABOVE");
        if(stopBreached)reasons.add("STOP_REFERENCE_BREACHED");
        var account=json.tree(accounts.get());
        var risk=out.putObject("accountRisk");risk.put("source","MANUAL_SETTINGS");risk.set("saved",saved.path("accountRisk").path("settings"));
        risk.set("current",account.path("settings"));risk.set("checkedAt",account.path("checkedAt"));risk.set("status",account.path("status"));
        String ac=!"AVAILABLE".equals(account.path("status").asText())?"UNKNOWN":
                risk.path("saved").equals(risk.path("current"))?"UNCHANGED":"CHANGED";
        risk.put("comparison",ac);
        if(!"UNCHANGED".equals(ac))reasons.add("UNKNOWN".equals(ac)?"ACCOUNT_RISK_UNAVAILABLE":"ACCOUNT_RISK_CHANGED");

        ObjectNode current=saved.deepCopy();current.set("accountRisk",account);
        var ch=current.putObject("holding");ch.put("positionStatus",!"AVAILABLE".equals(balance.status())?"UNKNOWN":h!=null && h.quantity().signum()>0?"HELD":"NOT_HELD");
        ch.set("holding",json.tree(h));
        var sizing=out.putObject("sizing");sizing.put("basis","INDICATIVE_MANUAL_INPUTS_NO_RESERVATION");
        boolean quantityChanged=false;
        sizing.set("saved",job.result().path("riskSizing"));sizing.set("savedQuantity",job.result().path("quantity"));
        String action=job.result().path("decisionAction").asText();
        sizing.put("action",action);
        if(price==null) sizing.put("status","PRICE_UNAVAILABLE");
        else if(!Set.of("BUY","ADD","REDUCE","SELL").contains(action))sizing.put("status","ACTION_NOT_SIZED");
        else if(!PositionSizingPolicy.VERSION.equals(job.input().path("versions").path("sizing").asText()))
            sizing.put("status","LEGACY_POLICY_REQUIRES_NEW_DECISION");
        else if("UNKNOWN".equals(ch.path("positionStatus").asText()))sizing.put("status","POSITION_UNKNOWN");
        else if(h==null)sizing.put("status","POSITION_UNCONFIRMED");
        else if((Set.of("ADD","REDUCE","SELL").contains(action) && !"HELD".equals(ch.path("positionStatus").asText()))
                || ("BUY".equals(action) && "HELD".equals(ch.path("positionStatus").asText())))
            sizing.put("status","POSITION_ACTION_CHANGED");
        else {
            ((ObjectNode)current.path("stock")).put("currentPrice",price);
            // Saved entry upper bound remains the cost ceiling. Never move a plan into a new range.
            var calculated=new PositionSizingPolicy().calculate(current,action,clock.instant());
            sizing.set("current",calculated);sizing.set("status",calculated.path("status"));
            var quantity=calculated.path("tradeQuantity").isNumber()?calculated.path("tradeQuantity"):calculated.path("maxBuyQuantity");
            if("READY".equals(calculated.path("status").asText()) && quantity.isNumber()) {
                sizing.set("indicativeQuantity",quantity);
                if(job.result().path("quantity").isNumber() && quantity.decimalValue().compareTo(job.result().path("quantity").decimalValue())!=0)
                    {reasons.add("QUANTITY_CHANGED");quantityChanged=true;}
            }
        }
        if(!"READY".equals(sizing.path("status").asText()))reasons.add("SIZING_"+sizing.path("status").asText());
        // A quote outside the old band cannot validate an entry even if the old budget calculation succeeds.
        sizing.put("entryRangeConfirmed","WITHIN".equals(range));
        var timing=validity.current(saved,clock.instant());out.set("validity",timing);
        boolean analysis=!"VALID".equals(timing.path("status").asText()) || !planValid || Set.of("BELOW","ABOVE").contains(range)
                || stopBreached;
        out.put("requiresAnalysisRefresh",analysis);
        out.put("requiresNewDecision",analysis || !"UNCHANGED".equals(hc) || !"UNCHANGED".equals(ac)
                || quantityChanged || timing.path("requiresResearchRefresh").asBoolean()
                || !Set.of("READY","ACTION_NOT_SIZED").contains(sizing.path("status").asText()));
        var links=out.putObject("links");links.put("analysis","/unified-analysis");links.put("holdings","/holdings");links.put("accountRisk","/settings#account-risk");
        out.put("evaluatedAt",clock.instant().toString());return out;
    }
}
