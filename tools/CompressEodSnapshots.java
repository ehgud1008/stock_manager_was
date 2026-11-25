import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import java.sql.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

/** Offline, resumable migration. Every CLOB is backed up and verified before clearing it. */
public class CompressEodSnapshots {
  static final Path BACKUP=Path.of("eod-storage-backup","20261021");
  static final String OWNER="eod-compression-20261021";
  record Row(String run,String code,String json,EodSnapshotCodec.Encoded encoded){}
  static void lockMigration(Connection c)throws SQLException{
    try(var s=c.createStatement();var r=s.executeQuery("SELECT owner_token,expires_epoch FROM ua_eod_lease WHERE lease_id=1 FOR UPDATE")){
      if(!r.next())throw new IllegalStateException("LEASE_MISSING");
      if(r.getString(1)!=null&&!OWNER.equals(r.getString(1))&&r.getLong(2)>System.currentTimeMillis())throw new IllegalStateException("STOP_EOD_WORKER_FIRST");
    }
    try(var s=c.prepareStatement("UPDATE ua_eod_lease SET owner_token=?,expires_epoch=?,heartbeat_epoch=? WHERE lease_id=1")){
      s.setString(1,OWNER);s.setLong(2,System.currentTimeMillis()+7200000);s.setLong(3,System.currentTimeMillis());s.executeUpdate();
    }
  }
  static long scalar(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
  static void stats(Connection c)throws SQLException{
    System.out.println("DB_VERSION="+c.getMetaData().getDatabaseMajorVersion());
    System.out.println("LEGACY="+scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_json IS NOT NULL"));
    System.out.println("SCHEMA_ALLOCATED="+scalar(c,"SELECT SUM(bytes) FROM user_segments"));
    System.out.println("ARCHIVE_PENDING="+scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_sha256 IS NOT NULL AND input_json IS NULL AND input_gzip IS NULL"));
    System.out.println("GZIP_ROWS="+scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_gzip IS NOT NULL"));
    System.out.println("ORIGINAL_UTF8_BYTES="+scalar(c,"SELECT SUM(input_utf8_bytes) FROM ua_eod_item WHERE input_gzip IS NOT NULL"));
    System.out.println("GZIP_PAYLOAD_BYTES="+scalar(c,"SELECT SUM(DBMS_LOB.GETLENGTH(input_gzip)) FROM ua_eod_item WHERE input_gzip IS NOT NULL"));
    try(var s=c.createStatement();var r=s.executeQuery("SELECT owner_token,expires_epoch FROM ua_eod_lease WHERE lease_id=1")){while(r.next())System.out.println("LEASE_OWNER="+r.getString(1)+" EXPIRES="+r.getLong(2)+" NOW="+System.currentTimeMillis());}
    try(var s=c.createStatement();var r=s.executeQuery("SELECT l.column_name,s.bytes FROM user_lobs l JOIN user_segments s ON s.segment_name=l.segment_name WHERE l.table_name='UA_EOD_ITEM'")){while(r.next())System.out.println(r.getString(1)+"_ALLOCATED="+r.getLong(2));}
  }
  static void backup(Row row)throws Exception{
    if(!row.run.matches("[a-f0-9-]{36}")||!row.code.matches("[A-Za-z0-9]{6}"))throw new IllegalStateException("INVALID_KEY");
    var dir=BACKUP.resolve(row.run);Files.createDirectories(dir);
    var path=dir.resolve(row.code+".json.gz");
    if(!Files.exists(path)){
      var temp=dir.resolve(row.code+".tmp");
      try(var file=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
        var buffer=ByteBuffer.wrap(row.encoded.gzip());while(buffer.hasRemaining())file.write(buffer);file.force(true);
      }
      Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE);
    }
    String restored=EodSnapshotCodec.decode(Files.readAllBytes(path),row.encoded.sha256(),row.encoded.utf8Bytes());
    if(!restored.equals(row.json))throw new IllegalStateException("BACKUP_MISMATCH");
    var manifest=dir.resolve(row.code+".sha256");
    try(var file=FileChannel.open(manifest,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
      var buffer=ByteBuffer.wrap((row.encoded.sha256()+" "+row.encoded.utf8Bytes()+"\n").getBytes(StandardCharsets.US_ASCII));while(buffer.hasRemaining())file.write(buffer);file.force(true);
    }
  }
  static void migrate(Connection c,boolean pilot)throws Exception{
    Files.createDirectories(BACKUP);
    if(Files.getFileStore(BACKUP).getUsableSpace()<2L*1024*1024*1024)throw new IllegalStateException("BACKUP_DISK_SPACE_LOW");
    c.setAutoCommit(false);long total=0,raw=0,zipped=0;
    try {
      while(true){
        lockMigration(c);
        var rows=new ArrayList<Row>();
        try(var s=c.createStatement()){
          s.setFetchSize(25);
          try(var r=s.executeQuery("SELECT run_id,stock_code,input_json FROM ua_eod_item WHERE input_json IS NOT NULL AND ROWNUM<=25 FOR UPDATE")){
            while(r.next()){String json=r.getString(3);rows.add(new Row(r.getString(1),r.getString(2),json,EodSnapshotCodec.encode(json)));}
          }
        }
        if(rows.isEmpty()){c.rollback();break;}
        for(var row:rows)backup(row);
        try(var s=c.prepareStatement("UPDATE ua_eod_item SET input_gzip=?,input_sha256=?,input_utf8_bytes=?,input_json=NULL WHERE run_id=? AND stock_code=? AND input_json IS NOT NULL")){
          for(var row:rows){s.setBinaryStream(1,new ByteArrayInputStream(row.encoded.gzip()),row.encoded.gzip().length);s.setString(2,row.encoded.sha256());s.setInt(3,row.encoded.utf8Bytes());s.setString(4,row.run);s.setString(5,row.code);s.addBatch();}
          for(int count:s.executeBatch())if(count!=1&&count!=Statement.SUCCESS_NO_INFO)throw new IllegalStateException("UPDATE_COUNT");
        }
        String predicate=String.join(" OR ",Collections.nCopies(rows.size(),"(run_id=? AND stock_code=?)"));
        var expected=new HashMap<String,Row>();for(var row:rows)expected.put(row.run+row.code,row);
        try(var s=c.prepareStatement("SELECT run_id,stock_code,input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE "+predicate)){
          int parameter=1;for(var row:rows){s.setString(parameter++,row.run);s.setString(parameter++,row.code);}s.setFetchSize(25);
          try(var r=s.executeQuery()){while(r.next()){
            var row=expected.remove(r.getString(1)+r.getString(2));
            if(row==null||r.getString("input_json")!=null||!row.json.equals(EodSnapshotCodec.read(r)))throw new IllegalStateException("DATABASE_ROUNDTRIP_MISMATCH");
          }}
        }
        if(!expected.isEmpty())throw new IllegalStateException("MISSING_UPDATED_ROW");
        c.commit();for(var row:rows){total++;raw+=row.encoded.utf8Bytes();zipped+=row.encoded.gzip().length;}
        if(total%100==0)System.out.println("MIGRATED="+total+" RAW="+raw+" GZIP="+zipped);
        if(pilot)break;
      }
      System.out.println("COMPLETE migrated="+total+" raw="+raw+" gzip="+zipped);
    }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
  }
  static void verify(Connection c)throws Exception{
    int count=0;long bytes=0;
    try(var s=c.createStatement()){
      s.setFetchSize(25);
      try(var r=s.executeQuery("SELECT run_id,stock_code,input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE input_gzip IS NOT NULL")){
        while(r.next()){
          String restored=EodSnapshotCodec.read(r);
          var backup=BACKUP.resolve(r.getString(1)).resolve(r.getString(2)+".json.gz");
          if(!Files.exists(backup)||!restored.equals(EodSnapshotCodec.decode(Files.readAllBytes(backup),r.getString("input_sha256"),r.getLong("input_utf8_bytes"))))throw new IllegalStateException("BACKUP_VERIFY_FAILED");
          bytes+=r.getLong("input_utf8_bytes");if(++count%500==0)System.out.println("VERIFIED="+count);
        }
      }
    }
    System.out.println("VERIFIED_ALL="+count+" UTF8_BYTES="+bytes);
  }
  static void archive(Connection c,boolean pilot)throws Exception{
    Files.createDirectories(BACKUP);
    if(Files.getFileStore(BACKUP).getUsableSpace()<2L*1024*1024*1024)throw new IllegalStateException("BACKUP_DISK_SPACE_LOW");
    c.setAutoCommit(false);long total=0;
    try{
      while(true){
        lockMigration(c);
        var rows=new ArrayList<Row>();
        try(var s=c.createStatement()){s.setFetchSize(25);try(var r=s.executeQuery("SELECT run_id,stock_code,input_json FROM ua_eod_item WHERE input_json IS NOT NULL AND ROWNUM<=25 FOR UPDATE")){
          while(r.next()){String json=r.getString(3);rows.add(new Row(r.getString(1),r.getString(2),json,EodSnapshotCodec.encode(json)));}
        }}
        if(rows.isEmpty()){c.rollback();break;}
        for(var row:rows)backup(row);
        try(var s=c.prepareStatement("UPDATE ua_eod_item SET input_json=NULL,input_sha256=?,input_utf8_bytes=? WHERE run_id=? AND stock_code=? AND input_gzip IS NULL AND input_json IS NOT NULL")){
          for(var row:rows){s.setString(1,row.encoded.sha256());s.setInt(2,row.encoded.utf8Bytes());s.setString(3,row.run);s.setString(4,row.code);s.addBatch();}
          for(int count:s.executeBatch())if(count!=1&&count!=Statement.SUCCESS_NO_INFO)throw new IllegalStateException("ARCHIVE_CONCURRENT_CHANGE");
        }
        c.commit();total+=rows.size();if(total%100==0)System.out.println("ARCHIVED="+total);if(pilot)break;
      }
      System.out.println("ARCHIVE_COMPLETE="+total);
    }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
  }
  static void restoreCompressed(Connection c)throws Exception{
    c.setAutoCommit(false);long total=0;
    try{
      while(true){
        lockMigration(c);
        var rows=new ArrayList<Row>();
        try(var s=c.createStatement()){s.setFetchSize(25);try(var r=s.executeQuery("SELECT run_id,stock_code,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE input_json IS NULL AND input_gzip IS NULL AND input_sha256 IS NOT NULL AND ROWNUM<=25 FOR UPDATE")){
          while(r.next()){
            String run=r.getString(1),code=r.getString(2);byte[] gzip=Files.readAllBytes(BACKUP.resolve(run).resolve(code+".json.gz"));
            String json=EodSnapshotCodec.decode(gzip,r.getString(3),r.getLong(4));
            rows.add(new Row(run,code,json,new EodSnapshotCodec.Encoded(gzip,r.getString(3),(int)r.getLong(4))));
          }
        }}
        if(rows.isEmpty()){c.rollback();break;}
        try(var s=c.prepareStatement("UPDATE ua_eod_item SET input_gzip=? WHERE run_id=? AND stock_code=? AND input_json IS NULL AND input_gzip IS NULL AND input_sha256=?")){
          for(var row:rows){s.setBinaryStream(1,new ByteArrayInputStream(row.encoded.gzip()),row.encoded.gzip().length);s.setString(2,row.run);s.setString(3,row.code);s.setString(4,row.encoded.sha256());s.addBatch();}
          for(int count:s.executeBatch())if(count!=1&&count!=Statement.SUCCESS_NO_INFO)throw new IllegalStateException("RESTORE_CONCURRENT_CHANGE");
        }
        String predicate=String.join(" OR ",Collections.nCopies(rows.size(),"(run_id=? AND stock_code=?)"));
        var expected=new HashMap<String,String>();for(var row:rows)expected.put(row.run+row.code,row.json);
        try(var s=c.prepareStatement("SELECT run_id,stock_code,input_json,input_gzip,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE "+predicate)){
          int p=1;for(var row:rows){s.setString(p++,row.run);s.setString(p++,row.code);}s.setFetchSize(25);
          try(var r=s.executeQuery()){while(r.next())if(!EodSnapshotCodec.read(r).equals(expected.remove(r.getString(1)+r.getString(2))))throw new IllegalStateException("RESTORE_ROUNDTRIP_MISMATCH");}
        }
        if(!expected.isEmpty())throw new IllegalStateException("MISSING_RESTORE");
        c.commit();total+=rows.size();if(total%100==0)System.out.println("RESTORED="+total);
      }
      System.out.println("RESTORE_COMPLETE="+total);
    }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
  }
  public static void main(String[] args)throws Exception{
    if(args.length!=1)throw new IllegalArgumentException("Use --prepare, --migrate, --verify, --shrink, --stats");
    var properties=new Properties();properties.setProperty("user",System.getenv("TASK_REPORT_USER"));properties.setProperty("password",System.getenv("TASK_REPORT_PASSWORD"));properties.setProperty("oracle.jdbc.defaultLobPrefetchSize","1048576");
    try(var c=DriverManager.getConnection(System.getenv("TASK_REPORT_JDBC"),properties)){
      switch(args[0]){
        case "--prepare" -> {
          long columns=scalar(c,"SELECT COUNT(*) FROM user_tab_columns WHERE table_name='UA_EOD_ITEM' AND column_name IN ('INPUT_GZIP','INPUT_SHA256','INPUT_UTF8_BYTES')");
          if(columns==0)try(var s=c.createStatement()){s.execute("ALTER TABLE ua_eod_item ADD (input_gzip BLOB,input_sha256 VARCHAR2(64),input_utf8_bytes NUMBER(19))");}
          else if(columns!=3)throw new IllegalStateException("PARTIAL_SCHEMA");
          if(scalar(c,"SELECT COUNT(*) FROM user_constraints WHERE constraint_name='CK_UA_EOD_GZIP'")==0)
            try(var s=c.createStatement()){s.execute("ALTER TABLE ua_eod_item ADD CONSTRAINT ck_ua_eod_gzip CHECK (input_gzip IS NULL OR (input_sha256 IS NOT NULL AND input_utf8_bytes IS NOT NULL AND input_utf8_bytes>=0))");}
          System.out.println("SCHEMA_READY");stats(c);
        }
        case "--migrate" -> migrate(c,false);
        case "--pilot" -> migrate(c,true);
        case "--archive-pilot" -> archive(c,true);
        case "--archive" -> archive(c,false);
        case "--restore-compressed" -> restoreCompressed(c);
        case "--reclaim-empty-clob" -> {
          boolean hasClob=scalar(c,"SELECT COUNT(*) FROM user_tab_columns WHERE table_name='UA_EOD_ITEM' AND column_name='INPUT_JSON'")>0;
          if(hasClob&&scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_json IS NOT NULL")!=0)throw new IllegalStateException("CLOB_NOT_EMPTY");
          long pending=scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_sha256 IS NOT NULL AND input_gzip IS NULL");
          if(pending==0)throw new IllegalStateException("NO_ARCHIVED_ROWS");
          // Inspect every archive before replacing the now-empty LOB segment.
          long checked=0;
          try(var s=c.createStatement();var r=s.executeQuery("SELECT run_id,stock_code,input_sha256,input_utf8_bytes FROM ua_eod_item WHERE input_sha256 IS NOT NULL AND input_gzip IS NULL")){
            while(r.next()){EodSnapshotCodec.decode(Files.readAllBytes(BACKUP.resolve(r.getString(1)).resolve(r.getString(2)+".json.gz")),r.getString(3),r.getLong(4));checked++;if(checked%1000==0)System.out.println("ARCHIVE_RECHECKED="+checked);}
          }
          if(checked!=pending)throw new IllegalStateException("ARCHIVE_COUNT_CHANGED");
          // MOVE needs a second segment before it frees the old one. At zero headroom,
          // recreate this verified-empty column instead; the archived data is never dropped.
          try(var s=c.createStatement()){
            s.setQueryTimeout(180);
            if(hasClob)s.execute("ALTER TABLE ua_eod_item DROP COLUMN input_json");
            s.execute("ALTER TABLE ua_eod_item ADD (input_json CLOB)");
          }
          System.out.println("EMPTY_CLOB_RECLAIMED");stats(c);
        }
        case "--verify" -> verify(c);
        case "--shrink" -> {
          if(c.getMetaData().getDatabaseMajorVersion()<21)throw new IllegalStateException("SECUREFILE_SHRINK_UNSUPPORTED");
          try(var s=c.createStatement()){s.setQueryTimeout(180);s.execute("ALTER TABLE ua_eod_item MODIFY LOB (input_json) (SHRINK SPACE)");}
          System.out.println("SHRINK_DONE");stats(c);
        }
        case "--stats" -> stats(c);
        case "--finish" -> {
          if(scalar(c,"SELECT COUNT(*) FROM ua_eod_item WHERE input_json IS NOT NULL OR (input_sha256 IS NOT NULL AND input_gzip IS NULL)")!=0)throw new IllegalStateException("MIGRATION_INCOMPLETE");
          try(var s=c.prepareStatement("UPDATE ua_eod_lease SET owner_token=NULL,expires_epoch=0 WHERE lease_id=1 AND owner_token=?")){s.setString(1,OWNER);s.executeUpdate();}
          System.out.println("MAINTENANCE_LEASE_RELEASED");
        }
        default -> throw new IllegalArgumentException("UNKNOWN_MODE");
      }
    }
  }
}
