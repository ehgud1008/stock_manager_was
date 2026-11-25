package com.example.stockanalysis.decision;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;
import org.junit.jupiter.api.*;

class PublicNewsCollectorTest {
    DecisionJson json=new DecisionJson(new ObjectMapper());HttpServer server;String base;AtomicInteger calls;
    @BeforeEach void setup() throws Exception {server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);base="http://127.0.0.1:"+server.getAddress().getPort()+"/";calls=new AtomicInteger();}
    @AfterEach void stop(){server.stop(0);}
    void respond(String path,int status,byte[] body){server.createContext(path,exchange->{calls.incrementAndGet();exchange.sendResponseHeaders(status,body.length);try(var out=exchange.getResponseBody()){out.write(body);}});}
    PublicNewsCollector collector(NewsSourceSettings settings){return new PublicNewsCollector(json,settings,Clock.fixed(Instant.parse("2026-10-11T00:00:00Z"),ZoneOffset.UTC),HttpClient.newHttpClient(),base+"rss?q=",base+"naver?",base);}
    JsonNode input(){return json.tree(Map.of("stock",Map.of("stockName","삼성전자","stockCode","005930"),"context",Map.of("sectorQuery",""),"windowStart","2026-10-01","windowEnd","2026-10-11"));}
    @Test void rssFiltersDatesAndUnrelatedCompaniesMergesDuplicatesAndDoesNotPretendToReadArticles() {
        String rss="<rss><channel>"+item("삼성전자 업황 회복","https://example.com/a?utm_source=rss","Fri, 09 Oct 2026 09:00:00 +0900")
                +item("삼성전자 업황 회복","https://example.com/a","Fri, 09 Oct 2026 09:00:00 +0900")
                +item("다른 기업 소식","https://example.com/b","Fri, 09 Oct 2026 09:00:00 +0900")
                +item("삼성전자 오래된 소식","https://example.com/c","Tue, 01 Sep 2026 09:00:00 +0900")+"</channel></rss>";
        respond("/rss",200,rss.getBytes(StandardCharsets.UTF_8));server.start();var result=collector(new NewsSourceSettings("","","")).collect(input());
        assertThat(result.path("sources")).hasSize(1);var source=result.path("sources").get(0);assertThat(source.path("url").asText()).isEqualTo("https://example.com/a");
        assertThat(source.path("excerpt").asText()).isEmpty();assertThat(source.path("depth").asText()).isEqualTo("TITLE_ONLY");assertThat(source.path("eventDate").isNull()).isTrue();
        assertThat(source.path("topics")).hasSize(2);assertThat(result.path("coverage").get(1).path("status").asText()).isEqualTo("COLLECTED");
        assertThat(result.path("coverage").get(2).path("status").asText()).isEqualTo("NOT_CONFIGURED");
    }
    @Test void naverKeepsSearchSnippetAndRejectsNonWebLinks() {
        var valid=Map.of("title","<b>삼성전자</b> 수요 회복","description","삼성전자 수요가 회복될 가능성 &amp; 불확실성","originallink","https://example.com/news","pubDate","Fri, 09 Oct 2026 09:00:00 +0900");
        var invalid=Map.of("title","삼성전자 링크","description","요약","originallink","javascript:alert(1)","link","file:///secret","pubDate","Fri, 09 Oct 2026 09:00:00 +0900");
        respond("/naver",200,json.write(Map.of("items",List.of(valid,invalid))).getBytes(StandardCharsets.UTF_8));server.start();
        var result=collector(new NewsSourceSettings("id","secret","")).collect(input());assertThat(result.path("sources")).hasSize(1);
        var source=result.path("sources").get(0);assertThat(source.path("depth").asText()).isEqualTo("SEARCH_SNIPPET");assertThat(source.path("excerpt").asText()).contains("&").doesNotContain("<b>");
    }
    @Test void sourceFailureIsNotNoNewsAndXmlExternalEntitiesAreBlocked() {
        respond("/rss",503,"unavailable".getBytes(StandardCharsets.UTF_8));server.start();var result=collector(new NewsSourceSettings("","","")).collect(input());
        assertThat(result.path("sources")).isEmpty();assertThat(result.path("coverage").get(0).path("status").asText()).isEqualTo("FAILED");
        assertThatThrownBy(()->PublicNewsCollector.xml("<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///secret'>]><rss>&secret;</rss>".getBytes(StandardCharsets.UTF_8))).isInstanceOf(Exception.class);
    }
    @Test void officialFilingsMapExactStockCodeAndPreserveCorrectionsWithoutInventingDetails() throws Exception {
        respond("/rss",200,"<rss><channel/></rss>".getBytes(StandardCharsets.UTF_8));
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("CORPCODE.xml"));zip.write("<result><list><stock_code>005930</stock_code><corp_code>00126380</corp_code></list></result>".getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
        respond("/corpCode.xml",200,bytes.toByteArray());
        respond("/list.json",200,json.write(Map.of("status","000","list",List.of(Map.of("stock_code","005930","report_nm","[기재정정]단일판매공급계약체결","rcept_no","20261009000001","rcept_dt","20261009"),Map.of("stock_code","000660","report_nm","다른 회사","rcept_no","20261009000002","rcept_dt","20261009")))).getBytes(StandardCharsets.UTF_8));
        server.start();var result=collector(new NewsSourceSettings("","","secret")).collect(input());assertThat(result.path("sources")).hasSize(1);
        var source=result.path("sources").get(0);assertThat(source.path("depth").asText()).isEqualTo("FILING_TITLE");assertThat(source.path("title").asText()).startsWith("[기재정정]");assertThat(source.path("url").asText()).endsWith("rcpNo=20261009000001");
    }
    @Test void urlValidationDoesNotAllowCredentialsOrLocalDestinations(){
        assertThat(NewsInsightContract.safeUrl("https://user:pass@example.com/a")).isFalse();assertThat(NewsInsightContract.safeUrl("http://127.0.0.1/a")).isFalse();assertThat(NewsInsightContract.safeUrl("javascript:alert(1)")).isFalse();
    }
    @Test void collectsOriginalAndOfficialRevenueWithoutMixingConsolidatedAndSeparateScopes() throws Exception {
        respond("/rss",200,"<rss><channel/></rss>".getBytes(StandardCharsets.UTF_8));
        respond("/corpCode.xml",200,DartDocumentExtractorTest.zipped("CORPCODE.xml","<result><list><stock_code>005930</stock_code><corp_code>00126380</corp_code></list></result>"));
        respond("/list.json",200,json.write(Map.of("status","000","list",List.of(Map.of("stock_code","005930","report_nm","단일판매공급계약체결","rcept_no","20261010000001","rcept_dt","20261010")))).getBytes(StandardCharsets.UTF_8));
        respond("/company.json",200,json.write(Map.of("status","000","stock_code","005930","corp_name","삼성전자","induty_code","264","acc_mt","12")).getBytes(StandardCharsets.UTF_8));
        respond("/fnlttSinglAcnt.json",200,json.write(Map.of("status","000","list",List.of(
                Map.of("stock_code","005930","account_nm","매출액","fs_div","CFS","sj_div","IS","rcept_no","20261010000001","thstrm_amount","9,999","currency","KRW","bsns_year","2025","reprt_code","11011"),
                Map.of("stock_code","005930","account_nm","매출액","fs_div","OFS","sj_div","IS","rcept_no","20261010000001","thstrm_amount","8,888","currency","KRW","bsns_year","2024","reprt_code","11011"),
                Map.of("stock_code","005930","account_nm","매출액","fs_div","OFS","sj_div","IS","rcept_no","20261010000001","thstrm_amount","7,777","currency","KRW","bsns_year","2025","reprt_code","11013"),
                Map.of("stock_code","005930","account_nm","매출액","fs_div","OFS","sj_div","IS","rcept_no","20261010000001","thstrm_amount","1,000","currency","KRW","thstrm_dt","2025.01.01 ~ 2025.12.31","bsns_year","2025","reprt_code","11011")))).getBytes(StandardCharsets.UTF_8));
        respond("/document.xml",200,DartDocumentExtractorTest.zipped("20261010000001.xml","<DOCUMENT><TITLE>공급계약체결</TITLE><TABLE>"+DartDocumentExtractorTest.row("계약금액(원)","250")+DartDocumentExtractorTest.row("최근매출액(원)","1000")+DartDocumentExtractorTest.row("주요제품","전력기기")+"</TABLE></DOCUMENT>"));
        server.start();var result=collector(new NewsSourceSettings("","","secret")).collect(input());
        assertThat(result.path("annualRevenue").path("value").asText()).isEqualTo("1000");assertThat(result.path("companyProfile").path("industryCode").asText()).isEqualTo("264");
        assertThat(result.path("sources").get(0).path("depth").asText()).isEqualTo("OFFICIAL_DOCUMENT");assertThat(result.path("sources").get(0).path("facts").get(0).path("ratioPct").decimalValue()).isEqualByComparingTo("25");
        assertThat(result.path("productSearch").asText()).isEqualTo("전력기기");
    }
    private String item(String title,String url,String date){return "<item><title>"+title+"</title><link>"+url+"</link><pubDate>"+date+"</pubDate><source>예시 언론</source><description>기사 전문이 아닌 링크 목록</description></item>";}
}
