package com.example.stockanalysis.stock.infrastructure.kiwoom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomNumberParser;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KiwoomStockProfileMapperTest {

    private final KiwoomStockProfileMapper mapper =
            new KiwoomStockProfileMapper(new KiwoomNumberParser());

    @Test
    void mapsKiwoomTransportFieldsToDomainTypes() {
        Ka10001Response response = response(
                "A005930",
                "삼성전자",
                "+81,000",
                "15.10"
        );

        StockProfile profile = mapper.toDomain(StockCode.of("005930"), response);

        assertThat(profile.stockCode()).isEqualTo(StockCode.of("005930"));
        assertThat(profile.stockName()).isEqualTo("삼성전자");
        assertThat(profile.currentPrice()).isEqualByComparingTo(new BigDecimal("81000"));
        assertThat(profile.per()).isEqualByComparingTo(new BigDecimal("15.10"));
    }

    @Test
    void rejectsMalformedNumericValueAtTheExternalBoundary() {
        Ka10001Response response = response(
                "005930",
                "삼성전자",
                "not-a-price",
                "15.10"
        );

        assertThatThrownBy(() -> mapper.toDomain(StockCode.of("005930"), response))
                .isInstanceOf(KiwoomApiException.class)
                .hasMessageContaining("현재가");
    }

    private Ka10001Response response(
            String stockCode,
            String stockName,
            String currentPrice,
            String per
    ) {
        return Ka10001Response.builder()
                .stkCd(stockCode)
                .stkNm(stockName)
                .curPrc(currentPrice)
                .openPric("80000")
                .highPric("82000")
                .lowPric("79500")
                .mac("4830000")
                .floStk("5969782550")
                .per(per)
                .eps("5400")
                .roe("9.2")
                .pbr("1.4")
                .bps("57800")
                .returnCode(0)
                .returnMsg("정상처리")
                .build();
    }
}
