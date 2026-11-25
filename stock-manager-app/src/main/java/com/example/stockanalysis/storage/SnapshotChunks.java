package com.example.stockanalysis.storage;

import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Reversible field order and receipt templates let immutable history share storage across JVMs. */
public final class SnapshotChunks {
    private static final String MARKER="\"~ua.snapshot.observedAt.v1~\"";
    private final ObjectMapper mapper;
    public SnapshotChunks(ObjectMapper mapper){this.mapper=mapper;}
    public record Part(String literal,String objectId,List<String> observedAt,int objectBytes,List<String> fieldOrder,List<String> valuesOrder){}
    public record Manifest(int version,String sha256,int utf8Bytes,List<Part> parts){}
    public record Document(String sha256,int utf8Bytes,Manifest manifest,Map<String,EodSnapshotCodec.Encoded> objects){}
    private record Span(int start,int end){}
    private record ArraySlice(List<Span> records){}
    private record Shape(String raw,LinkedHashMap<String,String> fields,Map<String,Span> spans,boolean compact){}
    private record Info(Span span,Shape shape,Shape values,LocalDate date){}
    public Document split(String json){
        try{
            var original=EodSnapshotCodec.encode(json);var arrays=new ArrayList<ArraySlice>();
            try(var p=mapper.getFactory().createParser(json)){if(p.nextToken()==null)throw new IllegalArgumentException("EMPTY_JSON");walk(p,arrays);if(p.nextToken()!=null)throw new IllegalArgumentException("MULTIPLE_JSON_VALUES");}
            arrays.sort(Comparator.comparingInt(a->a.records().get(0).start()));
            var parts=new ArrayList<Part>();var objects=new LinkedHashMap<String,EodSnapshotCodec.Encoded>();int cursor=0;
            for(var array:arrays){
                var info=new ArrayList<Info>();for(var span:array.records())info.add(info(json,span));
                // Use the median spacing: an exceptional holiday must not change the history bucket size.
                var gaps=new ArrayList<Long>();for(int i=1;i<Math.min(info.size(),10);i++)if(info.get(i-1).date()!=null&&info.get(i).date()!=null)gaps.add(Math.abs(ChronoUnit.DAYS.between(info.get(i-1).date(),info.get(i).date())));
                Collections.sort(gaps);boolean yearly=!gaps.isEmpty()&&gaps.get(gaps.size()/2)>=5;
                int first=0;
                while(first<info.size()){
                    int last=first+1;String bucket=bucket(info.get(first).date(),yearly);
                    while(last<info.size()&&last-first<64&&Objects.equals(bucket,bucket(info.get(last).date(),yearly)))last++;
                    var group=info.subList(first,last);var topOrder=commonOrder(group.stream().map(Info::shape).toList());var valuesOrder=commonOrder(group.stream().map(Info::values).toList());
                    boolean observed=!json.contains(MARKER)&&group.stream().allMatch(i->i.shape().fields().containsKey("observedAt"));
                    var receipts=observed?group.stream().map(i->i.shape().fields().get("observedAt")).toList():null;
                    int start=group.get(0).span().start(),end=group.get(group.size()-1).span().end();
                    if(start>cursor)parts.add(literal(json.substring(cursor,start)));
                    var chunk=new StringBuilder(json.substring(start,end));
                    for(int i=group.size()-1;i>=0;i--){var row=group.get(i);var fields=new LinkedHashMap<>(row.shape().fields());
                        if(observed)fields.put("observedAt",MARKER);
                        if(valuesOrder!=null)fields.put("values",render(row.values().fields(),sorted(valuesOrder)));
                        String normalized=topOrder!=null?render(fields,sorted(topOrder)):replaceFields(row.shape(),fields);
                        chunk.replace(row.span().start()-start,row.span().end()-start,normalized);
                    }
                    var encoded=EodSnapshotCodec.encode(chunk.toString());objects.putIfAbsent(encoded.sha256(),encoded);
                    parts.add(new Part(null,encoded.sha256(),receipts,encoded.utf8Bytes(),differentOrder(topOrder),differentOrder(valuesOrder)));
                    cursor=end;first=last;
                }
            }
            if(cursor<json.length())parts.add(literal(json.substring(cursor)));
            for(int i=0;i<parts.size();i++)if(parts.get(i).literal()!=null&&parts.get(i).literal().length()>2048){var e=EodSnapshotCodec.encode(parts.get(i).literal());objects.putIfAbsent(e.sha256(),e);parts.set(i,new Part(null,e.sha256(),null,e.utf8Bytes(),null,null));}
            return new Document(original.sha256(),original.utf8Bytes(),new Manifest(1,original.sha256(),original.utf8Bytes(),List.copyOf(parts)),objects);
        }catch(java.io.IOException e){throw new IllegalArgumentException("SNAPSHOT_JSON_INVALID",e);}
    }
    public String restore(Manifest manifest,Map<String,String> objects){
        if(manifest.version()!=1||manifest.utf8Bytes()<0||manifest.utf8Bytes()>32*1024*1024)throw new IllegalArgumentException("SNAPSHOT_MANIFEST_INVALID");
        var out=new StringBuilder();
        for(var part:manifest.parts()){
            if((part.literal()==null)==(part.objectId()==null))throw new IllegalArgumentException("SNAPSHOT_PART_INVALID");
            if(part.literal()!=null)out.append(part.literal());else{
                String value=objects.get(part.objectId());if(value==null)throw new IllegalArgumentException("SNAPSHOT_OBJECT_MISSING");
                if(part.fieldOrder()!=null||part.valuesOrder()!=null)value=restoreOrder(value,part);
                if(part.observedAt()!=null){var text=new StringBuilder();int cursor=0;for(String token:part.observedAt()){int at=value.indexOf(MARKER,cursor);if(at<0)throw new IllegalArgumentException("SNAPSHOT_RECEIPT_MISSING");text.append(value,cursor,at).append(token);cursor=at+MARKER.length();}if(value.indexOf(MARKER,cursor)>=0)throw new IllegalArgumentException("SNAPSHOT_RECEIPT_COUNT");value=text.append(value,cursor,value.length()).toString();}
                out.append(value);
            }
            if(out.length()>32*1024*1024)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
        }
        String json=out.toString();var bytes=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(bytes.length!=manifest.utf8Bytes()||!EodSnapshotCodec.hash(bytes).equals(manifest.sha256()))throw new IllegalArgumentException("SNAPSHOT_DOCUMENT_INTEGRITY_FAILED");return json;
    }
    private String restoreOrder(String chunk,Part part){
        try{
            var records=new ArrayList<Span>();String wrapped="["+chunk+"]";
            try(var p=mapper.getFactory().createParser(wrapped)){p.nextToken();while(p.nextToken()!=JsonToken.END_ARRAY){int start=(int)p.currentTokenLocation().getCharOffset()-1;p.skipChildren();records.add(new Span(start,(int)p.currentLocation().getCharOffset()-1));}}
            var out=new StringBuilder(chunk);
            for(int i=records.size()-1;i>=0;i--){var span=records.get(i);var shape=shape(chunk.substring(span.start(),span.end()));var fields=new LinkedHashMap<>(shape.fields());
                if(part.valuesOrder()!=null)fields.put("values",render(shape(fields.get("values")).fields(),part.valuesOrder()));
                String restored=part.fieldOrder()!=null?render(fields,part.fieldOrder()):replaceFields(shape,fields);out.replace(span.start(),span.end(),restored);
            }return out.toString();
        }catch(java.io.IOException e){throw new IllegalArgumentException("SNAPSHOT_ORDER_INVALID",e);}
    }
    private Info info(String json,Span span)throws java.io.IOException{
        var shape=shape(json.substring(span.start(),span.end()));LocalDate date=null;
        for(String key:List.of("businessDate","date","priceDate","openTime")){String value=shape.fields().get(key);if(value!=null&&value.startsWith("\"")&&value.length()>=12)try{date=LocalDate.parse(value.substring(1,11));break;}catch(RuntimeException ignored){}}
        String values=shape.fields().get("values");return new Info(span,shape,values!=null&&values.startsWith("{")?shape(values):null,date);
    }
    private Shape shape(String raw)throws java.io.IOException{
        var fields=new LinkedHashMap<String,String>();var spans=new LinkedHashMap<String,Span>();boolean simple=true;
        try(var p=mapper.getFactory().createParser(raw)){if(p.nextToken()!=JsonToken.START_OBJECT)throw new IllegalArgumentException("OBJECT_EXPECTED");while(p.nextToken()!=JsonToken.END_OBJECT){String key=p.currentName();simple&=key.matches("[A-Za-z0-9_]+");p.nextToken();int start=(int)p.currentTokenLocation().getCharOffset();if(p.currentToken()==JsonToken.VALUE_STRING)p.getText();else p.skipChildren();int end=(int)p.currentLocation().getCharOffset();if(fields.put(key,raw.substring(start,end))!=null)simple=false;spans.put(key,new Span(start,end));}}
        return new Shape(raw,fields,spans,simple&&raw.equals(render(fields,new ArrayList<>(fields.keySet()))));
    }
    private String replaceFields(Shape shape,Map<String,String> fields){
        var text=new StringBuilder(shape.raw());var keys=new ArrayList<>(shape.fields().keySet());for(int i=keys.size()-1;i>=0;i--){String key=keys.get(i);if(!Objects.equals(fields.get(key),shape.fields().get(key))){var span=shape.spans().get(key);text.replace(span.start(),span.end(),fields.get(key));}}return text.toString();
    }
    private List<String> commonOrder(List<Shape> shapes){List<String> order=null;for(var shape:shapes){if(shape==null||!shape.compact())return null;var keys=new ArrayList<>(shape.fields().keySet());if(order==null)order=keys;else if(!order.equals(keys))return null;}return order;}
    private List<String> sorted(List<String> values){var sorted=new ArrayList<>(values);Collections.sort(sorted);return sorted;}
    private List<String> differentOrder(List<String> order){return order==null||order.equals(sorted(order))?null:order;}
    private String render(Map<String,String> fields,List<String> order){var text=new StringJoiner(",","{","}");for(String key:order){String value=fields.get(key);if(value==null)throw new IllegalArgumentException("SNAPSHOT_FIELD_MISSING");text.add("\""+key+"\":"+value);}return text.toString();}
    private Part literal(String value){return new Part(value,null,null,0,null,null);}
    private String bucket(LocalDate date,boolean yearly){return date==null?"unknown":yearly?String.valueOf(date.getYear()):date.toString().substring(0,7);}
    private void walk(JsonParser p,List<ArraySlice> arrays)throws java.io.IOException{
        if(p.currentToken()==JsonToken.START_OBJECT){while(p.nextToken()!=JsonToken.END_OBJECT){p.nextToken();walk(p,arrays);}}
        else if(p.currentToken()==JsonToken.START_ARRAY){var nested=new ArrayList<ArraySlice>();var records=new ArrayList<Span>();boolean objects=true;while(p.nextToken()!=JsonToken.END_ARRAY){int start=(int)p.currentTokenLocation().getCharOffset();objects&=p.currentToken()==JsonToken.START_OBJECT;walk(p,nested);records.add(new Span(start,(int)p.currentLocation().getCharOffset()));}if(!nested.isEmpty())arrays.addAll(nested);else if(objects&&records.size()>=8&&records.get(records.size()-1).end()-records.get(0).start()>=1024)arrays.add(new ArraySlice(records));}
    }
}
