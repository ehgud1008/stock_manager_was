package com.example.stockanalysis.storage;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcStorageUsageProbe implements StorageUsageProbe {
    private final JdbcTemplate jdbc;private final StorageProperties settings;
    public JdbcStorageUsageProbe(DataSource source,StorageProperties settings){jdbc=new JdbcTemplate(source);this.settings=settings;}
    public Usage measure(){
        String scope="USER_DATA_SEGMENTS";long bytes;
        // Autonomous Database exposes Oracle-maintained sample/system schemas whose allocations
        // must not be compared with the application's provisioned user-storage limit.
        try{bytes=jdbc.queryForObject("SELECT COALESCE(SUM(s.bytes),0) FROM dba_segments s JOIN dba_users u ON u.username=s.owner WHERE u.oracle_maintained='N'",Long.class);}
        catch(org.springframework.dao.DataAccessException e){bytes=jdbc.queryForObject("SELECT COALESCE(SUM(bytes),0) FROM user_segments",Long.class);scope="SCHEMA_SEGMENTS";}
        Long capacity=settings.capacityBytes()>0?settings.capacityBytes():null;
        if(capacity==null)try{String value=jdbc.queryForObject("SELECT property_value FROM database_properties WHERE property_name='MAX_PDB_STORAGE'",String.class);long parsed=Long.parseLong(value);if(parsed>0)capacity=parsed;}catch(RuntimeException ignored){}
        var top=jdbc.query("SELECT * FROM (SELECT CASE WHEN l.table_name IS NULL THEN s.segment_name ELSE l.table_name||'.'||l.column_name END object_name,s.segment_type,s.bytes FROM user_segments s LEFT JOIN user_lobs l ON l.segment_name=s.segment_name ORDER BY s.bytes DESC) WHERE ROWNUM<=10",
            (r,n)->new Consumer(r.getString(1),r.getString(2),r.getLong(3)));
        return new Usage(bytes,capacity,scope,top);
    }
}
