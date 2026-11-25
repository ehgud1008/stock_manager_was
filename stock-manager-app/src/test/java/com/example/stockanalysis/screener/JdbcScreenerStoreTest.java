package com.example.stockanalysis.screener;

import com.example.stockanalysis.engine.screener.*;
import com.example.stockanalysis.engine.screener.ScreenerTypes.Mode;
import com.example.stockanalysis.screener.domain.*;
import com.example.stockanalysis.screener.infrastructure.JdbcScreenerStore;
import com.example.stockanalysis.screener.application.ScreenerQuery;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.*;

class JdbcScreenerStoreTest {
    @org.junit.jupiter.api.io.TempDir Path archiveDirectory;
    private JdbcScreenerStore store;
    private JdbcDataSource source;
    private ObjectMapper mapper;
    private ScreenerRun run;
    @BeforeEach void setup() {
        source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:screener_" + UUID.randomUUID() + ";MODE=Oracle;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("../database/oracle/ddl/08_screener.sql"))).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource(Path.of("../database/oracle/ddl/09_screener_summary.sql"))).execute(source);
        mapper = new ObjectMapper().findAndRegisterModules();
        store = new JdbcScreenerStore(source, mapper);
        run = new ScreenerRun("run1", "key1", new ScreenerRunRequest(Mode.SWING, 0, null, null, List.of()), LocalDateTime.of(2026,9,13,10,0),
                ScreenerConfig.defaults(), DefaultScreenerEngine.VERSION, Instant.parse("2026-09-13T01:00:00Z"), null, ScreenerRun.Status.READY,0,0,0,0,null);
    }
    @Test void oracleDdlAndSnapshotRoundTripRetainPrecisionAndSupportReplayAfterRestart() {
        store.create(run);
        assertThat(store.find(run.runId())).contains(run);
        assertThat(store.findByRequestKey(run.requestKey())).contains(run);
        assertThat(store.latest(Mode.SWING, 0)).isEmpty();
        var pending = ScreenerItem.pending(run.runId(), new ScreenerItem.Stock("005930", "삼성전자", "KOSPI", "", null, "ST"), "MARKET_UNIVERSE");
        run = run.progress(ScreenerRun.Status.RUNNING,1,0,0,0,null,null);
        store.initialize(run, List.of(pending));
        var price = new BigDecimal("100.12345678901234567890123456789");
        var t = LocalDateTime.of(2026,9,11,0,0);
        var input = new ScreenerInput("005930", run.request().timeframe(), run.cutoff(), List.of(new ScreenerBar(t,t.plusDays(1),price,price,price,price,100,null)),List.of());
        var result = new DefaultScreenerEngine(run.config()).analyze(input);
        var item = new ScreenerItem(run.runId(), "005930", "삼성전자", "KOSPI", ScreenerItem.ItemStatus.ANALYZED,"MARKET_UNIVERSE",true,result,List.of(),null);
        store.saveItem(item, input);
        store.saveItem(item, null); // status update must retain original input
        run = run.progress(ScreenerRun.Status.COMPLETED,1,1,0,0,null,Instant.parse("2026-09-13T02:00:00Z"));
        store.saveRun(run);
        var restarted = new JdbcScreenerStore(source, mapper);
        assertThat(restarted.items(run.runId())).containsExactly(item);
        assertThat(restarted.input(run.runId(), "005930")).contains(input);
        assertThat(restarted.latest(Mode.SWING,0)).contains(run);
        assertThat(restarted.latest(Mode.SHORT_TERM,5)).isEmpty();
        assertThat(new DefaultScreenerEngine(run.config()).analyze(restarted.input(run.runId(), "005930").orElseThrow())).isEqualTo(result);
    }
    @Test void archivedSharedScreenerInputRetainsPrecisionAndReplay() {
        for(String file:List.of("11_unified_analysis.sql","18_snapshot_storage.sql"))new ResourceDatabasePopulator(new FileSystemResource(Path.of("../database/oracle/ddl",file))).execute(source);
        var props=new com.example.stockanalysis.storage.StorageProperties(true,true,archiveDirectory.toString(),90,100,true,false,0,80,90,14,false,5);
        var repository=new com.example.stockanalysis.storage.SnapshotRepository(source,mapper,props);store=new JdbcScreenerStore(source,mapper,repository);store.create(run);
        var t=LocalDateTime.of(2026,9,11,0,0);var price=new BigDecimal("100.12345678901234567890123456789");
        var input=new ScreenerInput("005930",run.request().timeframe(),run.cutoff(),List.of(new ScreenerBar(t,t.plusDays(1),price,price,price,price,100,null)),List.of());
        var result=new DefaultScreenerEngine(run.config()).analyze(input);
        var item=new ScreenerItem(run.runId(),"005930","삼성전자","KOSPI",ScreenerItem.ItemStatus.ANALYZED,"MARKET_UNIVERSE",true,result,List.of(),null);
        store.saveItem(item,input);store.saveItem(item,null);
        assertThat(repository.archiveBefore(Long.MAX_VALUE,100,System.currentTimeMillis())).isPositive();
        var restarted=new JdbcScreenerStore(source,mapper,repository);assertThat(restarted.input(run.runId(),"005930")).contains(input);
        assertThat(new DefaultScreenerEngine(run.config()).analyze(restarted.input(run.runId(),"005930").orElseThrow())).isEqualTo(result);
    }
    @Test void initializationRollsBackBothMembershipAndRunOnInvalidItem() {
        store.create(run);
        var good = ScreenerItem.pending(run.runId(), new ScreenerItem.Stock("005930", "삼성전자", "KOSPI", "", null, "ST"), "MARKET_UNIVERSE");
        var bad = ScreenerItem.pending("missing-run", new ScreenerItem.Stock("000660", "SK", "KOSPI", "", null, "ST"), "MARKET_UNIVERSE");
        assertThatThrownBy(() -> store.initialize(run.progress(ScreenerRun.Status.RUNNING,2,0,0,0,null,null), List.of(good,bad))).isInstanceOf(RuntimeException.class);
        assertThat(store.items(run.runId())).isEmpty();
        assertThat(store.find(run.runId())).contains(run);
        assertThat(new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM sc_screening_summary", Long.class)).isZero();
    }
    private ScreenerItem sample(int index) throws Exception {
        var price = new BigDecimal("100.12345678901234567890123456789");
        var t = LocalDateTime.of(2026,9,11,0,0);
        var input = new ScreenerInput("005930", run.request().timeframe(), run.cutoff(), List.of(new ScreenerBar(t,t.plusDays(1),price,price,price,price,100,null)),List.of());
        ObjectNode r = mapper.valueToTree(new DefaultScreenerEngine(run.config()).analyze(input));
        r.put("stage", index % 6 + 1); r.put("previousDistinctStage", index % 6 == 0 ? 6 : index % 6);
        r.put("barsInStage", index % 8 + 1); r.put("barsSinceTransition", index % 8); r.put("quality", "VALID");
        r.put("lastTransition", index % 3 == 0 ? "REVERSE" : "FORWARD");
        ((ObjectNode) r.get("slopes")).put("shortSlope", index % 2 == 0 ? price : price.negate()).put("middleSlope", price).put("longSlope", index % 5 == 0 ? price.negate() : price);
        ((ObjectNode) r.get("middleLongGap")).put("state", index % 2 == 0 ? "EXPANDING" : "CONTRACTING").put("signedPercent", price);
        if (index % 7 == 0) ((ObjectNode) r.get("features")).putNull("tradingAmountMillionWon");
        else ((ObjectNode) r.get("features")).put("tradingAmountMillionWon", price.multiply(BigDecimal.valueOf(index)));
        return new ScreenerItem(run.runId(), String.format("%06d", index), index == 0 ? "literal%_!" : "종목" + index,
                index % 2 == 0 ? "KOSPI" : "KOSDAQ", ScreenerItem.ItemStatus.ANALYZED, "MARKET_UNIVERSE", true,
                mapper.treeToValue(r, ScreenerResult.class), List.of(), null);
    }
    private ScreenerQuery query(String json) throws Exception { return mapper.readValue(json, ScreenerQuery.class); }
    @Test void sqlFiltersSortsAndAggregatesMatchWholeRunSemantics() throws Exception {
        store.create(run);
        var all = new ArrayList<ScreenerItem>();
        for (int i = 0; i < 63; i++) { var item = sample(i); all.add(item); store.saveItem(item, null); }
        var failed = new ScreenerItem(run.runId(), "999999", "실패", "KOSPI", ScreenerItem.ItemStatus.FAILED, "MARKET_UNIVERSE", false, null, List.of(), "FAILED");
        all.add(failed); store.saveItem(failed, null);
        for (String json : List.of("{}", "{\"page\":2,\"size\":20}", "{\"page\":100}",
                "{\"sort\":\"stage\",\"descending\":true,\"size\":7}", "{\"sort\":\"tradingAmount\",\"descending\":true}",
                "{\"sort\":\"tradingAmount\",\"page\":1}", "{\"sort\":\"barsInStage\",\"size\":3,\"page\":2}",
                "{\"stage\":6,\"market\":\"KOSDAQ\"}", "{\"slope\":\"ALL_UP\"}", "{\"slope\":\"MID_LONG_UP\"}",
                "{\"middleLongGap\":\"CONTRACTING\"}", "{\"transitionFilter\":\"RECENT\"}",
                "{\"transitionFilter\":\"5_6\"}", "{\"transitionFilter\":\"6_1\"}", "{\"transitionFilter\":\"HOLD_1\"}",
                "{\"transitionFilter\":\"REVERSE\"}", "{\"transition\":\"FORWARD\",\"maxBarsSinceTransition\":2}",
                "{\"minTradingAmountMillionWon\":100.12345678901234567890123456789}",
                "{\"search\":\"%_!\"}", "{\"search\":\"00001\"}", "{\"status\":\"FAILED\"}",
                "{\"watchOnly\":true}", "{\"watchOnly\":true,\"watchedCodes\":[\"000061\",\"999999\",\"absent\"]}",
                "{\"stage\":1,\"quality\":\"VALID\",\"allSlopesUp\":true,\"watchedCodes\":[\"000061\",\"999999\"]}")) {
            var q = query(json);
            var actual = store.query(run.runId(), q);
            var expected = all.stream().filter(q::matches).sorted(q.comparator()).toList();
            assertThat(actual.totalElements()).as(json).isEqualTo(expected.size());
            assertThat(actual.content().stream().map(ScreenerSummary::stockCode)).as(json).containsExactlyElementsOf(
                    expected.stream().skip((long) q.page() * q.size()).limit(q.size()).map(ScreenerItem::stockCode).toList());
            for (int stage = 1; stage <= 6; stage++) {
                int s = stage;
                assertThat(actual.stageCounts().get(stage)).as(json + " stage " + stage).isEqualTo(all.stream().filter(q.withoutStage()::matches).filter(i -> i.result() != null && Objects.equals(i.result().stage(), s)).count());
            }
            assertThat(actual.recentCount()).isEqualTo(all.stream().filter(i -> i.result() != null && i.result().barsSinceTransition() < 3).count());
            assertThat(actual.watchCount()).isEqualTo(all.stream().filter(i -> q.watchedCodes().contains(i.stockCode())).count());
        }
    }
    @Test void listNeverReadsClobsAndDetailReadsOnlyRequestedPrimaryKey() throws Exception {
        store.create(run);
        var first = sample(1); var second = sample(2);
        store.saveItem(first, null); store.saveItem(second, null);
        new JdbcTemplate(source).update("UPDATE sc_screening_item SET item_json = 'invalid JSON' WHERE stock_code = ?", second.stockCode());
        var result = store.query(run.runId(), query("{\"size\":1}"));
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).middleSlope()).isEqualByComparingTo(first.result().slopes().middleSlope());
        assertThat(mapper.writeValueAsString(result.content())).doesNotContain("history", "input_json", "high52Week", "engineVersion");
        assertThat(store.item(run.runId(), first.stockCode())).contains(first);
        assertThat(store.item(run.runId(), "missing")).isEmpty();
    }
    @Test void projectionUpdatesAlongsideSnapshotAndIsIsolatedByRun() throws Exception {
        store.create(run);
        var item = sample(1); store.saveItem(item, null);
        assertThat(store.query("other-run", query("{}")).totalElements()).isZero();
        var failed = new ScreenerItem(run.runId(), item.stockCode(), item.stockName(), item.market(), ScreenerItem.ItemStatus.FAILED,
                "MARKET_UNIVERSE", false, null, List.of(), "FAILED");
        store.saveItem(failed, null);
        var page = store.query(run.runId(), query("{}"));
        assertThat(page.content()).containsExactly(ScreenerSummary.from(failed));
        assertThat(page.stageCounts().values()).allMatch(count -> count == 0);
    }
}
