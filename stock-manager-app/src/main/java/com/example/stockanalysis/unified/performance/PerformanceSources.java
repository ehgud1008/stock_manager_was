package com.example.stockanalysis.unified.performance;

import static com.example.stockanalysis.unified.performance.PerformanceModels.*;
import com.example.stockanalysis.engine.model.*;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import com.example.stockanalysis.unified.report.ReportModels.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read only: reuses EOD snapshots. Never requests a quote or changes the original analysis. */
@Repository
public class PerformanceSources {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    private record Saved(String id,String basis,Instant collected,String json,LocalDate tradeDate,String reference) {
        Saved(String id,String basis,Instant collected,String json,LocalDate date){this(id,basis,collected,json,date,null);}
    }
    public PerformanceSources(DataSource source,ObjectMapper mapper){this(source,mapper,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public PerformanceSources(DataSource source,ObjectMapper mapper,com.example.stockanalysis.storage.SnapshotRepository snapshots){jdbc=new JdbcTemplate(source);this.mapper=mapper;this.snapshots=snapshots;}
    private String reference(java.sql.ResultSet row)throws java.sql.SQLException{return snapshots==null?null:row.getString("input_snapshot_id");}
    private String inline(java.sql.ResultSet row)throws java.sql.SQLException{return reference(row)==null?EodSnapshotCodec.read(row):null;}
    private AnalysisInput input(Saved saved){return input(saved.reference()==null?saved.json():snapshots.read(saved.reference()));}
    public DailyReport report(String id){return jdbc.query("SELECT report_json FROM ua_daily_report WHERE run_id=? AND status='READY'",(r,n)->read(r.getString(1),DailyReport.class),id).stream().findFirst().orElse(null);}
    /** Frozen B selections, including rows beyond the report display limit. */
    public List<StockDecision> comparisonSelections(String id,List<String> codes){
        if(codes!=null&&codes.isEmpty())return List.of();
        var args=new ArrayList<Object>();args.add(id);
        String sql="SELECT detail_json FROM ua_candidate_evidence WHERE run_id=? AND disposition='RESEARCH_B'";
        if(codes!=null){sql+=" AND stock_code IN ("+String.join(",",Collections.nCopies(codes.size(),"?"))+")";args.addAll(codes);}
        return jdbc.query(sql+" ORDER BY stock_code",(r,n)->read(r.getString(1),StockDecision.class),args.toArray());
    }
    public Anchor anchor(String id,String code,String market,LocalDate date) {
        var saved=jdbc.queryForObject("SELECT e.price_basis,i.input_json,i.input_gzip,i.input_sha256,i.input_utf8_bytes"+(snapshots==null?"":",i.input_snapshot_id")+" FROM ua_eod_item i JOIN ua_eod_run e ON e.run_id=i.run_id WHERE i.run_id=? AND i.stock_code=?",
                (r,n)->new Saved(id,r.getString(1),null,inline(r),date,reference(r)),id,code);
        var input=input(saved);
        if(!code.equals(input.stock().stockCode()) || !date.equals(input.baseDate()) || !"KIWOOM_KRX_DAILY_ADJUSTED".equals(saved.basis()))throw new IllegalArgumentException("ANCHOR_IDENTITY");
        return new Anchor(code,market,saved.basis(),candle(input,date));
    }
    public Optional<Observation> stock(Anchor anchor,LocalDate date,Instant now) {
        var rows=jdbc.query("SELECT r.run_id,e.price_basis,i.collected_epoch,i.input_json,i.input_gzip,i.input_sha256,i.input_utf8_bytes"+(snapshots==null?"":",i.input_snapshot_id")+",r.base_date FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id JOIN ua_eod_item i ON i.run_id=r.run_id JOIN ua_scan_item s ON s.run_id=i.run_id AND s.stock_code=i.stock_code "
                +"WHERE i.stock_code=? AND s.market=? AND s.status='COMPLETED' AND (i.input_json IS NOT NULL OR i.input_gzip IS NOT NULL"+(snapshots==null?"":" OR i.input_snapshot_id IS NOT NULL")+") AND i.collected_epoch<=? AND r.base_date>=? AND r.base_date<=? ORDER BY i.collected_epoch,r.run_id FETCH FIRST 5 ROWS ONLY",
                (r,n)->new Saved(r.getString(1),r.getString(2),Instant.ofEpochMilli(r.getLong(3)),inline(r),r.getDate("base_date").toLocalDate(),reference(r)),anchor.code(),anchor.market(),now.toEpochMilli(),java.sql.Date.valueOf(date),java.sql.Date.valueOf(now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()));
        for(var saved:rows) {
            try {
                var input=input(saved);
                if(!anchor.code().equals(input.stock().stockCode()))return Optional.of(invalid("STOCK",anchor.code(),date,saved,"IDENTITY_MISMATCH"));
                if(!input.baseDate().equals(saved.tradeDate()) || input.baseDate().isAfter(saved.collected().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                        || input.dailyCandles().stream().anyMatch(c->c.businessDate().isAfter(input.baseDate())))throw new IllegalArgumentException();
                var target=candle(input,date);
                var previous=candle(input,anchor.candle().businessDate());
                if(!anchor.priceBasis().equals(saved.basis()) || !samePrices(previous,anchor.candle()))
                    return Optional.of(invalid("STOCK",anchor.code(),date,saved,"PRICE_BASIS_CHANGED"));
                if(date.isAfter(saved.collected().atZone(ZoneId.of("Asia/Seoul")).toLocalDate()))throw new IllegalArgumentException();
                return Optional.of(new Observation("STOCK",anchor.code(),date,"VALID",target,saved.id(),saved.collected(),"EOD_SNAPSHOT"));
            }catch(RuntimeException e){return Optional.of(invalid("STOCK",anchor.code(),date,saved,"INVALID_SOURCE"));}
        }
        return Optional.empty();
    }

    /** One bounded-range query per selected stock; no quotes, orders, or evaluation-table dependency. */
    public List<Observation> monitoringHistory(Anchor anchor,List<LocalDate> dates,Instant now) {
        if(dates.isEmpty())return List.of();
        var today=now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        var last=dates.get(dates.size()-1).plusDays(14);
        var until=today.isBefore(last)?today:last;
        if(until.isBefore(dates.get(0)))return List.of();
        long observedBy=Math.min(now.toEpochMilli(),last.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()-1);
        // The earliest saved snapshot for each base date is evidence; a later correction must not
        // silently replace an earlier invalid observation. A 20-session range plus 14 days is bounded.
        var rows=jdbc.query("SELECT run_id,price_basis,collected_epoch,input_json,input_gzip,input_sha256,input_utf8_bytes"+(snapshots==null?"":",input_snapshot_id")+",base_date FROM ("
                +"SELECT r.run_id,e.price_basis,i.collected_epoch,i.input_json,i.input_gzip,i.input_sha256,i.input_utf8_bytes"+(snapshots==null?"":",i.input_snapshot_id")+",r.base_date,"
                +"ROW_NUMBER() OVER(PARTITION BY r.base_date ORDER BY i.collected_epoch,r.run_id) sample_rank "
                +"FROM ua_scan_run r JOIN ua_eod_run e ON e.run_id=r.run_id JOIN ua_eod_item i ON i.run_id=r.run_id "
                +"JOIN ua_scan_item s ON s.run_id=i.run_id AND s.stock_code=i.stock_code "
                +"WHERE i.stock_code=? AND s.market=? AND s.status='COMPLETED' AND (i.input_json IS NOT NULL OR i.input_gzip IS NOT NULL"+(snapshots==null?"":" OR i.input_snapshot_id IS NOT NULL")+") "
                +"AND i.collected_epoch<=? AND r.base_date>=? AND r.base_date<=?) "
                +"WHERE sample_rank=1 ORDER BY collected_epoch,run_id",
                (r,n)->new Saved(r.getString(1),r.getString(2),Instant.ofEpochMilli(r.getLong(3)),inline(r),r.getDate("base_date").toLocalDate(),reference(r)),
                anchor.code(),anchor.market(),observedBy,java.sql.Date.valueOf(dates.get(0)),java.sql.Date.valueOf(until));
        var found=new LinkedHashMap<LocalDate,Observation>();
        for(var saved:rows) {
            AnalysisInput analysis=null;String invalidReason=null;
            try {
                analysis=input(saved);
                if(!anchor.code().equals(analysis.stock().stockCode()))invalidReason="IDENTITY_MISMATCH";
                else if(!analysis.baseDate().equals(saved.tradeDate()) || analysis.baseDate().isAfter(saved.collected().atZone(ZoneId.of("Asia/Seoul")).toLocalDate()))invalidReason="INVALID_SOURCE";
                else if(!anchor.priceBasis().equals(saved.basis()) || !samePrices(candle(analysis,anchor.candle().businessDate()),anchor.candle()))invalidReason="PRICE_BASIS_CHANGED";
            }catch(RuntimeException e){invalidReason="INVALID_SOURCE";}
            for(var date:dates) {
                if(date.isAfter(today)||date.isAfter(saved.tradeDate())||found.containsKey(date))continue;
                if(invalidReason!=null){found.put(date,invalid("STOCK",anchor.code(),date,saved,invalidReason));continue;}
                try {
                    found.put(date,new Observation("STOCK",anchor.code(),date,"VALID",candle(analysis,date),saved.id(),saved.collected(),"EOD_SNAPSHOT"));
                }catch(RuntimeException e){found.put(date,invalid("STOCK",anchor.code(),date,saved,"INVALID_SOURCE"));}
            }
        }
        return List.copyOf(found.values());
    }
    public Optional<Observation> index(MarketView market,LocalDate baseDate,LocalDate date,Instant now) {
        var rows=jdbc.query("SELECT d.run_id,d.market_json,r.base_date FROM ua_daily_report d JOIN ua_scan_run r ON r.run_id=d.run_id WHERE d.market_json IS NOT NULL AND r.base_date>=? AND r.base_date<=? ORDER BY r.base_date,d.created_epoch FETCH FIRST 20 ROWS ONLY",
                (r,n)->new Saved(r.getString(1),"KRX_INDEX_POINTS",null,r.getString(2),r.getDate(3).toLocalDate()),java.sql.Date.valueOf(date),java.sql.Date.valueOf(now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()));
        for(var row:rows) {
            var snapshot=read(row.json(),MarketSnapshot.class);
            if(snapshot.collectedAt().isAfter(now))continue;
            var saved=new Saved(row.id(),row.basis(),snapshot.collectedAt(),row.json(),row.tradeDate());
            try {
                var index=snapshot.indices().stream().filter(i->i.market().equals(market.market())).findFirst().orElseThrow();
                if(index.collectedAt().isAfter(now))continue;
                if(!"INDEX_POINTS".equals(index.unit()) || !(market.market().equals("KOSPI")?"001":"101").equals(index.code())
                        || !index.tradeDate().equals(row.tradeDate()) || index.tradeDate().isAfter(index.collectedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                        || index.bars().stream().anyMatch(b->b.date().isAfter(index.tradeDate())))throw new IllegalArgumentException();
                var base=index.bars().stream().filter(b->b.date().equals(baseDate)).findFirst().orElseThrow();
                if(base.close().compareTo(market.referenceClose())!=0)return Optional.of(invalid("INDEX",market.market(),date,saved,"PRICE_BASIS_CHANGED"));
                var matches=index.bars().stream().filter(b->b.date().equals(date)).toList();
                if(matches.size()!=1)throw new IllegalArgumentException();var bar=matches.get(0);
                return Optional.of(new Observation("INDEX",market.market(),date,"VALID",new DailyCandle(date,bar.open(),bar.high(),bar.low(),bar.close(),0),row.id(),index.collectedAt(),"REPORT_MARKET_SNAPSHOT"));
            }catch(RuntimeException e){return Optional.of(invalid("INDEX",market.market(),date,saved,"INVALID_SOURCE"));}
        }
        return Optional.empty();
    }
    private Observation invalid(String kind,String code,LocalDate date,Saved saved,String reason){return new Observation(kind,code,date,reason,null,saved.id(),saved.collected(),reason);}
    private DailyCandle candle(AnalysisInput input,LocalDate date){var matches=input.dailyCandles().stream().filter(c->c.businessDate().equals(date)).toList();if(matches.size()!=1)throw new IllegalArgumentException("BAR_MISSING_OR_DUPLICATE");return matches.get(0);}
    private boolean samePrices(DailyCandle a,DailyCandle b){return a.open().compareTo(b.open())==0 && a.high().compareTo(b.high())==0 && a.low().compareTo(b.low())==0 && a.close().compareTo(b.close())==0;}
    private AnalysisInput input(String value){try{return mapper.treeToValue(mapper.readTree(value).path("input"),AnalysisInput.class);}catch(Exception e){throw new IllegalArgumentException("INPUT_INVALID");}}
    private <T>T read(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("SOURCE_INVALID");}}
}
