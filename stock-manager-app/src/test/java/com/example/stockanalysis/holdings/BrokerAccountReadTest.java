package com.example.stockanalysis.holdings;

import com.example.stockanalysis.decision.AccountRiskModels;
import com.example.stockanalysis.decision.AccountRiskService;
import com.example.stockanalysis.kiwoom.client.KiwoomErrorDecoder;
import com.example.stockanalysis.kiwoom.client.account.KiwoomAccountReadClient;
import com.example.stockanalysis.kiwoom.config.KiwoomProperties;
import com.example.stockanalysis.shared.exception.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrokerAccountReadTest {
    private static final LocalDate TODAY=LocalDate.parse("2026-11-18");
    private static final Instant NOW=Instant.parse("2026-11-18T00:00:00Z");

    @Test void accessKeyIsCheckedBeforeCallingBrokerAndResultsAreNotCacheable() {
        var service=mock(BrokerAccountService.class);
        String key="local-test-01234567890123456789012345";
        var disabled=new BrokerAccountController(service,"");
        var protectedController=new BrokerAccountController(service,key);
        var request=new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.50");
        assertThatThrownBy(()->disabled.cash(key,request)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->protectedController.fills("wrong",TODAY,null,request)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->protectedController.cash(key,request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(service);
        when(service.cash()).thenReturn(new BrokerAccountService.Cash("BROKER_MOCK",NOW,null,null,null,null,null));
        request.setSecure(true);
        assertThat(protectedController.cash(key,request).getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test void usesFixedInquiryOperationsPreservesSignedPnlAndContinuesFills() {
        var builder=RestClient.builder().baseUrl("https://broker.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        var properties=new KiwoomProperties();
        properties.setUseMock(true);
        var accounts=mock(AccountRiskService.class);
        when(accounts.get()).thenReturn(new AccountRiskModels.View("NOT_CONFIGURED",null,NOW,""));
        var service=new BrokerAccountService(new KiwoomAccountReadClient(builder.build(),new KiwoomErrorDecoder()),properties,
                accounts,mock(HoldingService.class),Clock.fixed(NOW,ZoneOffset.UTC));
        server.expect(requestTo("https://broker.test/api/dostk/acnt")).andExpect(method(HttpMethod.POST))
                .andExpect(header("api-id","kt00001"))
                .andRespond(withSuccess("""
                        {"return_code":0,"entr":"00000100","ord_alow_amt":"300","100stk_ord_alow_amt":"80"}
                        """,MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://broker.test/api/dostk/acnt")).andExpect(header("api-id","kt00009"))
                .andExpect(header("cont-yn","N")).andExpect(jsonPath("$.ord_dt").value("20261118"))
                .andRespond(withSuccess("""
                        {"return_code":0,"acnt_ord_cntr_prst_array":[
                        {"stk_cd":"A005930","cntr_no":"0000001","cntr_qty":"00002","cntr_uv":"72000"},
                        {"stk_cd":"A005930","cntr_no":"0000000","cntr_qty":"0"}]}
                        """,MediaType.APPLICATION_JSON).header("cont-yn","Y").header("next-key","page2"));
        server.expect(requestTo("https://broker.test/api/dostk/acnt")).andExpect(header("api-id","kt00009"))
                .andExpect(header("cont-yn","Y")).andExpect(header("next-key","page2"))
                .andRespond(withSuccess("{\"return_code\":0,\"acnt_ord_cntr_prst_array\":[]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://broker.test/api/dostk/acnt")).andExpect(header("api-id","ka10074"))
                .andRespond(withSuccess("{\"return_code\":0,\"rlzt_pl\":\"-000050\",\"dt_rlzt_pl\":[]}",MediaType.APPLICATION_JSON));
        var cash=service.cash();
        assertThat(cash.environment()).isEqualTo("BROKER_MOCK");
        assertThat(cash.cashOnlyOrderAmount()).isEqualByComparingTo("80");
        assertThat(cash.brokerOrderAmount()).isEqualByComparingTo("300");
        var fills=service.fills(TODAY,null);
        assertThat(fills.items()).singleElement().satisfies(fill->{
            assertThat(fill.stockCode()).isEqualTo("005930");
            assertThat(fill.quantity()).isEqualByComparingTo("2");
        });
        assertThat(service.fills(TODAY,fills.next()).next()).isNull();
        assertThat(service.pnl(TODAY,TODAY,null).reportedRealized()).isEqualByComparingTo("-50");
        assertThatThrownBy(()->service.fills(TODAY.plusDays(1),null)).isInstanceOf(BusinessException.class);
        server.verify();
    }
}
