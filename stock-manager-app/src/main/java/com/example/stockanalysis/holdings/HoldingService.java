package com.example.stockanalysis.holdings;

import static com.example.stockanalysis.holdings.HoldingModels.*;
import com.example.stockanalysis.shared.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Single-owner aggregate balances. Frozen contexts survive edits and worker restarts. */
@Service
public class HoldingService {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper; private final Clock clock; private final String owner;
    private final Map<String,Snapshot> batches=new LinkedHashMap<>();
    public HoldingService(JdbcTemplate jdbc,ObjectMapper mapper,Clock clock,
                          @Value("${trade-decision.owner-scope:local-owner}") String owner) {
        this.jdbc=jdbc;this.mapper=mapper;this.clock=clock;this.owner=owner;
    }
    private Holding row(java.sql.ResultSet rs,int index)throws java.sql.SQLException {
        var bought=rs.getDate("purchased_on");
        return new Holding(rs.getString("stock_code"),rs.getString("stock_name"),rs.getBigDecimal("quantity"),rs.getBigDecimal("average_buy_price"),
                bought==null?null:bought.toLocalDate(),rs.getDate("balance_date").toLocalDate(),rs.getBigDecimal("stop_price"),rs.getBigDecimal("target_price"),rs.getLong("revision"));
    }
    public List<Holding> list() {return jdbc.query("SELECT * FROM ph_holding WHERE owner_scope=? ORDER BY stock_code",this::row,owner);}
    private Snapshot read(String code) {
        long started=System.nanoTime();
        try {
            var rows=code==null?list():jdbc.query("SELECT * FROM ph_holding WHERE owner_scope=? AND stock_code=?",this::row,owner,code);
            var map=new LinkedHashMap<String,Holding>();for(var h:rows)map.put(h.stockCode(),h);
            return new Snapshot(clock.instant(),"AVAILABLE",map);
        } catch(DataAccessException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Holding lookup unavailable: type={}",e.getClass().getSimpleName());
            return Snapshot.unknown(clock.instant());
        } finally {org.slf4j.LoggerFactory.getLogger(getClass()).info("Holding lookup: scope={}, elapsedMs={}",code==null?"BATCH":"SINGLE",(System.nanoTime()-started)/1_000_000.0);}
    }
    public Snapshot single(String code) {return read(code);}
    /** At most eight active/recent runs. No holdings query per item. */
    public synchronized Snapshot batch(String runKey) {
        if(batches.containsKey(runKey))return batches.get(runKey);
        Snapshot snapshot;
        try {
            var saved=jdbc.queryForList("SELECT context_json FROM ph_analysis_context WHERE owner_scope=? AND context_key=?",String.class,owner,runKey);
            if(!saved.isEmpty())snapshot=decode(saved.get(0),Snapshot.class);
            else {
                snapshot=read(null);
                try {save(runKey,snapshot);} catch(DuplicateKeyException e) {
                    snapshot=decode(jdbc.queryForObject("SELECT context_json FROM ph_analysis_context WHERE owner_scope=? AND context_key=?",String.class,owner,runKey),Snapshot.class);
                }
            }
        } catch(DataAccessException e) {snapshot=Snapshot.unknown(clock.instant());}
        batches.put(runKey,snapshot);if(batches.size()>8)batches.remove(batches.keySet().iterator().next());
        return snapshot;
    }
    public void save(String key,Object value) {
        jdbc.update("INSERT INTO ph_analysis_context(owner_scope,context_key,context_json) VALUES (?,?,?)",owner,key,encode(value));
    }
    public Evaluation evaluation(String key) {
        try {var rows=jdbc.queryForList("SELECT context_json FROM ph_analysis_context WHERE owner_scope=? AND context_key=?",String.class,owner,key);
            return rows.isEmpty()?null:decode(rows.get(0),Evaluation.class);
        }catch(DataAccessException e){return null;}
    }
    public Holding update(String code,Edit edit) {
        var today=LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        if(!code.matches("[0-9]{6}") || edit.quantity()==null || edit.quantity().signum()<0 || edit.quantity().stripTrailingZeros().scale()>0
            || edit.balanceDate()==null || edit.balanceDate().isAfter(today) || edit.quantity().signum()>0 && (edit.averageBuyPrice()==null || edit.averageBuyPrice().signum()<=0)
            || edit.purchasedOn()!=null && edit.purchasedOn().isAfter(edit.balanceDate())) throw new BusinessException(ErrorCode.INVALID_REQUEST,"수량·평균단가·잔고 기준일을 확인하세요.");
        var quantity=edit.quantity();var price=quantity.signum()==0?null:edit.averageBuyPrice();
        Object bought=quantity.signum()==0?null:edit.purchasedOn();
        if(edit.expectedRevision()==0) {
            try {jdbc.update("INSERT INTO ph_holding(owner_scope,stock_code,stock_name,quantity,average_buy_price,purchased_on,balance_date,stop_price,target_price,revision) VALUES (?,?,?,?,?,?,?,?,?,1)",
                    owner,code,edit.stockName().strip(),quantity,price,bought,edit.balanceDate(),edit.stopPrice(),edit.targetPrice());}
            catch(DuplicateKeyException e){throw conflict();}
        } else {
            int n=jdbc.update("UPDATE ph_holding SET stock_name=?,quantity=?,average_buy_price=?,purchased_on=?,balance_date=?,stop_price=?,target_price=?,revision=revision+1 WHERE owner_scope=? AND stock_code=? AND revision=?",
                    edit.stockName().strip(),quantity,price,bought,edit.balanceDate(),edit.stopPrice(),edit.targetPrice(),owner,code,edit.expectedRevision());
            if(n!=1)throw conflict();
        }
        return jdbc.queryForObject("SELECT * FROM ph_holding WHERE owner_scope=? AND stock_code=?",this::row,owner,code);
    }
    private BusinessException conflict(){return new BusinessException(ErrorCode.DECISION_CONFLICT,"다른 화면에서 보유정보가 변경되었습니다. 새로고침 후 다시 저장하세요.");}
    private String encode(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Holding snapshot encoding failed",e);}}
    private <T>T decode(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalStateException("Holding snapshot decoding failed",e);}}
}
