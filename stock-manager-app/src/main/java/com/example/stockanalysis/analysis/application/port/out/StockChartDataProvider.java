package com.example.stockanalysis.analysis.application.port.out;

import com.example.stockanalysis.analysis.domain.StockChartPoint;
import com.example.stockanalysis.analysis.domain.StockPricePeriod;
import java.time.LocalDate;
import java.util.List;

public interface StockChartDataProvider {

    List<StockChartPoint> fetch(
            String stockCode,
            StockPricePeriod period,
            int minuteInterval,
            LocalDate baseDate
    );
}
