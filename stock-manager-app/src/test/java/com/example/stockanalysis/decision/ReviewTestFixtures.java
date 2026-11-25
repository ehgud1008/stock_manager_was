package com.example.stockanalysis.decision;
import static com.example.stockanalysis.decision.DecisionModels.*;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
final class ReviewTestFixtures {
    static Independent independent() {return new Independent("CONDITIONAL","PARTIAL",explain("TREND"),List.of(),"가격 전략을 확정할 추가 근거가 필요합니다.");}
    static Detail detail() {return new Detail(explain("TREND"),explain("HORIZON"),explain("PLAN"),explain("PRICE_STRUCTURE"),explain("TIME_RULE"));}
    static Explanation explain(String id) {return new Explanation("확인된 조건을 검토합니다. 조건이 바뀌면 기존 판단을 다시 평가합니다.",List.of(id),List.of());}
    static JsonNode market() {return new ObjectMapper().valueToTree(Map.of("recommendedStyle","SWING","requiredMonitoring","DAILY","holdingPeriod",Map.of("minTradingDays",6,"maxTradingDays",20,"reason","추세 지속을 관찰합니다."),
            "fragility",fragility(),"styles",List.of("SHORT_TERM","SWING","MEDIUM_TERM","LONG_TERM").stream().map(s->Map.of("style",s,"status",s.equals("LONG_TERM")?"INSUFFICIENT":"CONDITIONAL","reason","자료의 추세를 확인합니다.","evidenceIds",List.of("TREND"))).toList()));}
    static Map<String,String> fragility() {return Map.of("strongestReason","추세를 확인합니다.","strongestCounter","반대 신호가 있습니다.","decisiveUnknown","새 공시 확인이 필요합니다.","avoidIf","지지가 깨지면 피합니다.");}
    static JsonNode personal() {return personal("WAIT");}
    static JsonNode personal(String action) {
        var next=new java.util.LinkedHashMap<String,Object>();next.put("action","REVIEW");next.put("instruction","조건을 다시 확인합니다.");next.put("transitionId",null);
        return new ObjectMapper().valueToTree(Map.ofEntries(Map.entry("fit","CONDITIONAL"),Map.entry("reason","사용자 조건을 반영합니다."),Map.entry("evidenceIds",List.of("TREND")),Map.entry("selectedStyle","SWING"),
                Map.entry("holdingPeriod",Map.of("minTradingDays",10,"maxTradingDays",10,"reason","추세를 관찰할 기간입니다.","earlyExit","지지가 깨지면 재검토합니다.","expiryAction","기간 경과 시 신호를 확인합니다.")),
                Map.entry("requiredMonitoring","DAILY"),Map.entry("fragility",fragility()),Map.entry("nextAction",next),
                Map.entry("perspectives",List.of("AGGRESSIVE","BALANCED","CONSERVATIVE").stream().map(p->Map.of("profile",p,"entryRule","진입 조건을 확인합니다.","exitRule","추세 훼손을 확인합니다.","tradeoff","기회와 위험을 비교합니다.")).toList()),
                Map.entry("transitions",List.of(transition("T1",action,"UNDETERMINED"),transition("T2","UNDETERMINED",action)))));
    }
    static Map<String,Object> transition(String id,String from,String to) {var m=new java.util.LinkedHashMap<String,Object>();m.put("id",id);m.put("fromAction",from);m.put("toAction",to);m.put("trigger","신호가 바뀌는 경우");m.put("invalidation","새로운 정보가 확인되는 경우");m.put("assessmentMode","AI_REVIEW");m.put("priceCondition",null);return m;}
}
