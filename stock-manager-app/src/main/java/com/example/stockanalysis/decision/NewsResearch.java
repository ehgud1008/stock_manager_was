package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Deterministic document identity, arithmetic and evidence coverage kept outside model judgement. */
public final class NewsResearch {
    private final DecisionJson json;
    public NewsResearch(DecisionJson json){this.json=json;}
    public ObjectNode prepare(JsonNode raw,String code) {
        ObjectNode evidence=raw.deepCopy();ArrayNode metrics=evidence.putArray("materialityMetrics");
        for(var n:evidence.path("sources")) {
            ObjectNode source=(ObjectNode)n;String documentKey=json.hash(Map.of("stock",code,"url",source.path("url").asText()));
            ObjectNode content=source.deepCopy();for(String field:List.of("id","documentKey","contentHash","documentVersionId","dedupKey","topics","originalStatus","originalFetchedEpoch")) content.remove(field);
            String hash=json.hash(content);source.put("documentKey",documentKey);source.put("contentHash",hash);source.put("documentVersionId",json.hash(List.of(documentKey,hash)));
            for(var fact:source.path("facts")) {
                ObjectNode metric=fact.deepCopy();metric.put("id","M"+(metrics.size()+1));metric.put("sourceId",source.path("id").asText());metrics.add(metric);
            }
        }
        evidence.put("researchVersion","research-v2");evidence.put("evidenceHash",json.hash(evidence.path("sources")));return evidence;
    }
    public ArrayNode modelSources(JsonNode evidence) {
        var selected=json.tree(List.of());ArrayNode out=(ArrayNode)selected;List<JsonNode> sources=new ArrayList<>();evidence.path("sources").forEach(sources::add);
        sources.sort(Comparator.comparingInt(s->full(s)?0:1));
        for(var source:sources) {
            if(out.size()>=24) break;ObjectNode bounded=source.deepCopy();bounded.remove("body");
            if(bounded.path("excerpt").asText().length()>600) bounded.put("excerpt",bounded.path("excerpt").asText().substring(0,600));
            ArrayNode passages=bounded.putArray("passages");int remaining=3000;Set<String> candidates=new LinkedHashSet<>();
            for(var fact:source.path("facts")) for(String field:List.of("amountQuote","revenueQuote")) if(fact.has(field)) candidates.add(fact.path(field).asText());
            for(var passage:source.path("passages")) candidates.add(passage.asText());
            for(String value:candidates) {if(value.length()>remaining) continue;passages.add(value);remaining-=value.length();}
            out.add(bounded);
        }
        return out;
    }
    public ObjectNode previous(JsonNode result) {
        ObjectNode previous=(ObjectNode)json.tree(Map.of());previous.put("engineHash",json.hash(result.path("engine")));previous.put("summary",result.path("report").path("summary").asText());
        if(result.path("report").has("signalInterpretation")) {
            var signal=result.path("report").path("signalInterpretation");previous.set("signalInterpretation",signal);
            var docs=previous.putArray("signalDocuments");for(var ref:signal.path("sourceIds")) for(var source:result.path("evidence").path("sources")) if(ref.equals(source.path("id")))
                docs.add(json.tree(Map.of("url",source.path("url").asText(),"contentHash",source.path("contentHash").asText())));
        }
        ArrayNode events=previous.putArray("events");int index=0;
        for(var n:result.path("report").path("events")) {
            ObjectNode event=(ObjectNode)json.tree(Map.of());String id=n.path("eventId").asText("LEGACY_"+(++index));event.put("eventId",id);
            for(String field:List.of("title","interpretation","engineRelation","importance","criticalQuestion")) if(n.has(field)) event.set(field,n.path(field));
            ArrayNode docs=event.putArray("documents");for(var ref:n.path("sourceIds")) for(var source:result.path("evidence").path("sources")) if(ref.asText().equals(source.path("id").asText()))
                docs.add(json.tree(Map.of("url",source.path("url").asText(),"contentHash",source.path("contentHash").asText(json.hash(source)),"title",source.path("title").asText(),"identityEligible",ref.equals(n.path("quoteSourceId")) && !source.path("contextOutsideWindow").asBoolean())));
            events.add(event);
        }
        return previous;
    }
    public ObjectNode enrichReport(JsonNode raw,JsonNode evidence,JsonNode previous,String jobId,JsonNode engine) {
        ObjectNode report=raw.deepCopy();int i=0;
        for(var n:report.path("events")) {
            ObjectNode event=(ObjectNode)n;Set<String> publishers=new HashSet<>();boolean original=false,snippet=false;Set<String> urls=new HashSet<>();
            for(var ref:event.path("sourceIds")) for(var source:evidence.path("sources")) if(ref.asText().equals(source.path("id").asText())) {
                original|=full(source);snippet|="SEARCH_SNIPPET".equals(source.path("depth").asText());publishers.add(source.path("publisher").asText("미확인"));urls.add(source.path("url").asText());
            }
            event.set("evidenceQuality",json.tree(Map.of("level",original?"ORIGINAL":snippet?"SNIPPET":"TITLE_ONLY","publisherCount",publishers.size(),"independentConfirmation","NOT_ESTABLISHED")));
            String previousId=event.path("previousEventId").asText("");JsonNode linked=null;
            for(var old:previous.path("events")) if(previousId.equals(old.path("eventId").asText())) linked=old;
            boolean exact=false,changed=false;Set<String> oldUrls=new HashSet<>();
            if(linked!=null) for(var doc:linked.path("documents")) {oldUrls.add(doc.path("url").asText());for(var source:evidence.path("sources")) if(urls.contains(source.path("url").asText()) && doc.path("url").equals(source.path("url"))) {
                if(doc.path("identityEligible").asBoolean() && source.path("id").equals(event.path("quoteSourceId")) && !source.path("contextOutsideWindow").asBoolean()) exact=true;
                changed|=!doc.path("contentHash").equals(source.path("contentHash"));
            }
            }
            String eventId=exact && previousId.matches("[0-9a-f]{64}")?previousId:json.hash(List.of(jobId,++i,event.path("title").asText()));
            event.put("eventId",eventId);event.put("eventVersionId",json.hash(List.of(jobId,eventId)));
            event.put("linkStatus",linked==null?"NEW":exact?"SAME_DOCUMENT":"AI_PROPOSED_LINK");
            event.put("documentChange",linked==null?"FIRST_OBSERVATION":!exact?"DIFFERENT_DOCUMENTS":!urls.equals(oldUrls)?"SOURCES_CHANGED":changed?"CONTENT_CHANGED":"SAME_CONTENT");
            if(linked!=null) {event.set("previousInterpretation",linked.path("interpretation"));event.set("previousEngineRelation",linked.path("engineRelation"));}
        }
        report.put("engineChanged",previous.has("engineHash") && !previous.path("engineHash").asText().equals(json.hash(engine)));
        return report;
    }
    public static boolean full(JsonNode source) {return Set.of("OFFICIAL_DOCUMENT","OFFICIAL_STRUCTURED").contains(source.path("depth").asText());}
}
