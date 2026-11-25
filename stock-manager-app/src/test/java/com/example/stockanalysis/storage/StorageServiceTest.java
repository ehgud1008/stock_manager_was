package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import com.example.stockanalysis.unified.report.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class StorageServiceTest {
    @TempDir Path archive;
    static class Time extends Clock{Instant now=Instant.parse("2026-10-21T03:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}void advance(){now=now.plusSeconds(86400);}}
    @Test void queuesOnlyLevelTransitionsAndNeverRetriesAmbiguousSends()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);var props=StorageFixtures.settings("",false,true);var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);
        var usage=new AtomicReference<>(new StorageUsageProbe.Usage(500,1000L,"PDB_SEGMENTS",List.of()));
        var gateway=mock(TelegramGateway.class);when(gateway.send(anyString(),anyString())).thenReturn(new TelegramGateway.Result("SENT","1",null,0));
        var time=new Time();var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),usage::get,props,mock(TradingCalendar.class),time,new TelegramProperties(true,"123456:abcdefghijklmnopqrstuvwxyzABCDE","123456",""),gateway);
        service.monitor();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_notice",Long.class)).isZero();
        time.advance();usage.set(new StorageUsageProbe.Usage(850,1000L,"PDB_SEGMENTS",List.of()));service.monitor();service.monitor();service.deliver();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_notice",Long.class)).isEqualTo(1);verify(gateway,times(1)).send(anyString(),anyString());
        time.advance();usage.set(new StorageUsageProbe.Usage(860,1000L,"PDB_SEGMENTS",List.of()));service.monitor();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_notice",Long.class)).isEqualTo(1);
        time.advance();usage.set(new StorageUsageProbe.Usage(950,1000L,"PDB_SEGMENTS",List.of()));service.monitor();
        when(gateway.send(anyString(),anyString())).thenThrow(new IllegalStateException("uncertain delivery"));service.deliver();service.deliver();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_storage_notice WHERE status='UNKNOWN'",Long.class)).isEqualTo(1);verify(gateway,times(2)).send(anyString(),anyString());
    }
    @Test void unknownCalendarOrDisabledArchiveNeverDeletesHotObjects()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);var props=StorageFixtures.settings(archive.toString(),true,false);var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);
        String id=repo.put("{\"old\":true}","EOD_INPUT",1);
        var calendar=mock(TradingCalendar.class);when(calendar.day(any())).thenAnswer(i->new TradingCalendar.Day(i.getArgument(0),TradingCalendar.Status.UNKNOWN,null,"unknown"));
        var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),()->new StorageUsageProbe.Usage(10,null,"SCHEMA_SEGMENTS",List.of()),props,calendar,new Time(),new TelegramProperties(false,"","",""),mock(TelegramGateway.class));
        assertThat(service.plan().state()).isEqualTo("CALENDAR_UNAVAILABLE");assertThatThrownBy(service::maintain).isInstanceOf(IllegalStateException.class);
        assertThat(repo.read(id)).isEqualTo("{\"old\":true}");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object",Long.class)).isEqualTo(1);
        var unknown=StorageHealth.assess(new StorageUsageProbe.Usage(100,null,"SCHEMA_SEGMENTS",List.of()),null,0,props);assertThat(unknown.level()).isEqualTo("UNKNOWN");assertThat(unknown.allocatedPercent()).isNull();
    }
    @Test void growthForecastUsesComparableElapsedDaysAndIgnoresNegativeGrowth(){
        var props=StorageFixtures.settings("",false,false);
        var risk=StorageHealth.assess(new StorageUsageProbe.Usage(600,1000L,"PDB_SEGMENTS",List.of()),500L,86400000,props);
        assertThat(risk.level()).isEqualTo("WARNING");assertThat(risk.estimatedDays()).isEqualTo(4);
        assertThat(StorageHealth.assess(new StorageUsageProbe.Usage(400,1000L,"PDB_SEGMENTS",List.of()),500L,86400000,props).estimatedDays()).isNull();
    }
    @Test void correctedScopeRefreshesWithinAnHourAndDoesNotUseTheOldScopeForGrowth()throws Exception{
        var ds=StorageFixtures.database();var props=StorageFixtures.settings("",false,false);var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);
        var usage=new AtomicReference<>(new StorageUsageProbe.Usage(900,1000L,"PDB_SEGMENTS",List.of()));var time=new Time();
        var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),usage::get,props,mock(TradingCalendar.class),time,new TelegramProperties(false,"","",""),mock(TelegramGateway.class));
        service.monitor();assertThat(service.status().latest().assessment().level()).isEqualTo("CRITICAL");
        usage.set(new StorageUsageProbe.Usage(300,1000L,"USER_DATA_SEGMENTS",List.of()));time.now=time.now.plusSeconds(1);service.monitor();
        var latest=service.status().latest();assertThat(latest.usage().scope()).isEqualTo("USER_DATA_SEGMENTS");
        assertThat(latest.assessment().level()).isEqualTo("NORMAL");assertThat(latest.assessment().dailyGrowthBytes()).isNull();
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT last_level FROM ua_storage_state WHERE state_id=1",String.class)).isEqualTo("NORMAL");
    }
    @Test void retentionIncludesTheLastDueSessionAndPreservesTheBoundaryDay()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);
        var props=new StorageProperties(true,true,archive.toString(),20,100,false,false,1000,80,90,14,false,5);
        var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);var zone=ZoneId.of("Asia/Seoul");
        String old=repo.put("{\"day\":\"old\"}","EOD_INPUT",LocalDate.of(2026,9,21).atStartOfDay(zone).toInstant().toEpochMilli());
        String boundary=repo.put("{\"day\":\"boundary\"}","EOD_INPUT",LocalDate.of(2026,9,22).atStartOfDay(zone).toInstant().toEpochMilli());
        var calendar=mock(TradingCalendar.class);
        when(calendar.latestDue(any())).thenCallRealMethod();
        when(calendar.day(any())).thenAnswer(i->{LocalDate date=i.getArgument(0);boolean closed=date.getDayOfWeek().getValue()>5||date.equals(LocalDate.of(2026,10,9));return new TradingCalendar.Day(date,closed?TradingCalendar.Status.CLOSED:TradingCalendar.Status.OPEN,LocalTime.of(20,0),"test");});
        var time=new Time();time.now=Instant.parse("2026-10-21T10:00:00Z");
        var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),()->new StorageUsageProbe.Usage(10,null,"SCHEMA_SEGMENTS",List.of()),props,calendar,time,new TelegramProperties(false,"","",""),mock(TelegramGateway.class));
        service.heartbeat();assertThat(service.plan().keepFrom()).isEqualTo(LocalDate.of(2026,9,22));service.maintain();
        assertThat(jdbc.queryForObject("SELECT archived_objects FROM ua_storage_state WHERE state_id=1",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object",Long.class)).isEqualTo(1);
        assertThat(repo.read(old)).isEqualTo("{\"day\":\"old\"}");assertThat(repo.read(boundary)).isEqualTo("{\"day\":\"boundary\"}");
        time.now=Instant.parse("2026-10-21T12:00:00Z");assertThat(service.plan().keepFrom()).isEqualTo(LocalDate.of(2026,9,23));
    }
    @Test void anActiveReaderWithoutTheSharedMountBlocksAutomaticArchiving()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);var props=StorageFixtures.settings(archive.toString(),true,false);var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);
        var time=new Time();var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),()->new StorageUsageProbe.Usage(10,null,"SCHEMA_SEGMENTS",List.of()),props,mock(TradingCalendar.class),time,new TelegramProperties(false,"","",""),mock(TelegramGateway.class));
        service.heartbeat();
        jdbc.update("INSERT INTO ua_storage_instance(instance_id,last_seen_epoch,archive_enabled) VALUES('another-server',?,0)",time.millis());
        assertThat(service.plan().state()).isEqualTo("READERS_NOT_READY");
        assertThatThrownBy(service::maintain).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ua_snapshot_object",Long.class)).isZero();
    }
    @Test void archiveOutageIsRecordedAndDoesNotPreventCapacityMeasurement()throws Exception{
        var ds=StorageFixtures.database();var jdbc=new JdbcTemplate(ds);var props=StorageFixtures.settings(archive.toString(),true,false);var repo=new SnapshotRepository(ds,StorageFixtures.JSON,props);
        var service=new StorageService(ds,StorageFixtures.JSON,repo,new StorageMigration(ds,repo),()->new StorageUsageProbe.Usage(10,null,"SCHEMA_SEGMENTS",List.of()),props,mock(TradingCalendar.class),new Time(),new TelegramProperties(false,"","",""),mock(TelegramGateway.class));
        service.heartbeat();java.nio.file.Files.delete(archive.resolve(".stock-snapshot-archive"));service.heartbeat();service.monitor();
        assertThat(service.status().archive().state()).isEqualTo("ARCHIVE_UNAVAILABLE");
        assertThat(service.status().latest()).isNotNull();assertThat(service.status().lastError()).isEqualTo("ARCHIVE_READERS_UNAVAILABLE");
    }
}
