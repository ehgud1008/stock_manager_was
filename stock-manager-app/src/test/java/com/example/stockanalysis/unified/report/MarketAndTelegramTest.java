package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.*;
import com.example.stockanalysis.kiwoom.contract.tr.ka20006.Ka20006Response;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.jupiter.api.Test;

class MarketAndTelegramTest {
    @Test void validatesIndexIdentityAndConvertsIntegerHundredthsWithoutMixingStockPrices(){
        var client=mock(KiwoomBackdataClient.class);
        when(client.execute(any(),anyMap(),eq("N"),eq(""))).thenAnswer(i->{
            Map<String,Object> request=i.getArgument(1);
            var rows=new ArrayList<Ka20006Response.IndsDtPoleQryItem>();
            for(var day:dates())rows.add(Ka20006Response.IndsDtPoleQryItem.builder().dt(day.format(DateTimeFormatter.BASIC_ISO_DATE))
                    .openPric("250000").highPric("251000").lowPric("249000").curPrc("250000").trdeQty("1000").trdePrica("10000").build());
            return new KiwoomBackdataCallResult(Ka20006Response.builder().indsCd((String)request.get("inds_cd")).indsDtPoleQry(rows).returnCode(0).build(),null);
        });
        var provider=new KiwoomMarketDataProvider(new KiwoomBackdataCatalog(),client,new KiwoomBackdataRecordMapper(MAPPER),Clock.fixed(NOW,ZoneOffset.UTC));
        var snapshot=provider.collect(DAY);
        assertThat(snapshot.indices()).hasSize(2);
        assertThat(snapshot.indices().get(0).bars().get(0).close()).isEqualByComparingTo("2500");
        assertThat(snapshot.indices().get(0).source()).hasSize(100);
        verify(client,times(2)).execute(any(),anyMap(),eq("N"),eq(""));
        when(client.execute(any(),anyMap(),anyString(),anyString())).thenReturn(new KiwoomBackdataCallResult(Ka20006Response.builder().indsCd("999").build(),null));
        assertThatThrownBy(()->provider.collect(DAY)).hasMessage("MARKET_IDENTITY_MISMATCH");
    }
    @Test void telegramProtocolSeparatesSuccessExplicitRejectionAndAmbiguousResponses(){
        var gateway=new TelegramHttpGateway(new TelegramProperties(false,"","",""),MAPPER);
        assertThat(gateway.interpret(200,"{\"ok\":true,\"result\":{\"message_id\":123}}").state()).isEqualTo("SENT");
        var limited=gateway.interpret(429,"{\"ok\":false,\"error_code\":429,\"parameters\":{\"retry_after\":123}}");
        assertThat(limited.state()).isEqualTo("RETRY_WAIT");assertThat(limited.retryAfterSeconds()).isEqualTo(123);
        assertThat(gateway.interpret(403,"{\"ok\":false,\"error_code\":403}").state()).isEqualTo("FAILED");
        assertThat(gateway.interpret(503,"bad gateway").state()).isEqualTo("UNKNOWN");
        assertThat(gateway.interpret(200,"{\"ok\":true}").state()).isEqualTo("UNKNOWN");
        assertThat(gateway.send("someone","hello").state()).isEqualTo("FAILED");
    }
    @Test void credentialsAreNotExposedAndDisabledConfigurationNeedsNoToken(){
        var settings=new TelegramProperties(true,"12345:secret_bot_token","123456","https://stocks.example");
        assertThat(settings.toString()).doesNotContain("secret_bot_token","123456");
        assertThatThrownBy(()->new TelegramProperties(true,"","","")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new TelegramProperties(false,"","","https://user:password@stocks.example")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void fiveCandidateSummaryFitsTelegramLimitAndIncludesActualStrategy() throws Exception {
        var analyzer=new CandidateAnalyzer(MAPPER,settings());var bundle=bundle("005930",true);
        var stock=analyzer.analyze("run","KOSPI",bundle.detail(),bundle.input(),market().indices().get(0));
        var index=new ReportModels.MarketView("KOSPI",java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,100,100,Map.of(),0,"중립","기본 시나리오".repeat(20),"상방 시나리오".repeat(20),"하방 시나리오".repeat(20),java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,java.math.BigDecimal.ONE,List.of());
        var report=new ReportModels.DailyReport("run","READY",DAY,DAY.plusDays(1),NOW,NOW,"v1","UNVALIDATED_RULES","ALL",List.of(index,index),Collections.nCopies(20,stock),100,100,List.of());
        var text=ReportMessage.format(report,"https://stocks.example/"+"a".repeat(480));
        assertThat(text).hasSizeLessThanOrEqualTo(4096).contains("전략: 돌파","선정 20종목").doesNotContain("실패 종목");
        assertThat(text.split("전략:",-1)).hasSize(6);
        assertThat(text).contains("진입 상단을 넘으면 진입하지 않습니다").doesNotContain("갭 상승 시 추격하지 않습니다");
    }
}
