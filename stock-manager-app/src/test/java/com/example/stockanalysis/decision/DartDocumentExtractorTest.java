package com.example.stockanalysis.decision;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

class DartDocumentExtractorTest {
    final DecisionJson json=new DecisionJson(new ObjectMapper());final DartDocumentExtractor extractor=new DartDocumentExtractor(json);
    static final String RECEIPT="20261010000001";
    static byte[] zipped(String filename,String body) throws Exception {
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry(filename));zip.write(body.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return bytes.toByteArray();
    }
    static String row(String label,String value){return "<TR><TD>"+label+"</TD><TD>"+value+"</TD></TR>";}
    String document(String rows){return "<DOCUMENT><TITLE>단일판매 공급계약</TITLE><P>공시 원문 근거 문단입니다.</P><TABLE>"+rows+"</TABLE></DOCUMENT>";}
    @Test void computesOnlyExplicitWonAmountsAndPreservesDateAndProductEvidence() throws Exception {
        var result=extractor.extract(zipped(RECEIPT+".xml",document(row("계약금액(원)","2,500,000")+row("최근매출액(원)","10,000,000")+row("계약체결일","2026-10-09")+row("주요 제품","전력기기"))),RECEIPT,"단일판매공급계약");
        assertThat(result.path("facts")).hasSize(1);assertThat(result.path("facts").get(0).path("ratioPct").decimalValue()).isEqualByComparingTo("25");
        assertThat(result.path("facts").get(0).path("period").asText()).contains("회계기간 원문 확인 필요");
        assertThat(result.path("body").asText()).contains(result.path("facts").get(0).path("amountQuote").asText());
        assertThat(result.path("dates").path("계약체결일").asText()).isEqualTo("2026-10-09");assertThat(result.path("products").get(0).asText()).isEqualTo("전력기기");
    }
    @Test void correctionsMixedUnitsRepeatedValuesAndZeroDenominatorsNeverProduceRatios() throws Exception {
        String valid=row("계약금액(원)","250")+row("최근매출액(원)","1000");
        assertThat(extractor.extract(zipped(RECEIPT+".xml",document(valid)),RECEIPT,"[기재정정]계약").path("facts")).isEmpty();
        for(String rows:new String[]{row("계약금액(달러)","250")+row("최근매출액(원)","1000"),valid+row("계약금액(원)","500"),row("계약금액(원)","250")+row("최근매출액(원)","0")})
            assertThat(extractor.extract(zipped(RECEIPT+".xml",document(rows)),RECEIPT,"공급계약체결").path("facts")).isEmpty();
    }
    @Test void groupedRowsRetainExactQuotationAndAttachmentsCannotBecomeMainDocuments() throws Exception {
        var result=extractor.extract(zipped(RECEIPT+".xml",document("<TR><TD>2. 계약내역</TD><TD>계약금액(원)</TD><TD>250</TD></TR>"+row("최근매출액(원)","1000"))),RECEIPT,"공급계약체결");
        assertThat(result.path("facts").get(0).path("amountQuote").asText()).isEqualTo("2. 계약내역 | 계약금액(원) | 250");
        assertThatThrownBy(()->extractor.extract(zipped("attachment.xml",document("")),RECEIPT,"계약")).isInstanceOf(IOException.class);
    }
}
