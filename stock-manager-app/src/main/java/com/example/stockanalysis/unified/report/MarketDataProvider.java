package com.example.stockanalysis.unified.report;

import java.time.LocalDate;
import com.example.stockanalysis.unified.report.ReportModels.MarketSnapshot;

public interface MarketDataProvider { MarketSnapshot collect(LocalDate date); }
