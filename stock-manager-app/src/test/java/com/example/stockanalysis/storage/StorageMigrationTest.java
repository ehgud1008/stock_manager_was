package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class StorageMigrationTest {
    @Test void preservesUnverifiedInputAndContinuesWithOtherCompletedStocks()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);
        jdbc.update("INSERT INTO ua_scan_run(run_id,request_key,market,base_date,engine_version,status,created_epoch) VALUES('r','r','ALL',DATE '2026-10-20','v','COMPLETED',1)");
        for(String code:java.util.List.of("000001","000002")){
            jdbc.update("INSERT INTO ua_scan_item(run_id,stock_code,stock_name,market,status) VALUES('r',?,'test','KOSPI','COMPLETED')",code);
            jdbc.update("INSERT INTO ua_eod_item(run_id,stock_code,input_json) VALUES('r',?,?)",code,code.equals("000001")?"not-json":"{\"input\":{\"unchanged\":1.2300}}");
        }
        var repo=new SnapshotRepository(ds,StorageFixtures.JSON,StorageFixtures.settings("",false,false));var migration=new StorageMigration(ds,repo);
        assertThat(migration.batch(10)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT input_json FROM ua_eod_item WHERE stock_code='000001'",String.class)).isEqualTo("not-json");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_migration_error",Long.class)).isEqualTo(1);
        String id=jdbc.queryForObject("SELECT input_snapshot_id FROM ua_eod_item WHERE stock_code='000002'",String.class);
        assertThat(repo.read(id)).isEqualTo("{\"input\":{\"unchanged\":1.2300}}");
        assertThat(migration.batch(10)).isZero();
    }
}
