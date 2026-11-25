package com.example.stockanalysis.backtest;

import static com.example.stockanalysis.backtest.SwingBacktestModels.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.stereotype.Repository;

@Repository
class SwingBacktestStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    SwingBacktestStore(DataSource dataSource, ObjectMapper mapper) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    void insert(Run run) {
        var tracked = run.followUp().isEmpty() ? run.sessions() : run.followUp();
        jdbc.update("INSERT INTO swing_paper_run (run_id, created_epoch, start_date, last_date, run_json) VALUES (?,?,?,?,?)",
                run.id().toString(), run.createdAt().toEpochMilli(), Date.valueOf(run.sessions().get(0)),
                Date.valueOf(tracked.get(tracked.size() - 1)), lob(run));
    }

    Run find(UUID id) {
        return jdbc.query("SELECT run_json FROM swing_paper_run WHERE run_id=?",
                (row, n) -> decode(row.getString(1), Run.class), id.toString()).stream().findFirst().orElse(null);
    }

    List<Run> recent() {
        return jdbc.query("SELECT run_json FROM swing_paper_run ORDER BY created_epoch DESC, run_id FETCH FIRST 30 ROWS ONLY",
                (row, n) -> decode(row.getString(1), Run.class));
    }

    List<Run> due(LocalDate date) {
        return jdbc.query("""
                SELECT r.run_json FROM swing_paper_run r
                WHERE r.start_date<=? AND r.last_date>=?
                AND NOT EXISTS (SELECT 1 FROM swing_paper_day d WHERE d.run_id=r.run_id AND d.business_date=?)
                ORDER BY r.created_epoch, r.run_id
                """, (row, n) -> decode(row.getString(1), Run.class),
                Date.valueOf(date), Date.valueOf(date), Date.valueOf(date));
    }

    Map<LocalDate, Frame> frames(UUID runId) {
        var frames = new TreeMap<LocalDate, Frame>();
        jdbc.query("SELECT business_date, frame_json FROM swing_paper_day WHERE run_id=?",
                row -> { frames.put(row.getDate(1).toLocalDate(), decode(row.getString(2), Frame.class)); },
                runId.toString());
        return frames;
    }

    void capture(UUID runId, LocalDate date, Frame frame) {
        try {
            jdbc.update("INSERT INTO swing_paper_day (run_id,business_date,frame_json) VALUES (?,?,?)",
                    runId.toString(), Date.valueOf(date), lob(frame));
        } catch (DuplicateKeyException ignored) {
            // Another worker already wrote the immutable capture for this date.
        }
    }

    private SqlParameterValue lob(Object value) {
        try {
            return new SqlParameterValue(Types.CLOB, new SqlLobValue(mapper.writeValueAsString(value)));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not serialize swing backtest", error);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not read swing backtest", error);
        }
    }
}
