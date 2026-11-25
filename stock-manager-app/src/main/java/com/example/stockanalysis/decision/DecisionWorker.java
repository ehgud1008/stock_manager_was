package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.DecisionModels.*;
import com.example.stockanalysis.shared.exception.*;
import com.example.stockanalysis.unified.application.UnifiedAnalysisService;
import com.example.stockanalysis.unified.scan.UnifiedScanStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="trade-decision",name="enabled",havingValue="true")
public class DecisionWorker implements AutoCloseable {
    private static final long LEASE=180_000;
    private final DecisionStore store; private final DecisionSettings settings; private final DecisionJson json;
    private final UnifiedAnalysisService analyzer; private final UnifiedScanStore scans; private final AiReviewPort ai;
    private final AiReviewContract contract; private final DecisionPolicy policy; private final Clock clock;
    private NewsEvidenceCollector newsCollector;
    private DecisionOrchestrator orchestrator;
    @org.springframework.beans.factory.annotation.Autowired
    public void orchestrator(DecisionOrchestrator orchestrator) {this.orchestrator=orchestrator;}
    @org.springframework.beans.factory.annotation.Autowired
    public void newsCollector(NewsEvidenceCollector collector) {this.newsCollector=collector;}
    private final String worker=UUID.randomUUID().toString(); private final AtomicReference<Job> active=new AtomicReference<>();
    private final ScheduledExecutorService queue=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"decision-worker");t.setDaemon(true);return t;});
    private final ScheduledExecutorService heartbeat=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"decision-lease");t.setDaemon(true);return t;});
    public DecisionWorker(DecisionStore store,DecisionSettings settings,DecisionJson json,UnifiedAnalysisService analyzer,UnifiedScanStore scans,
                          AiReviewPort ai,AiReviewContract contract,DecisionPolicy policy,Clock clock) {
        this.store=store;this.settings=settings;this.json=json;this.analyzer=analyzer;this.scans=scans;this.ai=ai;this.contract=contract;this.policy=policy;this.clock=clock;
    }
    @jakarta.annotation.PostConstruct public void start() {
        queue.scheduleWithFixedDelay(this::tick,5,2,TimeUnit.SECONDS);
        heartbeat.scheduleWithFixedDelay(()->{ Job j=active.get(); if(j!=null) try { store.heartbeat(j,clock.millis(),LEASE); } catch(RuntimeException ignored) { /* fencing prevents stale commits */ } },20,20,TimeUnit.SECONDS);
    }
    public void tick() {
        try {
            var claimed=store.claim(settings.ownerScope(),worker,clock.millis(),LEASE);
            if(claimed.isEmpty()) return;
            Job job=claimed.get(); active.set(job);
            try {
                JsonNode result="SNAPSHOT".equals(job.kind())?snapshot(job):review(job);
                store.complete(job,result,clock.millis());
            } catch(AiFailure e) {
                if("INTEGRATED_DECISION".equals(job.input().path("mode").asText()) && job.attempt()>=3 && !"LEASE_LOST".equals(e.code()))
                    store.complete(job,new DecisionArbitrator(json).finish(job.input(),null,e.code(),clock.millis()),clock.millis());
                else store.fail(job,e.code(),e.retryable(),clock.millis(),Math.max(e.delayMillis(),(long)Math.pow(2,job.attempt())*2000+ThreadLocalRandom.current().nextInt(1000)));
            } catch(BusinessException e) {
                store.fail(job,e.getErrorCode().name(),e.getErrorCode()==ErrorCode.EXTERNAL_API_ERROR,clock.millis(),10000);
            } catch(RuntimeException e) {
                store.fail(job,"PROCESSING_ERROR",false,clock.millis(),0);
            } finally { active.set(null); }
        } catch(RuntimeException e) {
            // Never log exception messages/SQL parameters containing frozen personal inputs.
            LoggerFactory.getLogger(getClass()).warn("Decision worker could not persist work; type={}",e.getClass().getSimpleName());
        }
    }
    private JsonNode snapshot(Job job) {
        if(!DecisionService.ENGINE_VERSION.equals(job.input().path("engineVersion").asText())) throw new AiFailure("ENGINE_VERSION_CHANGED",false);
        ObjectNode result=(ObjectNode)json.tree(new LinkedHashMap<>());
        if("UNIFIED_RUN_ITEM".equals(job.input().path("source").asText())) {
            String run=job.input().path("runId").asText();
            var saved=scans.detail(run,job.stockCode()).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if(!job.stockCode().equals(saved.result().stock().stockCode())) throw new AiFailure("SOURCE_MISMATCH",false);
            result.set("analysis",json.tree(saved)); result.set("origin",json.tree(Map.of("source","UNIFIED_RUN_ITEM","runId",run,"rawDataAvailable",false)));
        } else {
            var captured=analyzer.captureAt(job.stockCode(),LocalDateTime.parse(job.input().path("asOf").asText()));
            var holdingSnapshot=job.input().has("holdingSnapshot")?json.convert(job.input().path("holdingSnapshot"),com.example.stockanalysis.holdings.HoldingModels.Snapshot.class):null;
            result.set("analysis",json.tree(holdingSnapshot==null?captured.analysis():analyzer.personalize(captured.analysis(),holdingSnapshot)));
            // Retain normalized records and timestamps, not transport responses or credentials.
            result.set("provenance",json.tree(Map.of("input",captured.collected().input(),"sources",captured.collected().sources().stream().map(s->Map.of(
                    "apiId",s.apiId(),"records",s.records(),"requestedAt",s.requestedAt(),"receivedAt",s.receivedAt())).toList())));
            result.set("origin",json.tree(Map.of("source","COLLECT","rawDataAvailable",true)));
        }
        return result;
    }
    private JsonNode review(Job job) {
        if("INTEGRATED_DECISION".equals(job.input().path("mode").asText()))
            return orchestrator.run(job,ai,()->{
                if(!GeminiReviewAdapter.PROVIDER.equals(job.input().path("versions").path("provider").asText()))throw new AiFailure("AI_PROVIDER_CHANGED",false);
                if(!settings.aiConfigured())throw new AiFailure("AI_NOT_CONFIGURED",false);
                reserve(job.input());
            });
        if("NEWS_INSIGHTS".equals(job.input().path("mode").asText())) return newsInsights(job);
        if(!DecisionPolicy.VERSION.equals(job.input().path("versions").path("policy").asText())
                || !AiReviewContract.VERSION.equals(job.input().path("versions").path("contract").asText())) throw new AiFailure("POLICY_VERSION_CHANGED",false);
        if(!job.input().path("policy").path("blockReason").isNull()) return policy.finish(job.input(),null,clock.millis());
        if(!GeminiReviewAdapter.PROVIDER.equals(job.input().path("versions").path("provider").asText())) throw new AiFailure("AI_PROVIDER_CHANGED",false);
        if(settings.apiKey().isBlank() || job.input().path("versions").path("model").asText().isBlank()) throw new AiFailure("AI_NOT_CONFIGURED",false);
        var suitability=new SuitabilityContract(json);
        JsonNode market=store.marketAssessment(job).orElse(null);
        if(market==null) {
            ObjectNode marketRequest=job.input().deepCopy();marketRequest.set("policy",job.input().path("marketPolicy"));marketRequest.set("schema",job.input().path("marketSchema"));marketRequest.set("prompt",job.input().path("marketPrompt"));
            reserve(marketRequest);var assessed=ai.assess(marketRequest);suitability.validateMarket(assessed.assessment(),job.input().path("marketPolicy"));
            if(!store.recordMarket(job,assessed,clock.millis())) throw new AiFailure("LEASE_LOST",false);market=assessed.assessment();
        } else suitability.validateMarket(market,job.input().path("marketPolicy"));
        ObjectNode personal=job.input().deepCopy();((ObjectNode)personal.path("policy")).set("marketAssessment",market);
        reserve(personal);
        var response=ai.review(personal);store.recordPersonalized(job,market,response);contract.validate(response.review(),personal);
        ObjectNode result=(ObjectNode)policy.finish(job.input(),response.review(),clock.millis());result.set("marketAssessment",market);return result;
    }
    private JsonNode newsInsights(Job job) {
        if(!NewsInsightContract.VERSION.equals(job.input().path("versions").path("contract").asText())) throw new AiFailure("POLICY_VERSION_CHANGED",false);
        if(!GeminiReviewAdapter.PROVIDER.equals(job.input().path("versions").path("provider").asText())) throw new AiFailure("AI_PROVIDER_CHANGED",false);
        if(settings.apiKey().isBlank() || job.input().path("versions").path("model").asText().isBlank()) throw new AiFailure("AI_NOT_CONFIGURED",false);
        JsonNode evidence=store.newsEvidence(job).orElse(null);
        if(evidence==null) {
            evidence=new NewsResearch(json).prepare(newsCollector.collect(job.input()),job.stockCode());
            if(!store.recordNews(job,evidence,null,clock.millis())) throw new AiFailure("LEASE_LOST",false);
        }
        var contract=new NewsInsightContract(json);JsonNode report;
        ObjectNode request=job.input().deepCopy();ObjectNode modelInput=request.putObject("policy");
        var research=new NewsResearch(json);
        modelInput.set("stock",job.input().path("stock"));modelInput.set("engine",job.input().path("engine"));modelInput.set("sources",research.modelSources(evidence));
        for(String field:List.of("materialityMetrics","annualRevenue","companyProfile","businessProducts")) if(evidence.has(field)) modelInput.set(field,evidence.path(field));
        modelInput.set("previousResearch",job.input().path("previousResearch").isMissingNode()?json.tree(Map.of()):job.input().path("previousResearch"));
        modelInput.set("coverage",evidence.path("coverage"));modelInput.put("collectedAt",evidence.path("collectedAt").asText());modelInput.set("sectorScope",evidence.path("sectorScope"));
        modelInput.set("signalContext",SignalInterpretation.context(json,job.input().path("engine"),evidence));
        if(evidence.path("sources").isEmpty()) report=contract.noEvidence();
        else {
            reserve(request);var response=ai.assess(request);
            if(!store.recordNews(job,evidence,response,clock.millis())) throw new AiFailure("LEASE_LOST",false);
            report=response.assessment();contract.validate(report,request);
        }
        report=research.enrichReport(report,evidence,job.input().path("previousResearch"),job.id(),job.input().path("engine"));
        ObjectNode result=(ObjectNode)json.tree(Map.of("generationMode","NEWS_INSIGHTS","context",job.input().path("context"),"stock",job.input().path("stock"),
                "engine",job.input().path("engine"),"evidence",evidence,"report",report,"inputHash",json.hash(Map.of("input",job.input(),"evidenceHash",evidence.path("evidenceHash"))),"completedAt",clock.millis()));
        var considered=result.putArray("modelSourceIds");for(var source:modelInput.path("sources")) considered.add(source.path("id").asText());
        result.set("signalContext",modelInput.path("signalContext"));
        var changes=result.putObject("changes");changes.set("previousId",job.input().path("previousId").isMissingNode()?json.tree(null):job.input().path("previousId"));
        var previous=job.input().path("previousSources");Set<String> keys=new HashSet<>();for(var source:previous) keys.add(source.path("dedupKey").asText());
        var added=changes.putArray("newSourceIds");if(!previous.isMissingNode()) for(var source:evidence.path("sources")) if(!keys.contains(source.path("dedupKey").asText())) added.add(source.path("id").asText());
        changes.put("meaning","이전 저장 리포트 대비 새로 수집한 자료이며, 새 사건 또는 독립적인 확인을 의미하지 않습니다.");return result;
    }
    private void reserve(JsonNode input) {
        int bytes=json.write(GeminiReviewAdapter.request(input)).getBytes(StandardCharsets.UTF_8).length;
        if(bytes>input.path("versions").path("maxInputBytes").asInt()) throw new AiFailure("AI_INPUT_TOO_LARGE",false);
        // A deliberately conservative byte-based token reservation, including prompt/schema overhead.
        long reserve=bytes+input.path("versions").path("maxOutputTokens").asLong()+1024;
        if(!store.reserveCall(settings.ownerScope(),LocalDate.ofInstant(clock.instant(),DecisionService.SEOUL).toString(),settings.dailyCalls(),settings.dailyTokens(),reserve)) throw new AiFailure("AI_DAILY_LIMIT",false);
    }
    @PreDestroy public void close() { queue.shutdownNow(); heartbeat.shutdownNow(); }
}
