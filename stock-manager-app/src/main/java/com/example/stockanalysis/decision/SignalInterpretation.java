package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Grounded interpretation of the existing engine; never a second price model or order instruction. */
final class SignalInterpretation {
    static final String PROMPT="""
        signalInterpretation은 기존 매매 신호에 대한 종합 해석이다. 별도 매수·매도 명령이나 개인별 비중을 만들지 않는다.
        stance는 SUPPORTED(외부 근거가 엔진 신호를 뒷받침), CHECK_REQUIRED(추가 확인 필요), RECONSIDER(엔진 신호 재검토 필요), INSUFFICIENT(자료 부족으로 해석 유보) 중 하나다. 엔진이 대기라면 SUPPORTED도 대기를 뒷받침한다는 뜻이지 매수가 아니다.
        reason에는 엔진 신호와 외부 근거의 연결을 설명하고 sourceIds와 engineEvidenceIds를 참조한다. 근거 없는 중립·긍정 결론을 만들지 않는다.
        SUPPORTED/RECONSIDER는 해당 방향 SUPPORTS/WEAKENS 사건의 핵심 인용이 검색 기간 내 공식 원문일 때만 가능하다. 검색 요약은 CHECK_REQUIRED 또는 INSUFFICIENT, 제목뿐이면 INSUFFICIENT다. 공통 배경 자료만으로 확정하지 않는다.
        WEAKENS 사건은 conflicts에 빠짐없이 반영한다. conflicts는 엔진과 외부 근거의 충돌 및 중요한 이유를 설명하고 해당 핵심 인용 출처와 엔진 ID를 참조한다. 반대 사건이 있으면 SUPPORTED로 단정하지 않는다.
        nonHolderChecks는 미보유자의 진입 검토 전 확인 사항, holderChecks는 보유자의 유지 판단 재검토 사항, invalidationConditions는 이번 해석을 적용할 수 없게 되는 조건이다. 각 항목은 text/sourceIds/engineEvidenceIds를 갖고 적어도 하나의 근거를 연결한다. INSUFFICIENT일 때만 근거 없는 자료 확보 요청을 허용한다.
        매수단가·보유량·손실 허용 수준은 입력에 없으므로 개인별 매도 비중·실행 지시를 추정하지 않는다. 조건을 이미 충족한 사실처럼 쓰지 않는다.
        signalContext는 서버가 계산한 시점 정보다. 엔진 날짜 미확인·미래 날짜·수집 시점보다 칠 달력일 초과 과거이면 SUPPORTED/RECONSIDER를 금지하고 엔진 재분석 필요를 missingInformation에 설명한다. 칠 일 이내도 최신 거래일이라는 보장은 없다.
        엔진 QUALITY가 COMPLETE가 아니면 SUPPORTED/RECONSIDER를 금지하고 자료 품질 한계를 missingInformation에 쓴다. 엔진 전략이나 유효 신호가 없으면 INSUFFICIENT다.
        signalInterpretation의 설명 문장에는 숫자·금액·비율·가격 수준을 새로 쓰지 말고 '엔진 진입 구간', '엔진 손절 기준'처럼 참조한다. 실제 가격은 서버의 저장 엔진 값으로 화면에 별도 표시한다. 승률·목표가·손절가·포지션 비중을 생성하지 않는다.
        missingInformation에는 해석에 부족한 자료와 수집 범위 한계를 쓴다. 이전 signalInterpretation이 있으면 previousResearch의 과거 해석과 달라진 이유를 reason에 설명하되 현재 사실의 출처로 사용하지 않는다.
        """;
    private static Map<String,Object> text(){return Map.of("type","string","maxLength",900,"description","숫자·금액·비율 없이 근거와 조건을 설명한다.");}
    private static Map<String,Object> refs(int max){return array(Map.of("type","string"),0,max);}
    private static Map<String,Object> array(Object item,int min,int max){return Map.of("type","array","items",item,"minItems",min,"maxItems",max);}
    private static Map<String,Object> object(Map<String,Object> fields){return Map.of("type","object","properties",fields,"required",new ArrayList<>(fields.keySet()),"additionalProperties",false);}
    static Map<String,Object> schema(){
        var condition=object(Map.of("text",text(),"sourceIds",refs(8),"engineEvidenceIds",refs(3)));
        return object(Map.of("stance",Map.of("type","string","enum",List.of("SUPPORTED","CHECK_REQUIRED","RECONSIDER","INSUFFICIENT")),
                "reason",text(),"sourceIds",refs(12),"engineEvidenceIds",refs(3),"conflicts",array(condition,0,8),
                "nonHolderChecks",array(condition,1,3),"holderChecks",array(condition,1,3),"invalidationConditions",array(condition,1,3),"missingInformation",array(text(),0,6)));
    }
    static JsonNode context(DecisionJson json,JsonNode engine,JsonNode evidence){
        var result=(com.fasterxml.jackson.databind.node.ObjectNode)json.tree(Map.of());
        result.put("enginePriceDate",engine.path("priceDate").asText("미확인"));result.put("collectedAt",evidence.path("collectedAt").asText("미확인"));
        result.put("status","UNKNOWN");result.putNull("calendarDayGap");result.put("maxCalendarDayGap",7);
        try {
            LocalDate price=LocalDate.parse(engine.path("priceDate").asText());
            LocalDate collected=Instant.parse(evidence.path("collectedAt").asText()).atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
            long gap=ChronoUnit.DAYS.between(price,collected);result.put("calendarDayGap",gap);
            result.put("status",gap<0?"FUTURE_ENGINE":gap>7?"REFRESH_REQUIRED":"WITHIN_WINDOW");
        } catch(RuntimeException ignored) { /* missing/invalid dates must not imply freshness */ }
        return result;
    }
    static void validate(JsonNode report,JsonNode input,DecisionJson json){
        var signal=report.path("signalInterpretation");var policy=input.path("policy");var engine=policy.path("engine").path("evidence");
        Map<String,JsonNode> sources=new HashMap<>();policy.path("sources").forEach(s->sources.put(s.path("id").asText(),s));
        String stance=signal.path("stance").asText();boolean insufficient=stance.equals("INSUFFICIENT");
        refs(signal,sources,engine);
        for(String group:List.of("conflicts","nonHolderChecks","holderChecks","invalidationConditions")) for(var condition:signal.path(group)) {
            refs(condition,sources,engine);
            if(!insufficient && condition.path("sourceIds").isEmpty() && condition.path("engineEvidenceIds").isEmpty()) fail();
        }
        boolean detailed=false,matchingOfficial=false,weakens=false;
        for(var event:report.path("events")) {
            var source=sources.get(event.path("quoteSourceId").asText());if(source==null) continue;
            boolean direct=!source.path("contextOutsideWindow").asBoolean();
            if(direct && (NewsResearch.full(source) || "SEARCH_SNIPPET".equals(source.path("depth").asText()))) detailed=true;
            String relation=event.path("engineRelation").asText();
            if(relation.equals("WEAKENS")) {
                weakens=true;boolean explained=false;
                for(var conflict:signal.path("conflicts")) if(has(conflict.path("sourceIds"),event.path("quoteSourceId").asText()) && overlaps(conflict.path("engineEvidenceIds"),event.path("engineEvidenceIds"))) explained=true;
                if(!explained) fail();
            }
            if(direct && "OFFICIAL_DOCUMENT".equals(source.path("depth").asText()) && relation.equals(stance.equals("SUPPORTED")?"SUPPORTS":"WEAKENS")
                    && has(signal.path("sourceIds"),event.path("quoteSourceId").asText()) && overlaps(signal.path("engineEvidenceIds"),event.path("engineEvidenceIds"))) matchingOfficial=true;
        }
        boolean engineAvailable=Set.of("BUY_CANDIDATE","WATCH","WAIT","AVOID").contains(engine.path("ANALYSIS").path("strategy").path("action").asText());
        for(var row:engine.path("ANALYSIS").path("signalReport").path("signals")) if(Set.of("ACTIVE","CONFIRMED").contains(row.path("state").asText())) engineAvailable=true;
        if((!detailed || !engineAvailable) && !insufficient) fail();
        if(!insufficient && (signal.path("sourceIds").isEmpty() || signal.path("engineEvidenceIds").isEmpty())) fail();
        var timing=context(json,policy.path("engine"),policy);
        boolean stale=!"WITHIN_WINDOW".equals(timing.path("status").asText());
        boolean qualityLimited=!"COMPLETE".equals(engine.path("QUALITY").asText());
        if(Set.of("SUPPORTED","RECONSIDER").contains(stance) && (!matchingOfficial || stale || qualityLimited)) fail();
        if(stance.equals("SUPPORTED") && weakens) fail();
        if((insufficient || stale || qualityLimited) && signal.path("missingInformation").isEmpty()) fail();
        var previous=policy.path("previousResearch");var old=previous.path("signalInterpretation");
        if(old.isObject() && !old.path("stance").equals(signal.path("stance")) && Set.of("SUPPORTED","RECONSIDER").contains(stance) && previous.path("engineHash").asText().equals(json.hash(policy.path("engine")))) {
            Map<String,String> before=new HashMap<>(),after=new HashMap<>();
            for(var doc:previous.path("signalDocuments")) before.put(doc.path("url").asText(),doc.path("contentHash").asText());
            for(var ref:signal.path("sourceIds")) {var source=sources.get(ref.asText());after.put(source.path("url").asText(),source.path("contentHash").asText());}
            if(!before.isEmpty() && before.equals(after)) fail();
        }
    }
    private static void refs(JsonNode node,Map<String,JsonNode> sources,JsonNode engine){
        Set<String> seen=new HashSet<>();for(var ref:node.path("sourceIds")) if(!sources.containsKey(ref.asText()) || !seen.add(ref.asText())) fail();
        SuitabilityContract.refs(node.path("engineEvidenceIds"),engine);
    }
    private static boolean has(JsonNode refs,String value){for(var ref:refs) if(ref.asText().equals(value)) return true;return false;}
    private static boolean overlaps(JsonNode a,JsonNode b){for(var ref:a) if(has(b,ref.asText())) return true;return false;}
    private static void fail(){throw new DecisionModels.AiFailure("AI_INVALID_RESPONSE",false);}
    static Map<String,Object> unavailable(){
        var check=Map.of("text","원문 근거와 최신 엔진 신호를 확보한 후 조건을 다시 확인하세요.","sourceIds",List.of(),"engineEvidenceIds",List.of());
        return Map.of("stance","INSUFFICIENT","reason","외부 자료를 확보하지 못해 엔진 신호에 대한 종합 해석을 유보합니다.","sourceIds",List.of(),"engineEvidenceIds",List.of(),"conflicts",List.of(),
                "nonHolderChecks",List.of(check),"holderChecks",List.of(check),"invalidationConditions",List.of(check),"missingInformation",List.of("외부 근거가 미확보 상태입니다. 자료 부재는 중립이나 긍정의 근거가 아닙니다."));
    }
}
