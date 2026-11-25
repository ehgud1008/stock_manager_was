package com.example.stockanalysis.storage;

import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Small, resumable conversions. Original inline data is cleared only after a DB round trip. */
@Service
public class StorageMigration {
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final SnapshotRepository snapshots;
    public StorageMigration(DataSource source,SnapshotRepository snapshots){jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));this.snapshots=snapshots;}
    public int batch(int limit){
        int migrated=0,attempted=0;
        for(String kind:List.of("EOD_INPUT","UNIFIED_RESULT","SCREENER_INPUT")){
            while(attempted<limit){
                var keys=jdbc.query(keySql(kind),(r,n)->new String[]{r.getString(1),r.getString(2),r.getString(3)},System.currentTimeMillis());
                if(keys.isEmpty())break;String[] key=keys.get(0);
                attempted++;final boolean changed;
                try{changed=tx.execute(status->{
                    jdbc.queryForList("SELECT lock_id FROM ua_storage_lock WHERE lock_id=1 FOR UPDATE");
                    String table=table(kind),column=column(kind),reference=reference(kind);
                    var rows=jdbc.query("SELECT * FROM "+table+" WHERE run_id=? AND stock_code=? FOR UPDATE",(r,n)->{
                        if(r.getString(reference)!=null)return null;
                        String json=kind.equals("EOD_INPUT")?EodSnapshotCodec.read(r):r.getString(column);return json;
                    },key[0],key[1]);
                    if(rows.isEmpty()||rows.get(0)==null)return false;
                    String json=rows.get(0),id=snapshots.put(json,kind,Long.parseLong(key[2]));
                    if(!json.equals(snapshots.read(id)))throw new IllegalStateException("MIGRATION_DB_ROUNDTRIP_FAILED");
                    String clear=kind.equals("EOD_INPUT")?"input_json=NULL,input_gzip=NULL,input_sha256=?,input_utf8_bytes=?,input_snapshot_id=?":column+"=NULL,"+reference+"=?";
                    Object[] args=kind.equals("EOD_INPUT")?new Object[]{id,json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,id,key[0],key[1]}:new Object[]{id,key[0],key[1]};
                    return jdbc.update("UPDATE "+table+" SET "+clear+" WHERE run_id=? AND stock_code=? AND "+reference+" IS NULL",args)==1;
                });}catch(IllegalArgumentException|IllegalStateException e){
                    long retry=System.currentTimeMillis()+86400000;
                    int updated=jdbc.update("UPDATE ua_storage_migration_error SET error_code='SOURCE_NOT_VERIFIED',attempts=attempts+1,next_try_epoch=? WHERE kind=? AND run_id=? AND stock_code=?",retry,kind,key[0],key[1]);
                    if(updated==0)jdbc.update("INSERT INTO ua_storage_migration_error(kind,run_id,stock_code,error_code,next_try_epoch) VALUES(?,?,?,'SOURCE_NOT_VERIFIED',?)",kind,key[0],key[1],retry);continue;
                }
                if(changed){migrated++;jdbc.update("DELETE FROM ua_storage_migration_error WHERE kind=? AND run_id=? AND stock_code=?",kind,key[0],key[1]);}else break;
            }
            if(attempted>=limit)break;
        }return migrated;
    }
    private String keySql(String kind){
        String table=table(kind),column=column(kind),ref=reference(kind);
        String run=kind.equals("SCREENER_INPUT")?"sc_screening_run":"ua_scan_run";
        String epoch=kind.equals("EOD_INPUT")?"COALESCE(i.collected_epoch,r.created_epoch)":"r.created_epoch";
        String present=kind.equals("EOD_INPUT")?"(i.input_json IS NOT NULL OR i.input_gzip IS NOT NULL)":"i."+column+" IS NOT NULL";
        return "SELECT i.run_id,i.stock_code,"+epoch+" FROM "+table+" i JOIN "+run+" r ON r.run_id=i.run_id WHERE i."+ref+" IS NULL AND "+present+" AND r.status IN ('COMPLETED','COMPLETED_WITH_ERRORS','INTERRUPTED','FAILED') AND NOT EXISTS (SELECT 1 FROM ua_storage_migration_error m WHERE m.kind='"+kind+"' AND m.run_id=i.run_id AND m.stock_code=i.stock_code AND m.next_try_epoch>?) ORDER BY "+epoch+",i.run_id,i.stock_code FETCH FIRST 1 ROWS ONLY";
    }
    private String table(String kind){return kind.equals("SCREENER_INPUT")?"sc_screening_item":kind.equals("EOD_INPUT")?"ua_eod_item":"ua_scan_item";}
    private String column(String kind){return kind.equals("UNIFIED_RESULT")?"result_json":"input_json";}
    private String reference(String kind){return kind.equals("UNIFIED_RESULT")?"result_snapshot_id":"input_snapshot_id";}
}
