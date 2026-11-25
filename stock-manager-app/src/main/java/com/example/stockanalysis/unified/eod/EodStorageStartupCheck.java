package com.example.stockanalysis.unified.eod;

import javax.sql.DataSource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Do not start scheduled analysis against snapshots temporarily held in the migration backup. */
@Component
public class EodStorageStartupCheck implements SmartInitializingSingleton {
    private final JdbcTemplate jdbc;
    private final com.example.stockanalysis.storage.SnapshotRepository snapshots;
    public EodStorageStartupCheck(DataSource source){this(source,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public EodStorageStartupCheck(DataSource source,com.example.stockanalysis.storage.SnapshotRepository snapshots){jdbc=new JdbcTemplate(source);this.snapshots=snapshots;}
    @Override public void afterSingletonsInstantiated(){
        var pending=jdbc.queryForObject("SELECT COUNT(*) FROM ua_eod_item WHERE input_sha256 IS NOT NULL AND input_json IS NULL AND input_gzip IS NULL"+(snapshots==null?"":" AND input_snapshot_id IS NULL"),Long.class);
        if(pending!=null&&pending>0)throw new IllegalStateException("EOD snapshot migration is incomplete: restore compressed snapshots before starting the server ("+pending+" pending)");
        if(snapshots!=null)snapshots.validateArchiveConfiguration();
    }
}
