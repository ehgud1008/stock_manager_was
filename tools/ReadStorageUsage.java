import com.example.stockanalysis.storage.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.*;

/** Measure and assess only. Never records a sample or sends a notification. */
public class ReadStorageUsage {
    public static void main(String[] args)throws Exception{
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            var ds=new SingleConnectionDataSource(c,true);
            var settings=new StorageProperties(true,false,"",90,500,true,true,0,80,90,14,false,25);
            var usage=new JdbcStorageUsageProbe(ds,settings).measure();
            var result=java.util.Map.of("usage",usage,"assessment",StorageHealth.assess(usage,null,0,settings));
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));c.rollback();
        }
    }
}
