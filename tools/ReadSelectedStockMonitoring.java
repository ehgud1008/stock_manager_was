import com.example.stockanalysis.storage.*;
import com.example.stockanalysis.unified.eod.*;
import com.example.stockanalysis.unified.performance.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.core.io.DefaultResourceLoader;
import java.sql.*;
import java.time.*;

/** Execute the read-only monitoring view against persisted inputs, inside a read-only DB transaction. */
public class ReadSelectedStockMonitoring {
    public static void main(String[] args)throws Exception{
        var mapper=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"))){
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            var ds=new SingleConnectionDataSource(c,true);
            var settings=new StorageProperties(true,false,"",90,500,false,false,0,80,90,14,false,25);
            var snapshots=new SnapshotRepository(ds,mapper,settings);
            var sources=new PerformanceSources(ds,mapper,snapshots);
            var eod=new EodProperties("classpath:unified-calendar-2026.csv",Duration.ofHours(8),Duration.ofMinutes(10),Duration.ofSeconds(30),Duration.ofMinutes(90),Duration.ofHours(12),Duration.ofMinutes(1),3,Duration.ofMinutes(2));
            var calendar=new TradingCalendar(eod,new DefaultResourceLoader());
            var service=new ScenarioMonitoringService(sources,new ScenarioMonitoringEngine(),calendar,Clock.systemUTC());
            Object result=args.length>0&&args[0].equals("overview")
                ?new ScenarioMonitoringOverviewService(ds,sources,service,Clock.systemUTC()).list(null,null,"","ALL",0,20)
                :service.get("d3021de0-ca06-4f5b-92f2-b88b7877b284");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            c.rollback();
        }
    }
}
