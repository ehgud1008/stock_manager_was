package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.MarketStudy.*;
import com.example.stockanalysis.shared.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.example.stockanalysis.unified.eod.TradingCalendar;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class MarketStudyService {
    private static final Logger log = LoggerFactory.getLogger(MarketStudyService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final ObjectMapper json;
    private final KiwoomMarketArchive archive;
    private final Clock clock;
    private final Path runs;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();

    MarketStudyService(ObjectMapper json, KiwoomMarketArchive archive, Clock clock,
                       @Value("${swing.backtest.market-dir:./data/swing-market}") String dir) {
        this.json = json;
        this.archive = archive;
        this.clock = clock;
        this.runs = Path.of(dir).toAbsolutePath().resolve("runs");
    }

    synchronized Job create(UUID id, Spec spec) throws IOException {
        spec.validate(LocalDate.now(clock.withZone(SEOUL)));
        if (Files.exists(meta(id))) {
            var saved = job(id);
            if (!saved.spec().equals(spec)) throw invalid("같은 실행 ID에 다른 설정이 등록되어 있습니다.");
            return saved;
        }
        Job job = new Job(id,spec,clock.instant(),"QUEUED",null,"",null);
        save(job);
        return job;
    }

    List<Job> jobs() throws IOException {
        if (!Files.exists(runs)) return List.of();
        try (var folders = Files.list(runs)) {
            var jobs = new ArrayList<Job>();
            for (var path : folders.map(p -> p.resolve("job.json")).filter(Files::isRegularFile).toList()) {
                jobs.add(MarketFiles.read(json,path,Job.class));
            }
            jobs.sort(Comparator.comparing(Job::created).reversed());
            return jobs;
        }
    }

    Job job(UUID id) throws IOException {
        if (!Files.exists(meta(id))) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 실행이 없습니다.");
        return MarketFiles.read(json,meta(id),Job.class);
    }

    Result result(UUID id) throws IOException {
        if (!"COMPLETE".equals(job(id).state())) throw invalid("완료된 실행만 결과를 볼 수 있습니다.");
        return MarketFiles.read(json,dir(id).resolve("result.json.gz"),Result.class);
    }

    synchronized Job control(UUID id, boolean retry) throws IOException {
        var current = job(id);
        if (retry && Set.of("FAILED","CANCELLED").contains(current.state())) return state(current,"QUEUED",null,"",null);
        if (!retry && Set.of("QUEUED","RUNNING").contains(current.state())) return state(current,"CANCELLED",current.progress(),"사용자 중단",null);
        return current;
    }

    @Scheduled(fixedDelayString="5000", initialDelayString="5000")
    void tick() {
        if (pool.isShutdown() || !Files.exists(runs) || !busy.compareAndSet(false,true)) return;
        pool.execute(() -> {
            try {
                var pending = jobs().stream().filter(j -> Set.of("QUEUED","RUNNING").contains(j.state()))
                        .sorted(Comparator.comparing(Job::created)).toList();
                if (!pending.isEmpty()) execute(pending.get(0).id());
            } catch (Exception e) {
                log.warn("Market study worker failed",e);
            } finally { busy.set(false); }
        });
    }

    void execute(UUID id) throws IOException {
        try (var channel = FileChannel.open(dir(id).resolve("worker.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) return;
            Job current;
            synchronized (this) {
                current = job(id);
                if (!Set.of("QUEUED","RUNNING").contains(current.state())) return;
                current = state(current,"RUNNING",current.spec().from(),"",null);
            }
            try {
                var result = calculate(current);
                synchronized (this) {
                    check(id);
                    MarketFiles.write(json,dir(id).resolve("result.json.gz"),result);
                    state(current,"COMPLETE",current.spec().to(),"",result.metrics());
                }
            } catch (CancellationException cancelled) {
                // Preserve cancellation or a queued retry.
            } catch (Exception error) {
                synchronized (this) {
                    var latest = job(id);
                    if ("RUNNING".equals(latest.state()))
                        state(latest,"FAILED",latest.progress(),error.getMessage(),null);
                }
            }
        } catch (OverlappingFileLockException occupied) { /* already claimed */ }
    }

    private Result calculate(Job job) throws IOException, NoSuchAlgorithmException {
        Spec spec = job.spec();
        var replay = new MarketReplay(spec);
        var digest = MessageDigest.getInstance("SHA-256");
        var empty = new ArrayList<LocalDate>();
        var from = spec.from().minusDays(60);
        var inputs = dir(job.id()).resolve("kiwoom-v1");
        try {
            var stocks = archive.collect(inputs, spec, from, message -> {
                try { progress(job, null, message); }
                catch (IOException error) { throw new UncheckedIOException(error); }
            });
            int warmup = 0;
            for (int year = from.getYear(); year <= spec.to().getYear(); year++) {
                var sessions = archive.sessions(inputs, stocks, year, () -> {
                    try { check(job.id()); }
                    catch (IOException error) { throw new UncheckedIOException(error); }
                });
                var last = LocalDate.of(year, 12, 31);
                for (var date = from.isAfter(LocalDate.of(year, 1, 1)) ? from : LocalDate.of(year, 1, 1);
                     !date.isAfter(last) && !date.isAfter(spec.to()); date = date.plusDays(1)) {
                    if (date.getDayOfWeek().getValue() >= 6) continue;
                    var rows = sessions.get(date);
                    if (rows == null) {
                        var status = archive.status(date);
                        if (status == TradingCalendar.Status.OPEN)
                            throw invalid(date + " 개장일에 키움 시장 일봉이 없습니다.");
                        if (status == TradingCalendar.Status.UNKNOWN) empty.add(date);
                        continue;
                    }
                    if (date.isBefore(spec.from())) warmup++;
                    else {
                        if (warmup < SwingBreakoutSelector.WARMUP) throw invalid("직전 21거래일의 키움 일봉이 부족합니다.");
                        progress(job, date, "");
                    }
                    consume(replay, digest, date, rows);
                }
            }
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
        return replay.finish(empty, HexFormat.of().formatHex(digest.digest()));
    }

    private void consume(MarketReplay replay, MessageDigest digest, LocalDate date, Map<String,Quote> rows) throws IOException {
        digest.update(date.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        digest.update(json.writeValueAsBytes(rows));
        replay.session(date,rows);
    }

    private synchronized void progress(Job job, LocalDate date, String message) throws IOException {
        check(job.id());
        state(job, "RUNNING", date, message, null);
    }

    private void check(UUID id) throws IOException {
        if (Thread.currentThread().isInterrupted() || !"RUNNING".equals(job(id).state())) throw new CancellationException();
    }

    private Job state(Job j,String status,LocalDate date,String message,Metrics metrics) throws IOException {
        var updated = new Job(j.id(),j.spec(),j.created(),status,date,message,metrics);
        save(updated);
        return updated;
    }
    private void save(Job job) throws IOException { MarketFiles.write(json,meta(job.id()),job); }
    private Path dir(UUID id) { return runs.resolve(id.toString()); }
    private Path meta(UUID id) { return dir(id).resolve("job.json"); }
    @PreDestroy void stop() { pool.shutdownNow(); }
}
