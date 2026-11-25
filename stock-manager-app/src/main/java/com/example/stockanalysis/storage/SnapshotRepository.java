package com.example.stockanalysis.storage;

import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class SnapshotRepository {
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ObjectMapper mapper;
    private final StorageProperties settings;private final SnapshotChunks chunks;private final FileSnapshotArchive archive;
    private volatile boolean archiveRegistered;
    public SnapshotRepository(DataSource source,ObjectMapper mapper,StorageProperties settings){
        jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));this.mapper=mapper;this.settings=settings;
        chunks=new SnapshotChunks(mapper);archive=new FileSnapshotArchive(settings.archiveDirectory());
    }
    public boolean enabled(){return settings.deduplicationEnabled();}
    public String put(String json,String kind,long now){
        var document=chunks.split(json);
        var restored=new HashMap<String,String>();document.objects().forEach((id,value)->restored.put(id,EodSnapshotCodec.decode(value.gzip(),id,value.utf8Bytes())));
        if(!json.equals(chunks.restore(document.manifest(),restored)))throw new IllegalStateException("SNAPSHOT_WRITE_ROUNDTRIP_FAILED");
        final EodSnapshotCodec.Encoded manifest;
        try{manifest=EodSnapshotCodec.encode(mapper.writeValueAsString(document.manifest()));}catch(Exception e){throw new IllegalArgumentException("MANIFEST_SERIALIZATION_FAILED",e);}
        var objects=new LinkedHashMap<>(document.objects());objects.putIfAbsent(manifest.sha256(),manifest);
        return tx.execute(status->{
            // One short DB fence prevents insert races between immutable object writers.
            jdbc.queryForList("SELECT lock_id FROM ua_storage_lock WHERE lock_id=1 FOR UPDATE");
            saveObjects(objects,now);
            var existing=jdbc.queryForList("SELECT sha256 FROM ua_snapshot_document WHERE sha256=?",String.class,document.sha256());
            if(existing.isEmpty())jdbc.update("INSERT INTO ua_snapshot_document(sha256,manifest_id,manifest_utf8_bytes,utf8_bytes,kind,created_epoch) VALUES(?,?,?,?,?,?)",document.sha256(),manifest.sha256(),manifest.utf8Bytes(),document.utf8Bytes(),kind,now);
            return document.sha256();
        });
    }
    private void saveObjects(Map<String,EodSnapshotCodec.Encoded> objects,long now){
        var existing=new HashSet<String>();var cold=new HashSet<String>();
        for(var group:groups(new ArrayList<>(objects.keySet()),500))jdbc.query("SELECT sha256,CASE WHEN payload_gzip IS NULL THEN 1 ELSE 0 END cold FROM ua_snapshot_object WHERE sha256 IN ("+marks(group.size())+")",(org.springframework.jdbc.core.RowCallbackHandler)r->{existing.add(r.getString(1));if(r.getInt(2)==1)cold.add(r.getString(1));},group.toArray());
        var fresh=objects.entrySet().stream().filter(e->!existing.contains(e.getKey())).toList();
        jdbc.batchUpdate("INSERT INTO ua_snapshot_object(sha256,utf8_bytes,payload_gzip,created_epoch,last_reference_epoch) VALUES(?,?,?,?,?)",fresh,100,(s,e)->{
            s.setString(1,e.getKey());s.setInt(2,e.getValue().utf8Bytes());s.setBytes(3,e.getValue().gzip());s.setLong(4,now);s.setLong(5,now);
        });
        // A current analysis has the original bytes available: reuse or rehydrate its chunks locally.
        var reused=objects.entrySet().stream().filter(e->existing.contains(e.getKey())).toList();
        jdbc.batchUpdate("UPDATE ua_snapshot_object SET last_reference_epoch=GREATEST(last_reference_epoch,?) WHERE sha256=? AND utf8_bytes=?",reused,100,(s,e)->{
            s.setLong(1,now);s.setString(2,e.getKey());s.setInt(3,e.getValue().utf8Bytes());
        });
        var rehydrate=reused.stream().filter(e->cold.contains(e.getKey())).toList();
        jdbc.batchUpdate("UPDATE ua_snapshot_object SET payload_gzip=? WHERE sha256=? AND payload_gzip IS NULL",rehydrate,100,(s,e)->{s.setBytes(1,e.getValue().gzip());s.setString(2,e.getKey());});
    }
    public String read(String documentId){
        var header=jdbc.queryForObject("SELECT manifest_id,utf8_bytes,manifest_utf8_bytes FROM ua_snapshot_document WHERE sha256=?",(r,n)->new Header(r.getString(1),r.getInt(2),r.getInt(3)),documentId);
        final SnapshotChunks.Manifest manifest;
        try{manifest=mapper.readValue(readObjects(Map.of(header.manifest(),header.manifestBytes())).get(header.manifest()),SnapshotChunks.Manifest.class);}
        catch(Exception e){throw new IllegalStateException("SNAPSHOT_MANIFEST_UNREADABLE",e);}
        if(!documentId.equals(manifest.sha256())||header.bytes()!=manifest.utf8Bytes())throw new IllegalStateException("SNAPSHOT_REFERENCE_MISMATCH");
        var ids=new LinkedHashMap<String,Integer>();
        for(var part:manifest.parts())if(part.objectId()!=null){var previous=ids.putIfAbsent(part.objectId(),part.objectBytes());if(previous!=null&&previous!=part.objectBytes())throw new IllegalStateException("OBJECT_SIZE_CONFLICT");}
        return chunks.restore(manifest,readObjects(ids));
    }
    private record Header(String manifest,int bytes,int manifestBytes){}
    private record ObjectData(String id,long bytes,byte[] gzip,String archiveKey){}
    private Map<String,String> readObjects(Map<String,Integer> ids){
        var found=new HashMap<String,String>();
        for(var group:groups(new ArrayList<>(ids.keySet()),500)){
            var rows=jdbc.query("SELECT sha256,utf8_bytes,payload_gzip,archive_key FROM ua_snapshot_object WHERE sha256 IN ("+marks(group.size())+")",
                (r,n)->new ObjectData(r.getString(1),r.getLong(2),r.getBytes(3),r.getString(4)),group.toArray());
            for(var row:rows){
                if(row.bytes()!=ids.get(row.id()))throw new IllegalStateException("OBJECT_SIZE_CONFLICT");
                String value;
                if(row.gzip()!=null)value=EodSnapshotCodec.decode(row.gzip(),row.id(),row.bytes());
                else {if(!row.id().equals(row.archiveKey()))throw new IllegalStateException("ARCHIVE_REFERENCE_INVALID");if(!archiveRegistered)registerArchive();value=archive.read(row.id(),row.bytes());}
                found.put(row.id(),value);
            }
        }
        // Verified cold objects do not need permanent DB catalog rows: the manifest carries their hashes and sizes.
        for(var entry:ids.entrySet())if(!found.containsKey(entry.getKey())){if(!archiveRegistered)registerArchive();found.put(entry.getKey(),archive.read(entry.getKey(),entry.getValue()));}
        return found;
    }
    /** Upload, read back and validate before releasing a hot DB payload. */
    public int archiveBefore(long cutoff,int limit,long now){
        registerArchive();archive.checkWritable();
        var rows=jdbc.query("SELECT sha256,utf8_bytes,payload_gzip,archive_key FROM ua_snapshot_object WHERE payload_gzip IS NOT NULL AND last_reference_epoch<? ORDER BY last_reference_epoch,sha256 FETCH FIRST ? ROWS ONLY",
            (r,n)->new ObjectData(r.getString(1),r.getLong(2),r.getBytes(3),r.getString(4)),cutoff,limit);
        int archived=0;
        for(var row:rows){
            EodSnapshotCodec.decode(row.gzip(),row.id(),row.bytes());
            archive.put(new EodSnapshotCodec.Encoded(row.gzip(),row.id(),Math.toIntExact(row.bytes())));
            archived+=tx.execute(status->{
                jdbc.queryForList("SELECT lock_id FROM ua_storage_lock WHERE lock_id=1 FOR UPDATE");
                int updated=jdbc.update("UPDATE ua_snapshot_object SET archive_key=?,archived_epoch=?,payload_gzip=NULL WHERE sha256=? AND payload_gzip IS NOT NULL AND last_reference_epoch<?",row.id(),now,row.id(),cutoff);
                if(updated==1){
                    jdbc.update("DELETE FROM ua_snapshot_object WHERE sha256=? AND payload_gzip IS NULL AND archive_key=?",row.id(),row.id());
                    // Persist the cold-storage dependency even if a later object fails in this batch.
                    jdbc.update("UPDATE ua_storage_state SET archived_objects=archived_objects+1 WHERE state_id=1");
                }return updated;
            });
        }return archived;
    }
    public void validateArchiveConfiguration(){
        long cold=jdbc.queryForObject("SELECT archived_objects FROM ua_storage_state WHERE state_id=1",Long.class)
            +jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_document d WHERE NOT EXISTS(SELECT 1 FROM ua_snapshot_object o WHERE o.sha256=d.manifest_id)",Long.class);
        if(cold>0&&!archive.configured())throw new IllegalStateException("Archived snapshots exist: configure storage.snapshots.archive-directory before starting the server");
        if(archive.configured()||cold>0)registerArchive();
    }
    public String archiveIdentity(){if(!archive.configured())return null;if(!archiveRegistered)registerArchive();return archive.identity();}
    public void validateArchiveWriter(){archive.checkWritable();}
    private void registerArchive(){
        tx.executeWithoutResult(status->{
            String expected=jdbc.queryForObject("SELECT archive_store_id FROM ua_storage_state WHERE state_id=1 FOR UPDATE",String.class);
            if(expected==null){expected=archive.initializeIdentity();jdbc.update("UPDATE ua_storage_state SET archive_store_id=? WHERE state_id=1",expected);}
            archive.expectIdentity(expected);
        });
        archiveRegistered=true;
    }
    public Map<String,Object> counts(){
        var result=jdbc.queryForMap("SELECT COUNT(*) object_count,COALESCE(SUM(utf8_bytes),0) unique_utf8_bytes FROM ua_snapshot_object");
        result.put("ARCHIVED_OBJECTS",jdbc.queryForObject("SELECT archived_objects FROM ua_storage_state WHERE state_id=1",Long.class));
        result.put("DOCUMENT_COUNT",jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_document",Long.class));
        result.put("MIGRATION_ERRORS",jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_migration_error",Long.class));return result;
    }
    private static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
    private static <T>List<List<T>> groups(List<T> values,int size){var result=new ArrayList<List<T>>();for(int i=0;i<values.size();i+=size)result.add(values.subList(i,Math.min(values.size(),i+size)));return result;}
}
