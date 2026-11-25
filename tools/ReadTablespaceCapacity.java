import java.sql.*;

/** Read-only file allocation, free extent, and quota audit. */
public class ReadTablespaceCapacity {
    static void query(Connection c,String label,String sql){
        System.out.println("CHECK="+label);
        try(var s=c.createStatement()){s.setQueryTimeout(40);try(var r=s.executeQuery(sql)){var m=r.getMetaData();while(r.next()){var line=new StringBuilder();for(int i=1;i<=m.getColumnCount();i++){if(i>1)line.append(" | ");line.append(m.getColumnLabel(i)).append('=').append(r.getString(i));}System.out.println(line);}}}
        catch(SQLException e){System.out.println("SQL_ERROR vendorCode="+e.getErrorCode()+" state="+e.getSQLState());}
    }
    public static void main(String[] args) throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"), System.getenv("TASK_REPORT_USER"), System.getenv("TASK_REPORT_PASSWORD"))) {
            c.setAutoCommit(false);
            try (var s = c.createStatement()) { s.execute("SET TRANSACTION READ ONLY"); }
            query(c, "FILES", "SELECT d.tablespace_name, ROUND(SUM(d.bytes)/POWER(1024,3),4) allocated_gib, ROUND(SUM(CASE WHEN d.autoextensible='YES' THEN d.maxbytes ELSE d.bytes END)/POWER(1024,3),4) file_max_gib, MAX(d.autoextensible) autoextensible FROM dba_data_files d GROUP BY d.tablespace_name ORDER BY allocated_gib DESC");
            query(c, "TEMP", "SELECT tablespace_name,ROUND(SUM(bytes)/POWER(1024,3),4) allocated_gib FROM dba_temp_files GROUP BY tablespace_name");
            query(c, "SAMPLE_OWNERS", "SELECT s.owner,u.oracle_maintained,ROUND(SUM(s.bytes)/POWER(1024,3),4) segment_gib FROM dba_segments s JOIN dba_users u ON u.username=s.owner WHERE s.tablespace_name='SAMPLESCHEMA' GROUP BY s.owner,u.oracle_maintained");
            query(c, "QUOTAS", "SELECT username,tablespace_name,bytes,max_bytes FROM dba_ts_quotas WHERE tablespace_name='DATA' AND dropped='NO'");
            c.rollback();
        }
    }
}

