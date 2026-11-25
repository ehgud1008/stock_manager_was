package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;
import org.w3c.dom.*;

/** Conservative extraction: ambiguous units, repeated values and correction tables never become numeric facts. */
public final class DartDocumentExtractor {
    public static final String VERSION="dart-document-v1";
    private final DecisionJson json;
    public DartDocumentExtractor(DecisionJson json) {this.json=json;}
    public ObjectNode extract(byte[] zipBytes,String receipt,String title) throws Exception {
        byte[] selected=null;String file=null;long expanded=0;int entries=0;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;while((entry=zip.getNextEntry())!=null) {
                if(++entries>30) throw new IOException("Too many attachments");
                byte[] bytes=zip.readNBytes(8_000_001);expanded+=bytes.length;
                if(bytes.length>8_000_000 || expanded>20_000_000) throw new IOException("Document limit exceeded");
                // DART's principal XML is named after the receipt; attachments are not interchangeable.
                if(entry.getName().equalsIgnoreCase(receipt+".xml")) {selected=bytes;file=entry.getName();}
            }
        }
        if(selected==null) throw new IOException("Principal document missing");
        var document=PublicNewsCollector.xml(selected);var nodes=document.getElementsByTagName("*");
        List<String> blocks=new ArrayList<>();Map<String,List<String>> fields=new LinkedHashMap<>();
        for(int i=0;i<nodes.getLength();i++) {
            var element=(Element)nodes.item(i);String tag=element.getTagName().toUpperCase(Locale.ROOT);
            if(tag.equals("TR")) {
                List<String> cells=new ArrayList<>();for(Node n=element.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element cell && Set.of("TD","TH","TE","TU").contains(cell.getTagName().toUpperCase(Locale.ROOT))) cells.add(text(cell));
                if(cells.size()==2 || cells.size()==3 && cells.get(1).replaceAll("\\s+","").matches("계약금액\\(원\\)|최근매출액\\(원\\)|계약체결일|시작일|종료일"))
                    fields.computeIfAbsent(cells.get(cells.size()-2).replaceAll("\\s+",""),k->new ArrayList<>()).add(cells.get(cells.size()-1));
                if(!cells.isEmpty()) blocks.add(String.join(" | ",cells));
            } else if(Set.of("P","TITLE","SUBTITLE").contains(tag) && !insideTable(element)) {
                String text=text(element);if(!text.isBlank()) blocks.add(text);
            }
        }
        if(blocks.isEmpty()) throw new IOException("Unsupported document structure");
        // Keep the document body server-side. Only bounded relevant passages enter the model request.
        String body=String.join("\n",new LinkedHashSet<>(blocks));if(body.length()>1_000_000) throw new IOException("Text limit exceeded");
        ObjectNode result=(ObjectNode)json.tree(Map.of("body",body,"extractorVersion",VERSION,"file",file));
        ArrayNode passages=result.putArray("passages");int budget=0;
        var preferred=blocks.stream().filter(s->s.matches("(?s).*(계약|매출|정정|철회|주요제품|주요 제품|사업의 내용|제품명|영업이익|증자|전환사채|자기주식|거래정지).*" )).toList();
        var selectedBlocks=new LinkedHashSet<String>(preferred);selectedBlocks.addAll(blocks.stream().limit(10).toList());
        for(String block:selectedBlocks) {String clipped=block.length()>900?block.substring(0,900):block;if(budget+clipped.length()>6000 || passages.size()>=14) break;passages.add(clipped);budget+=clipped.length();}
        ArrayNode facts=result.putArray("facts");boolean corrected=title.contains("정정");
        if(!corrected && title.matches(".*(단일판매|공급계약|계약체결).*")) {
            BigDecimal amount=number(fields,"계약금액(원)"),sales=number(fields,"최근매출액(원)");
            if(amount!=null && sales!=null && amount.signum()>0 && sales.signum()>0) {
                String amountQuote="계약금액(원) | "+fields.get("계약금액(원)").get(0),salesQuote="최근매출액(원) | "+fields.get("최근매출액(원)").get(0);
                // Normalized labels may differ from original spacing: retain actual matching rows.
                amountQuote=matchingRow(blocks,"계약금액(원)");salesQuote=matchingRow(blocks,"최근매출액(원)");
                facts.add(json.tree(Map.of("kind","CONTRACT_TO_REPORTED_REVENUE","amount",amount.toPlainString(),"revenue",sales.toPlainString(),"currency","KRW",
                        "ratioPct",amount.multiply(BigDecimal.valueOf(100)).divide(sales,2,RoundingMode.HALF_UP),"amountQuote",amountQuote,"revenueQuote",salesQuote,
                        "period","공시 기재 최근매출액 기준 · 회계기간 원문 확인 필요","scope","공시 자체 비교 기준","limitation","계약 총액과 공시 기재 최근매출액의 비율입니다. 다년 계약의 연간 매출 기여나 이익률이 아닙니다.")));
            }
        }
        ObjectNode dates=result.putObject("dates");for(String label:List.of("계약체결일","시작일","종료일")) {
            var values=fields.get(label);if(values==null || values.size()!=1) continue;
            var matcher=Pattern.compile("^(20\\d{2})[-./년 ]+(\\d{1,2})[-./월 ]+(\\d{1,2})[일. ]*$").matcher(values.get(0));
            if(matcher.matches()) try {dates.put(label,LocalDate.of(Integer.parseInt(matcher.group(1)),Integer.parseInt(matcher.group(2)),Integer.parseInt(matcher.group(3))).toString());} catch(RuntimeException ignored) { /* invalid source date */ }
        }
        var products=result.putArray("products");for(String key:List.of("주요제품","주요제품명","제품명")) {
            var values=fields.get(key);if(values==null || values.size()!=1) continue;String value=values.get(0);
            if(value.matches("[\\p{L}\\p{N} ·-]{2,30}") && !value.matches(".*(매출|비율|금액|구분).*")) products.add(value);
        }
        return result;
    }
    private static String matchingRow(List<String> blocks,String label) {return blocks.stream().filter(b->{var parts=b.split(" \\| ");return parts.length>=2 && parts[parts.length-2].replaceAll("\\s+","").equals(label);}).findFirst().orElseThrow();}
    private static BigDecimal number(Map<String,List<String>> fields,String name) {
        var values=fields.get(name);if(values==null || values.size()!=1 || !values.get(0).matches("[0-9]{1,3}(,[0-9]{3})*|[0-9]+")) return null;
        try {var value=new BigDecimal(values.get(0).replace(",",""));return value.precision()<=18?value:null;}catch(RuntimeException e){return null;}
    }
    private static boolean insideTable(Node node) {for(Node p=node.getParentNode();p!=null;p=p.getParentNode()) if(p.getNodeName().equalsIgnoreCase("TABLE")) return true;return false;}
    private static String text(Element element) {return PublicNewsCollector.clean(element.getTextContent());}
}
