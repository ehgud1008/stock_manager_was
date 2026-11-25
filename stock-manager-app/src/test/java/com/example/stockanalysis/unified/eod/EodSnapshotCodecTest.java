package com.example.stockanalysis.unified.eod;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EodSnapshotCodecTest {
    @Test void blocksServerStartupUntilArchivedSnapshotsHaveBeenRestored(){
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:storage_startup_"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);
        jdbc.execute("CREATE TABLE ua_eod_item(input_json CLOB,input_gzip BLOB,input_sha256 VARCHAR(64))");
        var check=new EodStorageStartupCheck(source);
        check.afterSingletonsInstantiated();
        jdbc.update("INSERT INTO ua_eod_item(input_json) VALUES('{}')");
        check.afterSingletonsInstantiated();
        jdbc.update("UPDATE ua_eod_item SET input_json=NULL,input_sha256=?","a".repeat(64));
        assertThatThrownBy(check::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class).hasMessageContaining("1 pending");
        jdbc.update("UPDATE ua_eod_item SET input_gzip=?",new byte[]{1});
        check.afterSingletonsInstantiated();
    }
    @Test void roundTripsExactUnicodeJsonAndRejectsCorruptionOrMetadataMismatch(){
        String json="{\"종목\":\"하이트진로\",\"자료\":\""+"가격 이력\\n".repeat(10000)+"\"}";
        var encoded=EodSnapshotCodec.encode(json);
        assertThat(encoded.gzip().length).isLessThan(encoded.utf8Bytes()/10);
        assertThat(EodSnapshotCodec.decode(encoded.gzip(),encoded.sha256(),encoded.utf8Bytes())).isEqualTo(json);
        assertThatThrownBy(()->EodSnapshotCodec.decode(encoded.gzip(),"0".repeat(64),encoded.utf8Bytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->EodSnapshotCodec.decode(encoded.gzip(),encoded.sha256(),encoded.utf8Bytes()+1)).isInstanceOf(IllegalArgumentException.class);
        encoded.gzip()[encoded.gzip().length-5]^=1;
        assertThatThrownBy(()->EodSnapshotCodec.decode(encoded.gzip(),encoded.sha256(),encoded.utf8Bytes())).isInstanceOf(IllegalArgumentException.class);
    }
}
