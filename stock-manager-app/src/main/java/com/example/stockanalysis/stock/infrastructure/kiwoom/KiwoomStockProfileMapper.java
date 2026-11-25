package com.example.stockanalysis.stock.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomApiException;
import com.example.stockanalysis.kiwoom.client.KiwoomNumberParser;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KiwoomStockProfileMapper {

    private final KiwoomNumberParser numberParser;

    public KiwoomStockProfileMapper(KiwoomNumberParser numberParser) {
        this.numberParser = numberParser;
    }

    public StockProfile toDomain(StockCode requestedCode, Ka10001Response response) {
        String responseCode = normalizeStockCode(response.getStkCd());
        StockCode stockCode = StringUtils.hasText(responseCode)
                ? StockCode.of(responseCode)
                : requestedCode;

        if (!StringUtils.hasText(response.getStkNm())) {
            throw new KiwoomApiException("키움증권 종목정보 응답에 종목명이 없습니다.");
        }

        return new StockProfile(
                stockCode,
                response.getStkNm(),
                numberParser.absoluteDecimal(response.getCurPrc(), "현재가"),
                numberParser.absoluteDecimal(response.getOpenPric(), "시가"),
                numberParser.absoluteDecimal(response.getHighPric(), "고가"),
                numberParser.absoluteDecimal(response.getLowPric(), "저가"),
                numberParser.decimal(response.getMac(), "시가총액"),
                numberParser.decimal(response.getFloStk(), "상장주식수"),
                numberParser.decimal(response.getPer(), "PER"),
                numberParser.decimal(response.getEps(), "EPS"),
                numberParser.decimal(response.getRoe(), "ROE"),
                numberParser.decimal(response.getPbr(), "PBR"),
                numberParser.decimal(response.getBps(), "BPS")
        );
    }

    private String normalizeStockCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 7
                && (normalized.charAt(0) == 'A' || normalized.charAt(0) == 'a')) {
            return normalized.substring(1);
        }
        return normalized;
    }

}
