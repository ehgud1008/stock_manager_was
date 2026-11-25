package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** One bounded plain-text message; external names cannot inject HTML/Markdown formatting. */
public final class ReportMessage {
    private ReportMessage() {}
    public static String format(DailyReport report,String baseUrl){
        if(report.status().equals("FAILED"))return "오늘 종합분석 작업에 실패했습니다";
        var text=new StringBuilder("장 마감 종합분석 · ").append(report.tradeDate()).append("\n");
        text.append("정보 확인: ").append(DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul")).format(report.informationAsOf())).append(" KST\n");
        text.append("다음 거래일: ").append(report.forecastDate()).append("\n");
        for(var market:report.markets()){
            text.append("\n").append(market.market()).append(" ").append(market.dayReturn().setScale(2,RoundingMode.HALF_UP)).append("% · ").append(market.bias()).append("\n");
            text.append("분석 표본의 상승 비율 ").append(market.advancingPercent()==null?"미확인":market.advancingPercent().setScale(1,RoundingMode.HALF_UP)+"%").append("\n");
            if(market.validStocks()<market.eligibleStocks())text.append("분석 범위 ").append(market.validStocks()).append("/").append(market.eligibleStocks()).append("종목 · 확보된 표본 기준\n");
            text.append(market.baseScenario()).append("\n").append(market.upsideScenario()).append("\n").append(market.downsideScenario()).append("\n");
        }
        if(report.candidates().isEmpty())text.append(report.tierLists()!=null && report.tierLists().activeSignalsTotal()>0?"\n운영 선정 0종목 · 유효 매수 신호와 보류 사유를 상세 화면에서 확인하세요.\n":"\n오늘은 조건을 충족한 종목이 없습니다\n");
        else {
            long entries=report.candidates().stream().filter(s->s.disposition().equals("ENTRY_CONDITIONAL")).count();
            text.append("\n조건부 진입 ").append(entries).append("종목 · 관찰 ").append(report.candidates().size()-entries).append("종목\n");
            if(entries==0)text.append("오늘은 진입 조건을 충족한 종목이 없습니다. 아래는 관찰 대상입니다.\n");
            text.append("\n선정 ").append(report.candidates().size()).append("종목 · 아래는 최대 5종목 요약\n");
            for(var stock:report.candidates().stream().limit(5).toList()){
                text.append("\n").append(clean(stock.name(),35)).append(" (").append(stock.code()).append(") · ").append(stock.disposition().equals("WATCH")?"관찰":"조건부 진입 검토").append("\n");
                text.append("전략: ").append(switch(stock.strategy()){case "BREAKOUT"->"돌파";case "PULLBACK_RECOVERY"->"눌림 회복";case "BREAKOUT_RETEST"->"돌파 재지지";default->"6→1 전환 초기";}).append("\n");
                stock.evidence().stream().filter(e->Boolean.TRUE.equals(e.passed())&&e.role().equals("SELECTION")).limit(2).forEach(e->text.append("· ").append(e.label()).append("\n"));
                if(stock.scenario()!=null)text.append("참고 진입 ").append(stock.scenario().entryFrom()).append("~").append(stock.scenario().entryTo()).append("원 · 손절 기준 ").append(stock.scenario().stopLoss()).append("원\n");
                if(stock.netRewardRisk()!=null)text.append("비용 가정 반영 손익비 ").append(stock.netRewardRisk().setScale(2,RoundingMode.HALF_UP)).append("배\n");
                if(stock.disposition().equals("WATCH"))text.append("현재는 관찰 대상이며 진입 조건 충족을 뜻하지 않습니다.\n");
                text.append("조건 취소: 진입 구간 이탈·손절 기준 훼손·매도 신호 발생. 소폭 갭상승도 허용 진입 구간 안에서만 검토하며 진입 상단을 넘으면 진입하지 않습니다.\n");
            }
        }
        if(report.tierLists()!=null && report.tierLists().activeSignals()!=null) text.append("\nA/B등급 유효 매수 신호 ").append(report.tierLists().activeSignalsTotal()).append("종목 · 선정 여부와 보류 사유는 상세 화면 참고\n");
        if(report.tierLists()!=null)text.append("\n운영 선정과 별도: B등급 비교 ").append(report.tierLists().comparisonBTotal())
                .append("종목 · A/B 신호 대기 ").append(report.tierLists().signalWaitingTotal()).append("종목 (매수 추천 아님, 상세 화면 참고)\n");
        text.append("\n검증 전 규칙 기반 후보이며 승률·수익률을 보장하지 않습니다. 업종·이벤트·시장 전체 수급은 미확인입니다.\n");
        if(baseUrl!=null&&!baseUrl.isBlank())text.append("상세: ").append(baseUrl.replaceAll("/+$","")).append("/daily-reports?runId=").append(report.runId());
        String result=text.toString();
        if(result.length()>4096)throw new IllegalArgumentException("REPORT_MESSAGE_TOO_LONG");
        return result;
    }
    private static String clean(String value,int limit){var text=value.replaceAll("[\\p{Cntrl}\\r\\n]"," ");return text.length()>limit?text.substring(0,limit):text;}
}
