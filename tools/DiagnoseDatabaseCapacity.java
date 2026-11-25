import java.sql.*;

/** Read-only capacity audit: metadata, byte counts and maintenance state; no payloads or credentials. */
public class DiagnoseDatabaseCapacity {
    static void query(Connection c,String label,String sql){
        System.out.println("CHECK="+label);
        try(var s=c.createStatement()){s.setQueryTimeout(40);try(var r=s.executeQuery(sql)){var m=r.getMetaData();while(r.next()){var line=new StringBuilder();for(int i=1;i<=m.getColumnCount();i++){if(i>1)line.append(" | ");line.append(m.getColumnLabel(i)).append('=').append(r.getString(i));}System.out.println(line);}}}
        catch(SQLException e){System.out.println("SQL_ERROR vendorCode="+e.getErrorCode()+" state="+e.getSQLState());}
    }
    public static void main(String[] args)throws Exception{
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            query(c,"SCHEMA_ALLOCATED","SELECT SUM(bytes) allocated_bytes FROM user_segments");
            query(c,"PDB_ALLOCATED","SELECT SUM(bytes) allocated_bytes FROM dba_segments");
            query(c,"SEGMENTS_BY_SCOPE","SELECT u.oracle_maintained,s.tablespace_name,SUM(s.bytes) allocated_bytes FROM dba_segments s JOIN dba_users u ON u.username=s.owner GROUP BY u.oracle_maintained,s.tablespace_name ORDER BY allocated_bytes DESC");
            query(c,"TOP_OWNERS","SELECT * FROM (SELECT s.owner,u.oracle_maintained,SUM(s.bytes) allocated_bytes FROM dba_segments s JOIN dba_users u ON u.username=s.owner GROUP BY s.owner,u.oracle_maintained ORDER BY allocated_bytes DESC) WHERE ROWNUM<=12");
            query(c,"TABLESPACE_USAGE","SELECT tablespace_name,used_space,tablespace_size,used_percent FROM dba_tablespace_usage_metrics");
            query(c,"STORAGE_PROPERTIES","SELECT property_name,property_value FROM database_properties WHERE property_name LIKE '%STORAGE%'");
            query(c,"PDB_DATAFILES","SELECT SUM(bytes) datafile_bytes,SUM(maxbytes) max_bytes FROM dba_data_files");
            query(c,"FREE_EXTENTS","SELECT tablespace_name,SUM(bytes) free_bytes FROM dba_free_space GROUP BY tablespace_name");
            query(c,"TOP_SEGMENTS","SELECT * FROM (SELECT s.segment_name,s.segment_type,s.bytes,l.table_name,l.column_name FROM user_segments s LEFT JOIN user_lobs l ON l.segment_name=s.segment_name ORDER BY s.bytes DESC) WHERE ROWNUM<=20");
            query(c,"EOD_PAYLOADS","SELECT COUNT(*) total_rows,SUM(CASE WHEN input_json IS NOT NULL THEN 1 ELSE 0 END) legacy_rows,SUM(CASE WHEN input_gzip IS NOT NULL THEN 1 ELSE 0 END) gzip_rows,SUM(CASE WHEN input_snapshot_id IS NOT NULL THEN 1 ELSE 0 END) shared_rows,SUM(DBMS_LOB.GETLENGTH(input_json)) clob_characters,SUM(DBMS_LOB.GETLENGTH(input_gzip)) gzip_bytes FROM ua_eod_item");
            query(c,"SHARED_OBJECTS","SELECT COUNT(*) objects,SUM(utf8_bytes) original_bytes,SUM(DBMS_LOB.GETLENGTH(payload_gzip)) gzip_bytes,MIN(created_epoch) first_epoch,MAX(created_epoch) last_epoch FROM ua_snapshot_object");
            query(c,"SHARED_DOCUMENTS","SELECT kind,COUNT(*) documents FROM ua_snapshot_document GROUP BY kind");
            query(c,"SCAN_PAYLOADS","SELECT COUNT(*) rows_total,SUM(CASE WHEN result_json IS NOT NULL THEN 1 ELSE 0 END) inline_rows,SUM(CASE WHEN result_snapshot_id IS NOT NULL THEN 1 ELSE 0 END) shared_rows,SUM(DBMS_LOB.GETLENGTH(result_json)) inline_characters FROM ua_scan_item");
            query(c,"STATE","SELECT last_level,last_error,last_archive_health,archived_objects,last_archive_epoch FROM ua_storage_state");
            query(c,"RECENT_SAMPLES","SELECT * FROM (SELECT sample_date,measured_epoch,allocated_bytes,capacity_bytes,measurement_scope,severity FROM ua_storage_sample ORDER BY sample_date DESC) WHERE ROWNUM<=5");
            query(c,"NOTICES","SELECT severity,status,error_code,COUNT(*) count_notices FROM ua_storage_notice GROUP BY severity,status,error_code");
            query(c,"ACTIVE_INSTANCES","SELECT instance_id,archive_store_id,last_seen_epoch,archive_enabled FROM ua_storage_instance ORDER BY last_seen_epoch DESC");
            query(c,"MIGRATION_ERRORS","SELECT kind,error_code,COUNT(*) count_errors FROM ua_storage_migration_error GROUP BY kind,error_code");
            query(c,"RECENT_RUNS","SELECT * FROM (SELECT base_date,status,error_code,created_epoch,completed_epoch FROM ua_scan_run WHERE market='ALL' ORDER BY created_epoch DESC) WHERE ROWNUM<=5");
            c.rollback();
        }
    }
}
