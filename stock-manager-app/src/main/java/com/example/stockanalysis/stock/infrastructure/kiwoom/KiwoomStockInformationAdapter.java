package com.example.stockanalysis.stock.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.KiwoomResponse;
import com.example.stockanalysis.kiwoom.client.stockinfo.KiwoomStockInfoClient;
import com.example.stockanalysis.kiwoom.contract.tr.ka10001.Ka10001Response;
import com.example.stockanalysis.stock.application.port.out.StockInformationPort;
import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;
import org.springframework.stereotype.Component;

@Component
public class KiwoomStockInformationAdapter implements StockInformationPort {

    private final KiwoomStockInfoClient client;
    private final KiwoomStockProfileMapper mapper;

    public KiwoomStockInformationAdapter(
            KiwoomStockInfoClient client,
            KiwoomStockProfileMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public StockProfile getStockProfile(StockCode stockCode) {
        KiwoomResponse<Ka10001Response> response =
                client.getBasicInformation(stockCode.value());
        return mapper.toDomain(stockCode, response.body());
    }
}
