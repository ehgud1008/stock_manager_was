import java.sql.*;
import java.nio.file.*;
public class InspectVinatech {
 public static void main(String[] args)throws Exception {
  try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),System.getenv("TASK_REPORT_USER"),System.getenv("TASK_REPORT_PASSWORD"));
      var s=c.prepareStatement("SELECT r.base_date,s.current_price FROM ua_scan_item s JOIN ua_scan_run r ON r.run_id=s.run_id WHERE s.stock_code='126340' AND s.status='COMPLETED' AND r.base_date=DATE '2026-10-22' ORDER BY r.created_epoch DESC FETCH FIRST 1 ROWS ONLY");var r=s.executeQuery()) {
   if(r.next()){System.out.println("FOUND "+r.getDate(1)+" PRICE="+r.getBigDecimal(2));}else System.out.println("NO_SAVED_RESULT");
  }
 }
}
