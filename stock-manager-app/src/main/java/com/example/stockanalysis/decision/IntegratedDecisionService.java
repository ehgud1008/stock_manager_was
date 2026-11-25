package com.example.stockanalysis.decision;

import com.example.stockanalysis.holdings.*;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class IntegratedDecisionService {
    public record Request(Integer horizonTradingDays,String researchJobId) {}
    private final DecisionStore store;private final DecisionSettings settings;private final DecisionJson json;
    private final HoldingService holdings;private final Clock clock;private final DecisionValidityService validity;private final AccountRiskService accountRisk;
    public IntegratedDecisionService(DecisionStore store,DecisionSettings settings,DecisionJson json,HoldingService holdings,Clock clock,DecisionValidityService validity,AccountRiskService accountRisk) {
        this.store=store;this.settings=settings;this.json=json;this.holdings=holdings;this.clock=clock;this.validity=validity;this.accountRisk=accountRisk;
    }
    public DecisionModels.Job create(String snapshotId,Request request,String key,String parent) {
        if(!settings.enabled())throw new BusinessException(ErrorCode.DECISION_DISABLED);
        if(key==null || !key.matches("[A-Za-z0-9._:-]{1,100}"))throw invalid();
        int days=request==null || request.horizonTradingDays()==null?20:request.horizonTradingDays();
        String researchId=request==null?null:request.researchJobId();
        if(days<1 || days>252 || researchId!=null && !researchId.matches("[A-Za-z0-9-]{1,36}"))throw invalid();
        String operation=parent==null?"integrated-decision":"integrated-revision";
        String hash=json.hash(Arrays.asList(snapshotId,days,researchId,parent));
        var prior=store.prior(settings.ownerScope(),operation,key,hash);if(prior.isPresent())return prior.get();
        store.ensureIntegratedSchema();
        var snapshot=find(snapshotId);
        if(!"SNAPSHOT".equals(snapshot.kind()) || !"SUCCEEDED".equals(snapshot.status()))throw conflict("완료된 저장 분석이 필요합니다.");
        DecisionModels.Job research=null;
        if(researchId!=null) {
            research=find(researchId);
            if(!validResearchLink(research,snapshot))throw conflict("동일 종목·저장 분석에 연결된 완료 뉴스 리서치만 사용할 수 있습니다.");
        } else research=store.latestInsightsForSnapshot(settings.ownerScope(),snapshot.stockCode(),snapshot.id()).orElse(null);
        var data=json.convert(snapshot.result().path("analysis"),UnifiedAnalysisResponse.class);
        var policy=new DecisionContextBuilder(json).build(snapshot,data,holdings.single(snapshot.stockCode()),research,days,clock.instant());
        if(researchId!=null && !"VALID".equals(policy.path("research").path("status").asText()))
            throw conflict("지정한 리서치가 만료되었거나 근거가 부족합니다. 뉴스·업황을 새로 분석하세요.");
        policy.set("validity",validity.freeze(policy));
        // Server-only financial input; GeminiReviewAdapter excludes this field from provider requests.
        policy.set("accountRisk",json.tree(accountRisk.get()));
        ObjectNode input=(ObjectNode)json.tree(Map.of());input.put("mode","INTEGRATED_DECISION");
        input.set("context",json.tree(new Request(days,researchId)));input.set("policy",policy);
        input.put("evaluationRevision",parent==null?"INITIAL":parent+":"+key);
        input.put("prompt",IntegratedDecisionContract.PROMPT);input.set("schema",new IntegratedDecisionContract(json).schema());
        input.set("versions",json.tree(Map.of("policy",DecisionContextBuilder.VERSION,"contract",IntegratedDecisionContract.VERSION,
                "provider",GeminiReviewAdapter.PROVIDER,"model",settings.model(),"maxOutputTokens",settings.maxOutputTokens(),
                "maxInputBytes",settings.maxInputBytes(),"thinkingLevel",settings.thinkingLevel(),"engine",DecisionService.ENGINE_VERSION,"sizing",PositionSizingPolicy.VERSION)));
        return store.submit(settings.ownerScope(),operation,key,hash,json.hash(Map.of("owner",settings.ownerScope(),"input",input)),
                "DECISION",snapshot.stockCode(),snapshot.id(),parent,input,clock.millis(),LocalDate.ofInstant(clock.instant(),DecisionService.SEOUL).toString(),settings.dailyRequests());
    }
    public com.fasterxml.jackson.databind.JsonNode currentValidity(String id) {
        if(!settings.enabled())throw new BusinessException(ErrorCode.DECISION_DISABLED);
        var job=find(id);
        if(!"INTEGRATED_DECISION".equals(job.input().path("mode").asText()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"통합 판단의 유효 상태만 조회할 수 있습니다.");
        return validity.current(job.input().path("policy"),clock.instant());
    }
    private boolean validResearchLink(DecisionModels.Job r,DecisionModels.Job s) {
        return "DECISION".equals(r.kind()) && "SUCCEEDED".equals(r.status()) && s.id().equals(r.snapshotId())
                && s.stockCode().equals(r.stockCode()) && "NEWS_INSIGHTS".equals(r.result().path("generationMode").asText())
                && s.resultHash().equals(r.input().path("snapshotHash").asText());
    }
    private DecisionModels.Job find(String id){return store.find(settings.ownerScope(),id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
    private BusinessException conflict(String message){return new BusinessException(ErrorCode.DECISION_CONFLICT,message);}
}
