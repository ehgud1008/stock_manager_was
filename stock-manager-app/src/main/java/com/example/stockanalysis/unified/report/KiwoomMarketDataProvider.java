package com.example.stockanalysis.unified.report;

import static com.example.stockanalysis.unified.report.ReportModels.*;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.*;
import com.example.stockanalysis.kiwoom.contract.tr.ka20006.Ka20006Response;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class KiwoomMarketDataProvider implements MarketDataProvider {
    private final KiwoomBackdataCatalog catalog;
    private final KiwoomBackdataClient client;
    private final KiwoomBackdataRecordMapper mapper;
    private final Clock clock;
    public KiwoomMarketDataProvider(KiwoomBackdataCatalog catalog, KiwoomBackdataClient client,
                                   KiwoomBackdataRecordMapper mapper, Clock clock) {
        this.catalog=catalog;this.client=client;this.mapper=mapper;this.clock=clock;
    }
    public MarketSnapshot collect(LocalDate date) {
        var result=new ArrayList<IndexSnapshot>();
        for (var entry:Map.of("KOSPI","001","KOSDAQ","101").entrySet()) {
            var definition=catalog.getRequired("ka20006");
            Map<String,Object> request=Map.of("inds_cd",entry.getValue(),"base_dt",date.format(DateTimeFormatter.BASIC_ISO_DATE));
            var response=client.execute(definition,request,"N","");
            if (!(response.body() instanceof Ka20006Response body) || !entry.getValue().equals(body.getIndsCd()))
                throw new IllegalArgumentException("MARKET_IDENTITY_MISMATCH");
            var received=clock.instant();
            var records=mapper.map(definition,request,response.body(),date,received);
            var bars=new TreeMap<LocalDate,IndexBar>();
            for (var r:records) {
                if (r.businessDate()==null || r.businessDate().isAfter(date)) throw new IllegalArgumentException("MARKET_DATE_INVALID");
                var b=new IndexBar(r.businessDate(),point(r.value("open")),point(r.value("high")),point(r.value("low")),point(r.value("close")));
                if (b.low().compareTo(b.high())>0 || b.open().compareTo(b.low())<0 || b.open().compareTo(b.high())>0
                        || b.close().compareTo(b.low())<0 || b.close().compareTo(b.high())>0 || bars.putIfAbsent(b.date(),b)!=null)
                    throw new IllegalArgumentException("MARKET_OHLC_INVALID");
            }
            if (bars.size()<60 || !bars.lastKey().equals(date)) throw new IllegalStateException("MARKET_DATA_NOT_READY");
            result.add(new IndexSnapshot(entry.getKey(),entry.getValue(),"INDEX_POINTS",date,received,List.copyOf(bars.values()),records,
                    List.of("키움 KRX 지수 일봉 조회값이며 이후 정정될 수 있습니다. 첫 페이지 범위만 사용합니다.")));
        }
        result.sort(Comparator.comparing(IndexSnapshot::market));
        return new MarketSnapshot(List.copyOf(result),clock.instant());
    }
    private BigDecimal point(String text) {
        // ka20006 prices are integer hundredths of an index point; never apply stock-price scaling.
        if (text==null || !text.replace(",","").matches("[+-]?[0-9]+")) throw new IllegalArgumentException("MARKET_PRICE_FORMAT");
        var value=new BigDecimal(text.replace(",","")).abs().movePointLeft(2);
        if (value.signum()<=0) throw new IllegalArgumentException("MARKET_PRICE_INVALID");
        return value;
    }
}
