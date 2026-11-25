import java.sql.*;
public class InspectEodStorage {
  static void query(Connection c,String label,String sql) {
    System.out.println("QUERY "+label);
    try(var s=c.createStatement()){s.setQueryTimeout(90);try(var r=s.executeQuery(sql)){
      var m=r.getMetaData();while(r.next()){var line=new StringBuilder();for(int i=1;i<=m.getColumnCount();i++){if(i>1)line.append(" | ");line.append(m.getColumnLabel(i)).append('=').append(r.getString(i));}System.out.println(line);}
    }}catch(SQLException e){System.out.println("UNAVAILABLE code="+e.getErrorCode());}
  }
  public static void main(String[] args)throws Exception {
    try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
      c.setAutoCommit(false);
      try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
      query(c,"SEGMENTS", "SELECT * FROM (SELECT s.segment_name,s.segment_type,s.bytes, l.table_name,l.column_name FROM user_segments s LEFT JOIN user_lobs l ON l.segment_name=s.segment_name ORDER BY s.bytes DESC) WHERE ROWNUM<=15");
      query(c,"SCHEMA_TOTAL", "SELECT SUM(bytes) allocated_bytes FROM user_segments");
      query(c,"LOB_STORAGE", "SELECT table_name,column_name,segment_name,securefile,compression,deduplication,retention FROM user_lobs WHERE table_name IN ('UA_EOD_ITEM','UA_SCAN_ITEM','UA_CANDIDATE_EVIDENCE')");
      query(c,"QUOTA", "SELECT tablespace_name,bytes,max_bytes FROM user_ts_quotas");
      query(c,"PDB_SIZE", "SELECT name,total_size FROM v$pdbs");
      query(c,"EOD_BY_DATE", "SELECT r.base_date,COUNT(DISTINCT i.run_id) runs,COUNT(*) rows_total,SUM(CASE WHEN i.input_json IS NOT NULL THEN 1 ELSE 0 END) snapshots,SUM(DBMS_LOB.GETLENGTH(i.input_json)) json_characters,ROUND(AVG(DBMS_LOB.GETLENGTH(i.input_json))) avg_chars,MAX(DBMS_LOB.GETLENGTH(i.input_json)) max_chars FROM ua_eod_item i JOIN ua_scan_run r ON r.run_id=i.run_id GROUP BY r.base_date ORDER BY r.base_date");
      query(c,"STORAGE_PROPERTIES", "SELECT property_name,property_value FROM database_properties WHERE property_name LIKE '%STORAGE%' OR property_name LIKE '%SIZE%'");
      c.rollback();
    }
  }
}
