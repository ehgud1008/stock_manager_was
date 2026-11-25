package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcStorageUsageProbeTest {
    static final long GIB=1024L*1024*1024;
    JdbcTemplate jdbc;DriverManagerDataSource ds;
    StorageProperties settings(){return new StorageProperties(true,false,"",90,500,true,true,0,80,90,14,false,25);}
    @BeforeEach void setup(){
        ds=new DriverManagerDataSource("jdbc:h2:mem:usage_"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE dba_segments(owner VARCHAR2(100),bytes NUMBER(19))");
        jdbc.execute("CREATE TABLE dba_users(username VARCHAR2(100),oracle_maintained VARCHAR2(1))");
        jdbc.execute("CREATE TABLE user_segments(segment_name VARCHAR2(100),segment_type VARCHAR2(30),bytes NUMBER(19))");
        jdbc.execute("CREATE TABLE user_lobs(segment_name VARCHAR2(100),table_name VARCHAR2(100),column_name VARCHAR2(100))");
        jdbc.execute("CREATE TABLE database_properties(property_name VARCHAR2(100),property_value VARCHAR2(100))");
        jdbc.update("INSERT INTO database_properties VALUES('MAX_PDB_STORAGE',?)",Long.toString(20*GIB));
        jdbc.update("INSERT INTO user_segments VALUES('APP_TABLE','TABLE',?)",7*GIB);
    }
    @Test void oracleMaintainedSamplesDoNotCauseAFalseCriticalAlert(){
        jdbc.update("INSERT INTO dba_users VALUES('ADMIN','N'),('OTHER_APP','N'),('SSB','Y'),('SYS','Y')");
        jdbc.update("INSERT INTO dba_segments VALUES('ADMIN',?),('OTHER_APP',?),('SSB',?),('SYS',?)",7*GIB,GIB,162*GIB,3*GIB);
        var usage=new JdbcStorageUsageProbe(ds,settings()).measure();
        assertThat(usage.scope()).isEqualTo("USER_DATA_SEGMENTS");assertThat(usage.allocatedBytes()).isEqualTo(8*GIB);
        assertThat(usage.capacityBytes()).isEqualTo(20*GIB);
        var assessment=StorageHealth.assess(usage,null,0,settings());
        assertThat(assessment.level()).isEqualTo("NORMAL");assertThat(assessment.allocatedPercent()).isEqualTo(40.0);
        assertThat(usage.consumers()).singleElement().satisfies(row->assertThat(row.object()).isEqualTo("APP_TABLE"));
    }
    @Test void unavailableAdminViewsFallBackToTheExplicitSchemaScope(){
        jdbc.execute("DROP TABLE dba_users");
        var usage=new JdbcStorageUsageProbe(ds,settings()).measure();
        assertThat(usage.scope()).isEqualTo("SCHEMA_SEGMENTS");assertThat(usage.allocatedBytes()).isEqualTo(7*GIB);
        assertThat(StorageHealth.assess(usage,null,0,settings()).level()).isEqualTo("NORMAL");
    }
    @Test void anUnknownLimitRemainsUnknownInsteadOfAssumingZeroOrFull(){
        jdbc.update("INSERT INTO dba_users VALUES('ADMIN','N')");jdbc.update("INSERT INTO dba_segments VALUES('ADMIN',?)",7*GIB);
        jdbc.update("DELETE FROM database_properties");
        var usage=new JdbcStorageUsageProbe(ds,settings()).measure();
        assertThat(usage.capacityBytes()).isNull();assertThat(StorageHealth.assess(usage,null,0,settings()).level()).isEqualTo("UNKNOWN");
    }
}
