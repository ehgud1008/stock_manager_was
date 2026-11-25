package com.example.stockanalysis.stock.application.port.out;

import com.example.stockanalysis.stock.domain.StockCode;
import com.example.stockanalysis.stock.domain.StockProfile;

public interface StockInformationPort {

    StockProfile getStockProfile(StockCode stockCode);
}
