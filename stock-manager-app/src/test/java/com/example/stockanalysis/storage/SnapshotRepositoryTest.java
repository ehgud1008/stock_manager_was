package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class SnapshotRepositoryTest {
    @TempDir Path archive;JdbcTemplate jdbc;SnapshotRepository store;
    @BeforeEach void setup()throws Exception{var ds=StorageFixtures.database();jdbc=new JdbcTemplate(ds);store=new SnapshotRepository(ds,StorageFixtures.JSON,StorageFixtures.settings(archive.toString(),true,false));}
    long count(){return jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object",Long.class);}
    @Test void sharesChunksAndColdReadsWorkAfterCatalogRowsAreReclaimed(){
        String first=StorageFixtures.history(0,"2026-10-20T11:00:00Z"),next=StorageFixtures.history(1,"2026-10-21T11:00:00Z");
        String a=store.put(first,"EOD_INPUT",10);long firstObjects=count();String b=store.put(next,"EOD_INPUT",20);
        assertThat(count()).isLessThan(firstObjects*2);assertThat(store.read(a)).isEqualTo(first);assertThat(store.read(b)).isEqualTo(next);
        int archived=store.archiveBefore(30,100,100);assertThat(archived).isPositive();assertThat(count()).isZero();
        assertThat(store.read(a)).isEqualTo(first);assertThat(store.read(b)).isEqualTo(next);
        // New observations can rehydrate immutable historical chunks without losing old documents.
        store.put(next,"EOD_INPUT",200);assertThat(count()).isPositive();assertThat(store.read(a)).isEqualTo(first);
        assertThat(store.archiveBefore(150,100,210)).isZero();
    }
    @Test void corruptArchiveNeverCausesHotPayloadDeletion()throws Exception{
        String id=store.put("{\"x\":1}","EOD_INPUT",10);
        String object=jdbc.queryForObject("SELECT manifest_id FROM ua_snapshot_document WHERE sha256=?",String.class,id);
        Path file=archive.resolve(object.substring(0,2)).resolve(object+".json.gz");Files.createDirectories(file.getParent());Files.write(file,new byte[]{1,2,3});
        assertThatThrownBy(()->store.archiveBefore(20,100,30)).isInstanceOf(RuntimeException.class);
        assertThat(count()).isEqualTo(1);assertThat(store.read(id)).isEqualTo("{\"x\":1}");
    }
    @Test void multipleWritersReuseOneDocument()throws Exception{
        try(var pool=new ClosingPool()){
            var tasks=new ArrayList<Callable<String>>();for(int i=0;i<8;i++)tasks.add(()->store.put(StorageFixtures.history(0,"2026-10-20T11:00:00Z"),"EOD_INPUT",10));
            var ids=new HashSet<String>();for(var future:pool.executor.invokeAll(tasks))ids.add(future.get());assertThat(ids).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_document",Long.class)).isEqualTo(1);
        }
    }
    @Test void missingMountMarkerRefusesBothArchivingAndColdReads()throws Exception{
        String id=store.put("{\"old\":true}","EOD_INPUT",1);store.archiveBefore(2,100,3);
        Files.delete(archive.resolve(".stock-snapshot-archive"));
        assertThatThrownBy(()->store.read(id)).isInstanceOf(RuntimeException.class);
        store.put("{\"new\":true}","EOD_INPUT",4);
        assertThatThrownBy(()->store.archiveBefore(5,100,6)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object WHERE payload_gzip IS NOT NULL",Long.class)).isEqualTo(1);
    }
    @Test void aPartiallyArchivedDocumentStillRequiresTheMountAtStartup(){
        String json=StorageFixtures.history(0,"2026-10-20T11:00:00Z");String id=store.put(json,"EOD_INPUT",1);
        jdbc.update("UPDATE ua_snapshot_object SET last_reference_epoch=10 WHERE sha256=(SELECT manifest_id FROM ua_snapshot_document WHERE sha256=?)",id);
        assertThat(store.archiveBefore(2,1,3)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_document d WHERE NOT EXISTS(SELECT 1 FROM ua_snapshot_object o WHERE o.sha256=d.manifest_id)",Long.class)).isZero();
        var source=jdbc.getDataSource();var unmounted=new SnapshotRepository(source,StorageFixtures.JSON,StorageFixtures.settings("",false,false));
        assertThatThrownBy(unmounted::validateArchiveConfiguration).isInstanceOf(IllegalStateException.class).hasMessageContaining("Archived snapshots exist");
        assertThat(store.read(id)).isEqualTo(json);
    }
    static class ClosingPool implements AutoCloseable{final ExecutorService executor=Executors.newFixedThreadPool(4);public void close(){executor.shutdownNow();}}
}
