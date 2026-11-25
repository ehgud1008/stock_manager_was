package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisResponse;
import com.example.stockanalysis.unified.internal.DefaultUnifiedStockAnalysisEngine;
import com.example.stockanalysis.engine.factor.internal.SwingSuitabilityEngine;
import com.example.stockanalysis.engine.signal.SwingSignalEngine;
import com.example.stockanalysis.engine.screener.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DecisionService {
    static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    public static final String ENGINE_VERSION=String.join("/",DefaultUnifiedStockAnalysisEngine.VERSION,
            SwingSuitabilityEngine.VERSION,SwingSignalEngine.VERSION,DefaultScreenerEngine.VERSION,ScreenerConfig.defaults().version());
    private final DecisionStore store; private final DecisionSettings settings; private final DecisionJson json;
    private final DecisionPolicy policy; private final AiReviewContract contract; private final Clock clock;
    public DecisionService(DecisionStore store,DecisionSettings settings,DecisionJson json,DecisionPolicy policy,AiReviewContract contract,Clock clock) {
        this.store=store;this.settings=settings;this.json=json;this.policy=policy;this.contract=contract;this.clock=clock;
    }
    private com.example.stockanalysis.holdings.HoldingService holdings;
    private IntegratedDecisionService integrated;
    @org.springframework.beans.factory.annotation.Autowired
    public void integrated(IntegratedDecisionService integrated){this.integrated=integrated;}
    @org.springframework.beans.factory.annotation.Autowired
    public void setHoldings(com.example.stockanalysis.holdings.HoldingService holdings){this.holdings=holdings;}
    public Map<String,Object> capabilities() { return Map.of("enabled",settings.enabled(),"aiConfigured",settings.aiConfigured(),"scopeMode","SINGLE_OWNER","policyVersion",DecisionPolicy.VERSION,"integratedContractVersion",IntegratedDecisionContract.VERSION); }
    public View snapshot(String code,SnapshotRequest request,String key) {
        enabled(); validateKey(key); validateCode(code);
        if(request==null || request.source()==null || !Set.of("COLLECT","UNIFIED_RUN_ITEM").contains(request.source())
                || ("COLLECT".equals(request.source()) && request.runId()!=null)
                || ("UNIFIED_RUN_ITEM".equals(request.source()) && (request.runId()==null || !request.runId().matches("[A-Za-z0-9-]{1,36}")))) throw invalid();
        String hash=json.hash(Map.of("code",code,"request",request));
        var prior=store.prior(owner(),"snapshot",key,hash); if(prior.isPresent()) return view(prior.get());
        ObjectNode input=(ObjectNode)json.tree(request);
        if(holdings!=null && "COLLECT".equals(request.source())) input.set("holdingSnapshot",json.tree(holdings.single(code)));
        input.put("asOf",LocalDateTime.ofInstant(clock.instant(),SEOUL).toString()); input.put("engineVersion",ENGINE_VERSION);
        String content=json.hash(Map.of("owner",owner(),"operation","snapshot","key",key));
        if("UNIFIED_RUN_ITEM".equals(request.source())) {
            var imported=store.importedSnapshot(owner(),code,request.runId());
            content=imported.filter(j->!"FAILED".equals(j.status())).map(store::contentKey).orElseGet(()->
                    json.hash(Map.of("owner",owner(),"sourceRunId",request.runId(),"code",code,
                            "retryOf",imported.map(Job::id).orElse("INITIAL"))));
        }
        return view(store.submit(owner(),"snapshot",key,hash,content,"SNAPSHOT",code,null,null,input,clock.millis(),day(),settings.dailyRequests()));
    }
    public View decision(String snapshotId,ReviewRequest request,String key) { return createDecision(snapshotId,request,key,null); }
    public record InsightRequest(Integer lookbackDays,String sectorQuery) {}
    public View insights(String snapshotId,InsightRequest request,String key) {
        enabled();validateKey(key);
        int days=request==null || request.lookbackDays()==null?14:request.lookbackDays();
        String sector=request==null || request.sectorQuery()==null?"":request.sectorQuery().strip();
        if(days<1 || days>30 || sector.length()>50 || !sector.matches("[\\p{L}\\p{N} &·/()\\-]*")) throw invalid();
        var context=Map.of("lookbackDays",days,"sectorQuery",sector);
        String hash=json.hash(Map.of("snapshotId",snapshotId,"context",context));
        var prior=store.prior(owner(),"news-insights",key,hash);if(prior.isPresent()) return view(prior.get());
        store.ensureResearchSchema();
        Job snapshot=getJob(snapshotId,"SNAPSHOT");if(!"SUCCEEDED".equals(snapshot.status())) throw new BusinessException(ErrorCode.DECISION_CONFLICT,"저장 분석이 완료된 후 요청하세요.");
        var data=json.convert(snapshot.result().path("analysis"),UnifiedAnalysisResponse.class);
        var contract=new NewsInsightContract(json);ObjectNode input=(ObjectNode)json.tree(Map.of());
        input.put("mode","NEWS_INSIGHTS");input.set("context",json.tree(context));input.put("snapshotHash",snapshot.resultHash());
        input.put("researchOwner",owner());
        input.set("stock",json.tree(data.result().stock()));input.put("priceDate",data.result().priceDate().toString());
        // Public engine context only: no holdings, cost basis, preferences, or invented trade opinion.
        input.set("engine",json.tree(Map.of("priceDate",data.result().priceDate(),"asOf",data.result().asOf(),"evidence",Map.of(
                "QUALITY",data.collectionQuality(),"ANALYSIS",data.result().analysis(),"SCREENER",data.result().screener()))));
        LocalDate now=LocalDate.ofInstant(clock.instant(),SEOUL);input.put("windowStart",now.minusDays(days-1).toString());input.put("windowEnd",now.toString());
        input.put("requestedAt",clock.instant().toString());input.put("requestRevision",key);
        input.put("prompt",NewsInsightContract.PROMPT);input.set("schema",contract.schema());
        input.set("versions",json.tree(Map.of("contract",NewsInsightContract.VERSION,"provider",GeminiReviewAdapter.PROVIDER,"model",settings.model(),"maxOutputTokens",settings.maxOutputTokens(),"maxInputBytes",settings.maxInputBytes(),"thinkingLevel",settings.thinkingLevel())));
        store.latestInsights(owner(),snapshot.stockCode()).ifPresent(previous->{
            input.put("previousId",previous.id());var sources=input.putArray("previousSources");
            for(var source:previous.result().path("evidence").path("sources")) {var item=sources.addObject();for(String field:List.of("id","title","url","dedupKey","contentHash")) if(source.has(field)) item.set(field,source.path(field));}
            input.set("previousResearch",new NewsResearch(json).previous(previous.result()));
        });
        return view(store.submit(owner(),"news-insights",key,hash,json.hash(Map.of("owner",owner(),"input",input)),"DECISION",snapshot.stockCode(),snapshotId,null,input,clock.millis(),day(),settings.dailyRequests()));
    }
    public View revision(String parentId,String key) {
        enabled(); validateKey(key); Job parent=getJob(parentId,"DECISION");
        if("NEWS_INSIGHTS".equals(parent.input().path("mode").asText())) throw new BusinessException(ErrorCode.INVALID_REQUEST,"뉴스·업황은 최신 자료 수집으로 새로 요청하세요.");
        if(!Set.of("SUCCEEDED","FAILED").contains(parent.status())) throw new BusinessException(ErrorCode.DECISION_CONFLICT,"진행 중인 판단의 재평가를 만들 수 없습니다.");
        if("INTEGRATED_DECISION".equals(parent.input().path("mode").asText()))
            return view(integrated.create(parent.snapshotId(),json.convert(parent.input().path("context"),IntegratedDecisionService.Request.class),key,parentId));
        return createDecision(parent.snapshotId(),json.convert(parent.input().path("context"),ReviewRequest.class),key,parentId);
    }
    private View createDecision(String snapshotId,ReviewRequest request,String key,String parentId) {
        enabled();validateKey(key);
        validateRequest(request);
        String operation=parentId==null?"decision":"revision";
        var identity=new LinkedHashMap<String,Object>();identity.put("positionStatus",request.positionStatus());identity.put("horizonTradingDays",request.horizonTradingDays());
        if(request.averageBuyPrice()!=null) identity.put("averageBuyPrice",request.averageBuyPrice());
        if(request.quantity()!=null) identity.put("quantity",request.quantity());
        if(request.balanceDate()!=null) identity.put("balanceDate",request.balanceDate());
        if(request.purchasedOn()!=null) identity.put("purchasedOn",request.purchasedOn());
        if(request.preferences()!=null) identity.put("preferences",request.preferences());
        String hash=json.hash(Arrays.asList(snapshotId,identity,parentId));
        var prior=store.prior(owner(),operation,key,hash); if(prior.isPresent()) return view(prior.get());
        Job snapshot=getJob(snapshotId,"SNAPSHOT");
        if(!"SUCCEEDED".equals(snapshot.status())) throw new BusinessException(ErrorCode.DECISION_CONFLICT,"저장 분석이 완료된 후 AI 검토를 요청하세요.");
        var data=json.convert(snapshot.result().path("analysis"),UnifiedAnalysisResponse.class);
        validateContext(request,data);
        var normalized=new ReviewRequest(request.positionStatus(),request.horizonTradingDays(),request.averageBuyPrice(),request.purchasedOn(),request.effectivePreferences(),request.quantity(),request.balanceDate());
        ObjectNode input=(ObjectNode)json.tree(new LinkedHashMap<>());
        input.set("context",json.tree(normalized)); input.set("policy",policy.prepare(data,normalized));
        input.put("snapshotHash",snapshot.resultHash());
        input.put("evaluationRevision",parentId==null?"INITIAL":parentId+":"+key);
        input.set("versions",json.tree(Map.of("policy",DecisionPolicy.VERSION,"contract",AiReviewContract.VERSION,
                "horizon",HorizonPlanCalculator.VERSION,"canonical","canonical-v1","provider",GeminiReviewAdapter.PROVIDER,"model",settings.model(),"maxOutputTokens",settings.maxOutputTokens(),"maxInputBytes",settings.maxInputBytes(),"thinkingLevel",settings.thinkingLevel())));
        input.put("prompt",AiReviewContract.PROMPT); input.set("schema",contract.schema());
        input.put("marketPrompt",SuitabilityContract.MARKET_PROMPT);input.set("marketSchema",new SuitabilityContract(json).marketSchema());
        input.set("marketPolicy",SuitabilityContract.marketInput(input.path("policy")));
        String content=json.hash(Map.of("owner",owner(),"input",input));
        return view(store.submit(owner(),operation,key,hash,content,"DECISION",snapshot.stockCode(),snapshotId,parentId,input,clock.millis(),day(),settings.dailyRequests()));
    }
    public View get(String id,String kind) { enabled(); return view(getJob(id,kind)); }
    public View importedSnapshot(String code,String sourceRunId) {
        enabled();validateCode(code);
        if(sourceRunId==null || !sourceRunId.matches("[A-Za-z0-9-]{1,36}")) throw invalid();
        return store.importedSnapshot(owner(),code,sourceRunId).map(this::view).orElse(null);
    }
    public History history(String code,int page,int size) {
        enabled();validateCode(code); if(page<0 || page>10000 || size<1 || size>50) throw invalid(); return store.history(owner(),code,page,size);
    }
    public History history(String code,int page,int size,String snapshotId) {
        if(snapshotId==null) return history(code,page,size);
        enabled();validateCode(code);if(page<0 || page>10000 || size<1 || size>50) throw invalid();
        if(!code.equals(getJob(snapshotId,"SNAPSHOT").stockCode())) throw invalid();
        return store.history(owner(),code,page,size,snapshotId);
    }
    public View retry(String id,String key) {
        enabled();validateKey(key); Job job=getJob(id,null);
        return view(store.retryRequest(job,key,clock.millis()));
    }
    private Job getJob(String id,String kind) {
        Job job=store.find(owner(),id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(kind!=null && !kind.equals(job.kind())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); return job;
    }
    private View view(Job j) {
        JsonNode result=j.result();
        // Collection provenance stays server-side. AI inputs and provider raw responses are not public API data.
        if("SNAPSHOT".equals(j.kind()) && result!=null) result=json.tree(Map.of("analysis",result.path("analysis"),"origin",result.path("origin")));
        if(result!=null && "NEWS_INSIGHTS".equals(result.path("generationMode").asText())) {result=result.deepCopy();for(var source:result.path("evidence").path("sources")) if(source instanceof ObjectNode object) object.remove("body");}
        JsonNode context=j.input().path("context");
        if("INTEGRATED_DECISION".equals(j.input().path("mode").asText())) {
            ObjectNode visible=context.deepCopy();var p=j.input().path("policy");
            visible.set("engineAction",p.path("engineAction"));visible.set("ruleAction",p.path("holding").path("action"));visible.set("priceDate",p.path("priceDate"));context=visible;
        }
        return new View(j.id(),j.kind(),j.stockCode(),j.snapshotId(),j.parentId(),j.status(),context,result,j.action(),j.createdAt(),j.completedAt(),j.attempt(),j.errorCode(),j.retryable(),j.input().path("mode").asText("SNAPSHOT".equals(j.kind())?"SNAPSHOT":"LEGACY_DECISION"),j.input().path("versions").path("contract").asText(null));
    }
    private String owner() { return settings.ownerScope(); }
    static void validateContext(ReviewRequest request,UnifiedAnalysisResponse data) {
        validateRequest(request);
        if((request.positionStatus()!=Position.HELD && (request.averageBuyPrice()!=null || request.purchasedOn()!=null || request.quantity()!=null || request.balanceDate()!=null))
                || (request.averageBuyPrice()!=null && (request.averageBuyPrice().signum()<=0 || request.averageBuyPrice().scale()>2 || request.averageBuyPrice().precision()-request.averageBuyPrice().scale()>12))
                || (request.balanceDate()!=null && request.balanceDate().isAfter(data.result().priceDate()))
                || (request.purchasedOn()!=null && request.purchasedOn().isAfter(data.result().priceDate())))
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"보유 조건을 확인하세요. 매수일은 저장 분석 가격일 이후일 수 없습니다.");
    }
    static void validateRequest(ReviewRequest request) {
        if(request==null || request.positionStatus()==null || request.horizonTradingDays()!=null && (request.horizonTradingDays()<1 || request.horizonTradingDays()>252)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if(request.quantity()!=null && (request.quantity().signum()<=0 || request.quantity().stripTrailingZeros().scale()>0 || request.quantity().compareTo(new java.math.BigDecimal("9999999999"))>0 || request.averageBuyPrice()==null)) throw invalidHolding();
        var p=request.effectivePreferences();
        if(p.aggressive()<0 || p.aggressive()>100 || p.balanced()<0 || p.balanced()>100 || p.conservative()<0 || p.conservative()>100
                || p.aggressive()+p.balanced()+p.conservative()!=100
                || !java.util.Arrays.asList("INTRADAY","DAILY","OCCASIONAL").contains(p.monitoring())
                || !java.util.Arrays.asList("EARLY","CONFIRMED").contains(p.entryPreference())
                || !java.util.Arrays.asList("LOW","MODERATE","HIGH").contains(p.volatilityTolerance())
                || !java.util.Arrays.asList("QUICK","STRUCTURAL").contains(p.lossResponse()))
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"성향 비율 합계는 백분율 전체여야 하며 행동 선호를 확인해야 합니다.");
    }
    private static BusinessException invalidHolding(){return new BusinessException(ErrorCode.INVALID_REQUEST,"보유수량과 평균단가를 확인하세요.");}
    private String day() { return LocalDate.ofInstant(clock.instant(),SEOUL).toString(); }
    private void enabled() { if(!settings.enabled()) throw new BusinessException(ErrorCode.DECISION_DISABLED); }
    private void validateCode(String code) { if(code==null || !code.matches("[0-9]{6}")) throw invalid(); }
    private void validateKey(String key) { if(key==null || !key.matches("[A-Za-z0-9._:-]{1,100}")) throw invalid(); }
    private BusinessException invalid() { return new BusinessException(ErrorCode.INVALID_REQUEST); }
}
