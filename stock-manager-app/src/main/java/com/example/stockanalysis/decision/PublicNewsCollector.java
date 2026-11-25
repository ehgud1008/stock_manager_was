package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PublicNewsCollector implements NewsEvidenceCollector {
    private final DecisionJson json; private final NewsSourceSettings settings; private final HttpClient client;
    private final String rssBase,naverBase,dartBase; private final Clock clock;
    private Map<String,String> corpCodes=Map.of(); private long corpExpires;
    private DecisionStore documentStore;
    @Autowired public void documentStore(DecisionStore store) {this.documentStore=store;}
    @Autowired public PublicNewsCollector(DecisionJson json,NewsSourceSettings settings,Clock clock) {
        this(json,settings,clock,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build(),
                "https://news.google.com/rss/search?q=","https://openapi.naver.com/v1/search/news.json?","https://opendart.fss.or.kr/api/");
    }
    PublicNewsCollector(DecisionJson json,NewsSourceSettings settings,Clock clock,HttpClient client,String rssBase,String naverBase,String dartBase) {
        this.json=json;this.settings=settings;this.clock=clock;this.client=client;this.rssBase=rssBase;this.naverBase=naverBase;this.dartBase=dartBase;
    }
    @Override public JsonNode collect(JsonNode input) {
        var out=json.tree(Map.of()).deepCopy();ObjectNode bundle=(ObjectNode)out;
        bundle.put("collectedAt",clock.instant().toString());bundle.put("windowStart",input.path("windowStart").asText());bundle.put("windowEnd",input.path("windowEnd").asText());
        ArrayNode sources=bundle.putArray("sources"),coverage=bundle.putArray("coverage");
        String company=input.path("stock").path("stockName").asText(),code=input.path("stock").path("stockCode").asText();
        if(company.isBlank() || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("Stock identity required");
        String sector=input.path("context").path("sectorQuery").asText("");
        collectNews(company,"COMPANY",company,input,sources,coverage);
        collectNews(sector.isBlank()?company+" 업황 산업":sector+" 업황","SECTOR",sector.isBlank()?company:sector,input,sources,coverage);
        try {
            if(settings.dartApiKey().isBlank()) coverage.add(status("DART","DISCLOSURE","NOT_CONFIGURED",0,"공시 API 키가 설정되지 않아 공식 공시 목록을 조회하지 않았습니다."));
            else {
                String corp=corpCode(code);
                if(corp==null) coverage.add(status("DART","DISCLOSURE","NO_MATCH",0,"종목코드에 해당하는 공시 고유번호를 찾지 못했습니다."));
                else {
                    String query="crtfc_key="+enc(settings.dartApiKey())+"&corp_code="+corp+"&bgn_de="+input.path("windowStart").asText().replace("-","")+"&end_de="+input.path("windowEnd").asText().replace("-","")+"&page_count=20&sort=date&sort_mth=desc";
                    var data=json.read(new String(get(dartBase+"list.json?"+query,false,1_000_000),StandardCharsets.UTF_8));
                    if(!Set.of("000","013").contains(data.path("status").asText())) throw new IOException("DART unavailable");
                    int before=sources.size();for(var item:data.path("list")) {
                        if(!code.equals(item.path("stock_code").asText())) continue;
                        String receipt=item.path("rcept_no").asText();if(!receipt.matches("[0-9]{14}")) continue;
                        String date=LocalDate.parse(item.path("rcept_dt").asText(),DateTimeFormatter.BASIC_ISO_DATE).toString();
                        add(sources,"DART","DISCLOSURE",item.path("report_nm").asText(),"",date,"https://dart.fss.or.kr/dsaf001/main.do?rcpNo="+receipt,"금융감독원 DART","FILING_TITLE",input);
                    }
                    coverage.add(status("DART","DISCLOSURE",sources.size()>before?"COLLECTED":"NO_MATCH",sources.size()-before,"공시 접수 목록 최대 이십 건입니다. 본문·계약 금액은 수집하지 않았습니다."));
                    enrichDart(corp,code,input,bundle,sources,coverage);
                }
            }
        } catch(Exception failure) { if(failure instanceof InterruptedException) Thread.currentThread().interrupt();coverage.add(status("DART","DISCLOSURE","FAILED",0,"공시 제공처 조회에 실패했습니다. 공시가 없다는 뜻은 아닙니다.")); }
        bundle.put("sectorQuery",sector);bundle.put("sectorScope",sector.isBlank()?"COMPANY_CONTEXT":"USER_QUERY");
        bundle.put("status",sources.isEmpty()?"INSUFFICIENT":"PARTIAL"); // Search/list metadata never establishes complete coverage.
        bundle.put("evidenceHash",json.hash(sources));return bundle;
    }
    private void enrichDart(String corp,String code,JsonNode input,ObjectNode bundle,ArrayNode sources,ArrayNode coverage) {
        String key="crtfc_key="+enc(settings.dartApiKey());
        try {
            var profile=json.read(new String(get(dartBase+"company.json?"+key+"&corp_code="+corp,false,500_000),StandardCharsets.UTF_8));
            if(!"000".equals(profile.path("status").asText()) || !code.equals(profile.path("stock_code").asText())) throw new IOException("Company mismatch");
            bundle.set("companyProfile",json.tree(Map.of("status","OFFICIAL_API","corpName",profile.path("corp_name").asText(),"industryCode",profile.path("induty_code").asText(),
                    "fiscalMonth",profile.path("acc_mt").asText(),"retrievedAt",clock.instant().toString(),"source","OpenDART 기업개황 API")));
        } catch(Exception e) {bundle.set("companyProfile",json.tree(Map.of("status","UNAVAILABLE")));}
        // Obtain an annual baseline and the associated business report. Never substitute quarterly revenue for annual revenue.
        try {
            int year=LocalDate.parse(input.path("windowEnd").asText()).getYear()-1;
            var financial=json.read(new String(get(dartBase+"fnlttSinglAcnt.json?"+key+"&corp_code="+corp+"&bsns_year="+year+"&reprt_code=11011",false,1_000_000),StandardCharsets.UTF_8));
            for(var row:financial.path("list")) if(code.equals(row.path("stock_code").asText()) && Integer.toString(year).equals(row.path("bsns_year").asText()) && "11011".equals(row.path("reprt_code").asText()) && "매출액".equals(row.path("account_nm").asText()) && "OFS".equals(row.path("fs_div").asText()) && "IS".equals(row.path("sj_div").asText()) && row.path("rcept_no").asText().matches("[0-9]{14}")) {
                String receipt=row.path("rcept_no").asText(),amount=row.path("thstrm_amount").asText(),currency=row.path("currency").asText();
                String digits=amount.replace(",","");
                if(!amount.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)") || !digits.matches("[0-9]{1,18}") || !"KRW".equals(currency)) continue;
                ObjectNode baseline=sources.addObject();baseline.put("id","S"+sources.size());baseline.put("provider","DART");baseline.putArray("topics").add("BUSINESS");
                baseline.put("title",year+" 사업보고서 · 별도 매출액");baseline.put("excerpt",year+" 사업연도 별도 매출액: "+amount+" KRW · 기간 "+row.path("thstrm_dt").asText());
                baseline.put("url","https://dart.fss.or.kr/dsaf001/main.do?rcpNo="+receipt);baseline.put("publisher","금융감독원 DART");baseline.putNull("publishedDate");baseline.putNull("eventDate");
                baseline.put("depth","OFFICIAL_STRUCTURED");baseline.put("contextOutsideWindow",true);baseline.put("dedupKey",json.hash(Map.of("receipt",receipt,"account","OFS_REVENUE")));
                bundle.set("annualRevenue",json.tree(Map.of("value",digits,"currency","KRW","period",year+" 사업연도", "periodDetail",row.path("thstrm_dt").asText(),"scope","별도 재무제표","sourceId",baseline.path("id").asText())));
                break;
            }
            coverage.add(status("DART","BUSINESS",bundle.has("annualRevenue")?"COLLECTED":"NO_MATCH",bundle.has("annualRevenue")?1:0,"직전 사업연도 별도 매출액 기준입니다. 공시 기재 최근매출액 비교의 분모로 자동 대체하지 않습니다."));
        } catch(Exception e) {coverage.add(status("DART","BUSINESS","FAILED",0,"직전 사업연도 별도 매출액을 확인하지 못했습니다. 연결·분기 매출로 대신 계산하지 않습니다."));}
        int attempts=0,read=0;ArrayNode products=bundle.putArray("businessProducts");
        // Business report first, then material recent filings; bounded to four original documents per request.
        List<JsonNode> ordered=new ArrayList<>();sources.forEach(ordered::add);
        ordered.sort(Comparator.comparingInt(s->s.path("contextOutsideWindow").asBoolean()?0:1));
        for(var node:ordered) {
            if(!"DART".equals(node.path("provider").asText()) || !node.path("title").asText().matches(".*(보고서|계약|증자|전환사채|자기주식|거래정지|철회).*")) continue;
            if(attempts++>=4) break;ObjectNode source=(ObjectNode)node;String receipt=source.path("url").asText().replaceAll(".*rcpNo=","");
            try {
                JsonNode original=documentStore==null?null:documentStore.cachedResearchDocument(input.path("researchOwner").asText(),code,source.path("url").asText(),clock.millis()-86_400_000).orElse(null);
                ObjectNode document;
                if(original!=null && source.path("title").equals(original.path("title")) && DartDocumentExtractor.VERSION.equals(original.path("extractorVersion").asText())) document=(ObjectNode)original;
                else {document=new DartDocumentExtractor(json).extract(get(dartBase+"document.xml?"+key+"&rcept_no="+receipt,false,8_000_000),receipt,source.path("title").asText());document.put("originalFetchedEpoch",clock.millis());}
                for(String field:List.of("body","passages","facts","dates","products","file","extractorVersion","originalFetchedEpoch")) source.set(field,document.path(field).deepCopy());
                source.put("depth","OFFICIAL_DOCUMENT");source.put("originalStatus","EXTRACTED");
                if(!source.path("dates").path("계약체결일").isMissingNode() && source.path("title").asText().matches(".*(단일판매|공급계약|계약체결).*" ) && !source.path("title").asText().contains("정정")) source.set("eventDate",source.path("dates").path("계약체결일"));
                for(var product:source.path("products")) products.add(json.tree(Map.of("name",product.asText(),"sourceId",source.path("id").asText())));
                read++;
            } catch(Exception e) {if(e instanceof InterruptedException) Thread.currentThread().interrupt();source.put("originalStatus","UNAVAILABLE_OR_UNSUPPORTED");}
        }
        coverage.add(status("DART","ORIGINAL",read>0?"COLLECTED":"NO_MATCH",read,"주요 원문 최대 네 건을 시도합니다. 추출 실패·지원하지 않는 문서는 제목 또는 공식 API 수치 수준으로 남습니다."));
        if(input.path("context").path("sectorQuery").asText("").isBlank() && !products.isEmpty()) {
            String product=products.get(0).path("name").asText();collectNews(product+" 업황","PRODUCT",product,input,sources,coverage);
            bundle.put("productSearch",product);
        }
    }
    private void collectNews(String query,String topic,String required,JsonNode input,ArrayNode sources,ArrayNode coverage) {
        String provider=settings.naverConfigured()?"NAVER":"GOOGLE_NEWS_RSS";int before=sources.size();
        try {
            if(settings.naverConfigured()) {
                var data=json.read(new String(get(naverBase+"query="+enc(query)+"&display=20&sort=date",true,1_000_000),StandardCharsets.UTF_8));
                if(!data.path("items").isArray()) throw new IOException("Invalid news response");
                for(var item:data.path("items")) {
                    String title=clean(item.path("title").asText()),excerpt=clean(item.path("description").asText());
                    if(!relevant(title+" "+excerpt,required)) continue;
                    String url=item.path("originallink").asText();if(!NewsInsightContract.safeUrl(url)) url=item.path("link").asText();
                    add(sources,provider,topic,title,excerpt,newsDate(item.path("pubDate").asText()),url,host(url),excerpt.isBlank()?"TITLE_ONLY":"SEARCH_SNIPPET",input);
                }
            } else {
                var document=xml(get(rssBase+enc(query+" after:"+input.path("windowStart").asText()+" before:"+LocalDate.parse(input.path("windowEnd").asText()).plusDays(1))+"&hl=ko&gl=KR&ceid=KR:ko",false,1_000_000));
                NodeList items=document.getElementsByTagName("item");
                for(int i=0;i<Math.min(items.getLength(),30);i++) {
                    Element item=(Element)items.item(i);String title=clean(tag(item,"title"));
                    if(!relevant(title,required)) continue;
                    add(sources,provider,topic,title,"",newsDate(tag(item,"pubDate")),tag(item,"link"),clean(tag(item,"source")),"TITLE_ONLY",input);
                    if(sources.size()-before>=12) break;
                }
            }
            int count=0;for(var source:sources) for(var included:source.path("topics")) if(topic.equals(included.asText())) count++;
            coverage.add(status(provider,topic,count>0?"COLLECTED":"NO_MATCH",count,
                    settings.naverConfigured()?"뉴스 검색 요약이며 기사 전문·사건 발생일을 확인한 자료는 아닙니다.":"뉴스 RSS 제목만 확보했습니다. 상세 영향은 원문 확인이 필요합니다."));
        } catch(Exception failure) {if(failure instanceof InterruptedException) Thread.currentThread().interrupt();coverage.add(status(provider,topic,"FAILED",sources.size()-before,"뉴스 제공처 조회에 실패했습니다. 관련 사건이 없다는 뜻은 아닙니다."));}
    }
    void add(ArrayNode sources,String provider,String topic,String title,String excerpt,String publishedDate,String url,String publisher,String depth,JsonNode input) {
        title=clean(title);excerpt=clean(excerpt);
        if(title.isBlank() || title.length()>500 || excerpt.length()>1800 || !NewsInsightContract.safeUrl(url) || publishedDate==null) return;
        LocalDate date=LocalDate.parse(publishedDate);if(date.isBefore(LocalDate.parse(input.path("windowStart").asText())) || date.isAfter(LocalDate.parse(input.path("windowEnd").asText()))) return;
        String canonical=canonicalUrl(url),key=json.hash(Map.of("title",title.replaceAll("[^\\p{L}\\p{N}]","").toLowerCase(Locale.ROOT),"date",publishedDate));
        for(var old:sources) if(old.path("url").asText().equals(canonical) || old.path("dedupKey").asText().equals(key)) {
            ArrayNode topics=(ArrayNode)old.path("topics");if(!topics.toString().contains("\""+topic+"\"")) topics.add(topic);return;
        }
        ObjectNode source=sources.addObject();source.put("id","S"+sources.size());source.put("provider",provider);source.putArray("topics").add(topic);
        source.put("title",title);source.put("excerpt",excerpt);source.put("publishedDate",publishedDate);source.putNull("eventDate");
        source.put("url",canonical);source.put("publisher",publisher);source.put("depth",depth);source.put("dedupKey",key);
    }
    static String canonicalUrl(String raw) {
        URI uri=URI.create(raw);String query=uri.getRawQuery();
        if(query!=null) query=Arrays.stream(query.split("&")).filter(p->!p.toLowerCase(Locale.ROOT).matches("(utm_[^=]*|gclid|fbclid)=.*")).reduce((a,b)->a+"&"+b).orElse("");
        return uri.getScheme().toLowerCase(Locale.ROOT)+"://"+uri.getRawAuthority().toLowerCase(Locale.ROOT)+(uri.getRawPath()==null?"":uri.getRawPath())+(query==null || query.isBlank()?"":"?"+query);
    }
    private synchronized String corpCode(String code) throws Exception {
        if(clock.millis()>=corpExpires) {
            byte[] zip=get(dartBase+"corpCode.xml?crtfc_key="+enc(settings.dartApiKey()),false,8_000_000);
            try(var stream=new ZipInputStream(new ByteArrayInputStream(zip))) {
                if(stream.getNextEntry()==null) throw new IOException("Invalid corporate directory");
                byte[] data=stream.readNBytes(35_000_001);if(data.length>35_000_000) throw new IOException("Directory too large");
                var list=xml(data).getElementsByTagName("list");Map<String,String> mapped=new HashMap<>();
                for(int i=0;i<list.getLength();i++) {var item=(Element)list.item(i);String stock=tag(item,"stock_code"),corp=tag(item,"corp_code");if(stock.matches("[0-9]{6}") && corp.matches("[0-9]{8}")) mapped.put(stock,corp);}
                corpCodes=Map.copyOf(mapped);corpExpires=clock.millis()+86_400_000;
            }
        }
        return corpCodes.get(code);
    }
    private byte[] get(String url,boolean naver,int limit) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent","StockManager/1.0");
        if(naver) request.header("X-Naver-Client-Id",settings.naverClientId()).header("X-Naver-Client-Secret",settings.naverClientSecret());
        var response=client.send(request.GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        try(var body=response.body()) {if(response.statusCode()!=200) throw new IOException("Source unavailable");byte[] data=body.readNBytes(limit+1);if(data.length>limit) throw new IOException("Source too large");return data;}
    }
    static Document xml(byte[] bytes) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
    static String clean(String value) {return value.replaceAll("<[^>]*>"," ").replace("&quot;","\"").replace("&#39;","'").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&nbsp;"," ").replaceAll("\\s+"," ").strip();}
    static boolean relevant(String text,String required) {return text.replaceAll("\\s+","").toLowerCase(Locale.ROOT).contains(required.replaceAll("\\s+","").toLowerCase(Locale.ROOT));}
    private static String tag(Element item,String tag) {var values=item.getElementsByTagName(tag);return values.getLength()==0?"":values.item(0).getTextContent().strip();}
    private static String newsDate(String raw) {try{return ZonedDateTime.parse(raw,DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(DecisionService.SEOUL).toLocalDate().toString();}catch(RuntimeException e){return null;}}
    private JsonNode status(String provider,String topic,String status,int count,String detail) {return json.tree(Map.of("provider",provider,"topic",topic,"status",status,"count",count,"detail",detail));}
    private static String enc(String value) {return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static String host(String url) {try{return URI.create(url).getHost();}catch(RuntimeException e){return "뉴스 제공처";}}
}
