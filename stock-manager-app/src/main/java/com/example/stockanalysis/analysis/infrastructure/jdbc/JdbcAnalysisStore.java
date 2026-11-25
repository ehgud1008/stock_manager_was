package com.example.stockanalysis.analysis.infrastructure.jdbc;

import com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload;
import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.model.NewAnalysisRun;
import com.example.stockanalysis.analysis.application.port.out.AnalysisStore;
import com.example.stockanalysis.analysis.domain.AnalysisFactorView;
import com.example.stockanalysis.analysis.domain.AnalysisRunView;
import com.example.stockanalysis.analysis.domain.AnalysisStatus;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.analysis.domain.StockPricePoint;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.AnalysisResult;
import com.example.stockanalysis.engine.model.FactorResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

public class JdbcAnalysisStore implements AnalysisStore {

    private static final String HEADER_SELECT = """
            SELECT r.analysis_run_id, r.stock_code, r.analysis_mode, r.status,
                   r.data_quality_status, r.base_date, r.engine_version,
                   r.error_code, r.error_message, r.started_at, r.completed_at,
                   a.stock_name, a.total_score, a.score_grade, a.strategy_code,
                   a.strategy_name, a.strategy_action, a.current_price_krw,
                   a.historical_samples,
                   s.entry_from_krw, s.entry_to_krw, s.stop_loss_krw
              FROM an_analysis_run r
              LEFT JOIN an_analysis_result a ON a.analysis_run_id = r.analysis_run_id
              LEFT JOIN an_trade_scenario s
                ON s.analysis_run_id = r.analysis_run_id AND s.scenario_code = 'BASE'
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAnalysisStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public long createRun(NewAnalysisRun run) {
        String sql = """
                INSERT INTO an_analysis_run (
                    request_key, stock_code, analysis_mode, base_date, status,
                    data_quality_status, engine_version, config_version,
                    data_cutoff_at, force_recalculate_yn, requested_by,
                    started_at, created_at, updated_at
                ) VALUES (
                    :requestKey, :stockCode, :analysisMode, :baseDate, 'RUNNING',
                    'PENDING', :engineVersion, :configVersion,
                    :dataCutoffAt, :forceYn, :requestedBy,
                    :startedAt, SYSTIMESTAMP, SYSTIMESTAMP
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requestKey", run.requestKey())
                .addValue("stockCode", run.stockCode())
                .addValue("analysisMode", run.analysisMode().name())
                .addValue("baseDate", Date.valueOf(run.baseDate()))
                .addValue("engineVersion", run.engineVersion())
                .addValue("configVersion", run.configVersion())
                .addValue("dataCutoffAt", dbTimestamp(run.dataCutoffAt()))
                .addValue("forceYn", run.forceRecalculate() ? "Y" : "N")
                .addValue("requestedBy", run.requestedBy())
                .addValue("startedAt", dbTimestamp(run.dataCutoffAt()));
        return insertKey(sql, params, "ANALYSIS_RUN_ID");
    }

    @Override
    @Transactional
    public void saveCompleted(long runId, CollectedAnalysisData data, AnalysisResult result, Instant completedAt) {
        saveCompleted(runId, data, result, completedAt, null);
    }

    @Override
    @Transactional
    public void saveCompleted(
            long runId,
            CollectedAnalysisData data,
            AnalysisResult result,
            Instant completedAt,
            com.example.stockanalysis.judgment.TradeJudgment judgment
    ) {
        for (AnalysisSourcePayload source : data.sources()) {
            long responseId = saveSource(runId, source);
            saveTypedRecords(responseId, source);
        }
        saveResult(runId, data, result);
        saveFactors(runId, data, result);
        if (result.scenario() != null) {
            long scenarioId = saveScenario(runId, result);
            saveTargets(scenarioId, result);
        }
        saveReasons(runId, result);
        if (judgment != null) {
            jdbc.update("""
                    INSERT INTO an_analysis_reason (analysis_run_id, reason_type, reason_code,
                        reason_message, display_order, details_json, created_at)
                    VALUES (:runId, 'TRADE_JUDGMENT', 'TRADE_JUDGMENT_V1', '매매판단 스냅샷', 0, :details, SYSTIMESTAMP)
                    """, new MapSqlParameterSource("runId", runId)
                    .addValue("details", json(judgment), java.sql.Types.CLOB));
        }

        jdbc.update("""
                UPDATE an_analysis_run
                   SET status = 'COMPLETED',
                       data_quality_status = :quality,
                       input_fingerprint = :fingerprint,
                       completed_at = :completedAt,
                       updated_at = SYSTIMESTAMP,
                       error_code = NULL,
                       error_message = NULL
                 WHERE analysis_run_id = :runId
                """, new MapSqlParameterSource()
                .addValue("quality", data.dataQualityStatus().name())
                .addValue("fingerprint", hash(json(data.input())))
                .addValue("completedAt", dbTimestamp(completedAt))
                .addValue("runId", runId));
    }

    @Override
    @Transactional
    public void markFailed(long runId, String errorCode, String errorMessage, Instant failedAt) {
        jdbc.update("""
                UPDATE an_analysis_run
                   SET status = 'FAILED', data_quality_status = 'INVALID',
                       error_code = :errorCode, error_message = :errorMessage,
                       completed_at = :failedAt, updated_at = SYSTIMESTAMP
                 WHERE analysis_run_id = :runId
                """, new MapSqlParameterSource()
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", errorMessage)
                .addValue("failedAt", dbTimestamp(failedAt))
                .addValue("runId", runId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalysisRunView> findById(long runId) {
        return findHeader(HEADER_SELECT + " WHERE r.analysis_run_id = :runId",
                Map.of("runId", runId)).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalysisRunView> findLatest(String stockCode, AnalysisMode mode) {
        List<Long> ids = jdbc.queryForList("""
                SELECT analysis_run_id
                  FROM an_analysis_run
                 WHERE stock_code = :stockCode
                   AND analysis_mode = :analysisMode
                   AND status = 'COMPLETED'
                 ORDER BY base_date DESC, created_at DESC
                 FETCH FIRST 1 ROWS ONLY
                """, Map.of("stockCode", stockCode, "analysisMode", mode.name()), Long.class);
        return ids.isEmpty() ? Optional.empty() : findById(ids.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalysisRunView> findCompleted(
            String stockCode,
            AnalysisMode mode,
            LocalDate baseDate
    ) {
        List<Long> ids = jdbc.queryForList("""
                SELECT analysis_run_id
                  FROM an_analysis_run
                 WHERE stock_code = :stockCode
                   AND analysis_mode = :analysisMode
                   AND base_date = :baseDate
                   AND status = 'COMPLETED'
                 ORDER BY created_at DESC
                 FETCH FIRST 1 ROWS ONLY
                """, new MapSqlParameterSource()
                .addValue("stockCode", stockCode)
                .addValue("analysisMode", mode.name())
                .addValue("baseDate", Date.valueOf(baseDate)), Long.class);
        return ids.isEmpty() ? Optional.empty() : findById(ids.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisRunView> findHistory(
            String stockCode,
            AnalysisMode mode,
            int offset,
            int size
    ) {
        List<Long> ids = jdbc.queryForList("""
                SELECT analysis_run_id
                  FROM an_analysis_run
                 WHERE stock_code = :stockCode AND analysis_mode = :analysisMode
                 ORDER BY created_at DESC
                 OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """, new MapSqlParameterSource()
                .addValue("stockCode", stockCode)
                .addValue("analysisMode", mode.name())
                .addValue("offset", offset)
                .addValue("size", size), Long.class);
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countHistory(String stockCode, AnalysisMode mode) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM an_analysis_run
                 WHERE stock_code = :stockCode AND analysis_mode = :analysisMode
                """, Map.of("stockCode", stockCode, "analysisMode", mode.name()), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockPricePoint> findPrices(
            String stockCode,
            LocalDate from,
            LocalDate to
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT business_date, open_price_krw, high_price_krw,
                       low_price_krw, close_price_krw, volume
                  FROM (
                    SELECT c.*, ROW_NUMBER() OVER (
                        PARTITION BY c.stock_code, c.business_date
                        ORDER BY c.observed_at DESC, c.stock_daily_candle_id DESC
                    ) AS rn
                      FROM an_stock_daily_candle c
                     WHERE c.stock_code = :stockCode
                """);
        MapSqlParameterSource params = new MapSqlParameterSource("stockCode", stockCode);
        if (from != null) {
            sql.append(" AND c.business_date >= :fromDate");
            params.addValue("fromDate", Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND c.business_date <= :toDate");
            params.addValue("toDate", Date.valueOf(to));
        }
        sql.append(") WHERE rn = 1 ORDER BY business_date");
        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new StockPricePoint(
                rs.getDate("business_date").toLocalDate(),
                rs.getBigDecimal("open_price_krw"),
                rs.getBigDecimal("high_price_krw"),
                rs.getBigDecimal("low_price_krw"),
                rs.getBigDecimal("close_price_krw"),
                rs.getLong("volume")
        ));
    }

    private long saveSource(long runId, AnalysisSourcePayload source) {
        String requestJson = json(source.request());
        String responseJson = json(source.response());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("apiId", source.apiId())
                .addValue("dtoType", source.responseDtoType())
                .addValue("stockCode", source.records().stream()
                        .map(BackdataRecord::stockCode).filter(java.util.Objects::nonNull)
                        .findFirst().orElse(null))
                .addValue("referenceCode", source.records().stream()
                        .map(BackdataRecord::referenceCode).filter(java.util.Objects::nonNull)
                        .findFirst().orElse(null))
                .addValue("requestHash", hash(requestJson))
                .addValue("responseHash", hash(responseJson))
                .addValue("requestJson", requestJson)
                .addValue("responseJson", responseJson)
                .addValue("returnCode", source.response().getReturnCode())
                .addValue("returnMessage", source.response().getReturnMsg())
                .addValue("contYn", source.continuation().hasNext() ? "Y" : "N")
                .addValue("nextKey", source.continuation().nextKey())
                .addValue("recordCount", source.records().size())
                .addValue("requestedAt", dbTimestamp(source.requestedAt()))
                .addValue("receivedAt", dbTimestamp(source.receivedAt()));
        long responseId = insertKey("""
                INSERT INTO an_tr_response (
                    api_id, response_dto_type, stock_code, reference_code,
                    request_hash, response_hash, request_json, response_json,
                    return_code, return_message, cont_yn, next_key, page_no,
                    record_count, response_status, requested_at, received_at, created_at
                ) VALUES (
                    :apiId, :dtoType, :stockCode, :referenceCode,
                    :requestHash, :responseHash, :requestJson, :responseJson,
                    :returnCode, :returnMessage, :contYn, :nextKey, 1,
                    :recordCount, 'VALID', :requestedAt, :receivedAt, SYSTIMESTAMP
                )
                """, params, "TR_RESPONSE_ID");
        jdbc.update("""
                INSERT INTO an_analysis_source (
                    analysis_run_id, tr_response_id, source_role,
                    required_yn, selected_yn, created_at
                ) VALUES (:runId, :responseId, :sourceRole, :requiredYn, 'Y', SYSTIMESTAMP)
                """, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("responseId", responseId)
                .addValue("sourceRole", source.sourceRole())
                .addValue("requiredYn", isRequired(source.apiId()) ? "Y" : "N"));
        return responseId;
    }

    private void saveTypedRecords(long responseId, AnalysisSourcePayload source) {
        switch (source.apiId()) {
            case "ka10001" -> saveBasic(responseId, source.records());
            case "ka10081" -> saveCandles(responseId, source.records());
            case "ka10082" -> saveTimeframeCandles(responseId, "WEEK", source.records());
            case "ka10083" -> saveTimeframeCandles(responseId, "MONTH", source.records());
            case "ka10086" -> saveDailyPrices(responseId, source.records());
            case "ka10059" -> saveInvestorFlow(responseId, source.records());
            default -> {
                // The raw response remains available even when no typed projection is needed.
            }
        }
    }

    private void saveBasic(long responseId, List<BackdataRecord> records) {
        if (records.isEmpty()) return;
        BackdataRecord record = records.get(0);
        jdbc.update("""
                INSERT INTO an_stock_basic_snap (
                    tr_response_id, stock_code, stock_name, listed_shares,
                    market_cap_100m_krw, per_value, eps_krw, roe_rate, pbr_value,
                    bps_krw, revenue_100m_krw, op_profit_100m_krw,
                    current_price_krw, volume, observed_at, created_at
                ) VALUES (
                    :responseId, :stockCode, :stockName, :listedShares,
                    :marketCap, :perValue, :eps, :roe, :pbr,
                    :bps, :revenue, :operatingProfit,
                    :currentPrice, :volume, :observedAt, SYSTIMESTAMP
                )
                """, recordParams(responseId, record)
                .addValue("stockName", record.value("name"))
                .addValue("listedShares", decimal(record, "listedShares"))
                .addValue("marketCap", decimal(record, "marketCap"))
                .addValue("perValue", decimal(record, "per"))
                .addValue("eps", decimal(record, "eps"))
                .addValue("roe", decimal(record, "roe"))
                .addValue("pbr", decimal(record, "pbr"))
                .addValue("bps", decimal(record, "bps"))
                .addValue("revenue", decimal(record, "revenue"))
                .addValue("operatingProfit", decimal(record, "operatingProfit"))
                .addValue("currentPrice", decimal(record, "currentPrice"))
                .addValue("volume", decimal(record, "volume")));
    }

    private void saveCandles(long responseId, List<BackdataRecord> records) {
        int rowNo = 0;
        for (BackdataRecord record : records) {
            if (record.businessDate() == null) continue;
            jdbc.update("""
                    INSERT INTO an_stock_daily_candle (
                        tr_response_id, row_no, stock_code, business_date,
                        price_variant, close_price_krw, volume, trading_amt_mil_krw,
                        open_price_krw, high_price_krw, low_price_krw,
                        change_price_krw, turnover_rate, observed_at, created_at
                    ) VALUES (
                        :responseId, :rowNo, :stockCode, :businessDate,
                        :variant, :closePrice, :volume, :tradingAmount,
                        :openPrice, :highPrice, :lowPrice,
                        :changePrice, :turnoverRate, :observedAt, SYSTIMESTAMP
                    )
                    """, recordParams(responseId, record)
                    .addValue("rowNo", rowNo++)
                    .addValue("businessDate", Date.valueOf(record.businessDate()))
                    .addValue("variant", variant(record, "upd_stkpc_tp", "1"))
                    .addValue("closePrice", decimal(record, "close"))
                    .addValue("volume", decimal(record, "volume"))
                    .addValue("tradingAmount", decimal(record, "tradingAmount"))
                    .addValue("openPrice", decimal(record, "open"))
                    .addValue("highPrice", decimal(record, "high"))
                    .addValue("lowPrice", decimal(record, "low"))
                    .addValue("changePrice", decimal(record, "change"))
                    .addValue("turnoverRate", decimal(record, "turnoverRate")));
        }
    }

    private void saveDailyPrices(long responseId, List<BackdataRecord> records) {
        int rowNo = 0;
        for (BackdataRecord record : records) {
            if (record.businessDate() == null) continue;
            jdbc.update("""
                    INSERT INTO an_stock_daily_price (
                        tr_response_id, row_no, stock_code, business_date, indicator_type,
                        open_price_krw, high_price_krw, low_price_krw, close_price_krw,
                        volume, amount_mil_krw, credit_rate, individual_value,
                        institution_value, foreign_quantity, foreign_value, program_value,
                        foreign_rate, foreign_holding, foreign_weight, foreign_net_buy,
                        institution_net_buy, individual_net_buy, credit_balance_rate,
                        observed_at, created_at
                    ) VALUES (
                        :responseId, :rowNo, :stockCode, :businessDate, :variant,
                        :openPrice, :highPrice, :lowPrice, :closePrice,
                        :volume, :amount, :creditRate, :individual,
                        :institution, :foreignQuantity, :foreignValue, :programValue,
                        :foreignRate, :foreignHolding, :foreignWeight, :foreignNetBuy,
                        :institutionNetBuy, :individualNetBuy, :creditBalanceRate,
                        :observedAt, SYSTIMESTAMP
                    )
                    """, recordParams(responseId, record)
                    .addValue("rowNo", rowNo++)
                    .addValue("businessDate", Date.valueOf(record.businessDate()))
                    .addValue("variant", variant(record, "indc_tp", "0"))
                    .addValue("openPrice", decimal(record, "open"))
                    .addValue("highPrice", decimal(record, "high"))
                    .addValue("lowPrice", decimal(record, "low"))
                    .addValue("closePrice", decimal(record, "close"))
                    .addValue("volume", decimal(record, "volume"))
                    .addValue("amount", decimal(record, "tradingAmount"))
                    .addValue("creditRate", decimal(record, "creditRatio"))
                    .addValue("individual", decimal(record, "individual"))
                    .addValue("institution", decimal(record, "institution"))
                    .addValue("foreignQuantity", decimal(record, "foreignQuantity"))
                    .addValue("foreignValue", decimal(record, "foreigner"))
                    .addValue("programValue", decimal(record, "program"))
                    .addValue("foreignRate", decimal(record, "foreignRatio"))
                    .addValue("foreignHolding", decimal(record, "foreignHolding"))
                    .addValue("foreignWeight", decimal(record, "foreignHoldingRatio"))
                    .addValue("foreignNetBuy", decimal(record, "foreignNetBuy"))
                    .addValue("institutionNetBuy", decimal(record, "institutionNetBuy"))
                    .addValue("individualNetBuy", decimal(record, "individualNetBuy"))
                    .addValue("creditBalanceRate", decimal(record, "creditBalanceRatio")));
        }
    }

    private void saveTimeframeCandles(
            long responseId,
            String timeframeCode,
            List<BackdataRecord> records
    ) {
        int rowNo = 0;
        for (BackdataRecord record : records) {
            if (record.businessDate() == null) continue;
            jdbc.update("""
                    INSERT INTO an_stock_timeframe_candle (
                        tr_response_id, row_no, timeframe_code, stock_code, business_date,
                        price_variant, close_price_krw, volume, trading_amt_mil_krw,
                        open_price_krw, high_price_krw, low_price_krw,
                        change_price_krw, turnover_rate, observed_at, created_at
                    ) VALUES (
                        :responseId, :rowNo, :timeframeCode, :stockCode, :businessDate,
                        :variant, :closePrice, :volume, :tradingAmount,
                        :openPrice, :highPrice, :lowPrice,
                        :changePrice, :turnoverRate, :observedAt, SYSTIMESTAMP
                    )
                    """, recordParams(responseId, record)
                    .addValue("rowNo", rowNo++)
                    .addValue("timeframeCode", timeframeCode)
                    .addValue("businessDate", Date.valueOf(record.businessDate()))
                    .addValue("variant", variant(record, "upd_stkpc_tp", "1"))
                    .addValue("closePrice", decimal(record, "close"))
                    .addValue("volume", decimal(record, "volume"))
                    .addValue("tradingAmount", decimal(record, "tradingAmount"))
                    .addValue("openPrice", decimal(record, "open"))
                    .addValue("highPrice", decimal(record, "high"))
                    .addValue("lowPrice", decimal(record, "low"))
                    .addValue("changePrice", decimal(record, "change"))
                    .addValue("turnoverRate", decimal(record, "turnoverRate")));
        }
    }

    private void saveInvestorFlow(long responseId, List<BackdataRecord> records) {
        int rowNo = 0;
        for (BackdataRecord record : records) {
            if (record.businessDate() == null) continue;
            jdbc.update("""
                    INSERT INTO an_investor_daily (
                        tr_response_id, row_no, stock_code, business_date,
                        metric_basis, trade_type, value_unit,
                        current_price_krw, accumulated_volume, accumulated_amt_mil_krw,
                        individual_value, foreign_value, institution_value,
                        financial_investment, insurance_value, investment_trust,
                        other_finance, bank_value, pension_value, private_fund,
                        government_value, other_corporation, other_foreign,
                        observed_at, created_at
                    ) VALUES (
                        :responseId, :rowNo, :stockCode, :businessDate,
                        :metricBasis, :tradeType, :valueUnit,
                        :currentPrice, :volume, :tradingAmount,
                        :individual, :foreigner, :institution,
                        :financialInvestment, :insurance, :investmentTrust,
                        :otherFinance, :bank, :pension, :privateFund,
                        :government, :otherCorporation, :otherForeign,
                        :observedAt, SYSTIMESTAMP
                    )
                    """, recordParams(responseId, record)
                    .addValue("rowNo", rowNo++)
                    .addValue("businessDate", Date.valueOf(record.businessDate()))
                    .addValue("metricBasis", variant(record, "amt_qty_tp", "2"))
                    .addValue("tradeType", variant(record, "trde_tp", "0"))
                    .addValue("valueUnit", variant(record, "unit_tp", "1"))
                    .addValue("currentPrice", decimal(record, "close"))
                    .addValue("volume", decimal(record, "volume"))
                    .addValue("tradingAmount", decimal(record, "tradingAmount"))
                    .addValue("individual", decimal(record, "individual"))
                    .addValue("foreigner", decimal(record, "foreigner"))
                    .addValue("institution", decimal(record, "institution"))
                    .addValue("financialInvestment", decimal(record, "financialInvestment"))
                    .addValue("insurance", decimal(record, "insurance"))
                    .addValue("investmentTrust", decimal(record, "investmentTrust"))
                    .addValue("otherFinance", decimal(record, "otherFinance"))
                    .addValue("bank", decimal(record, "bank"))
                    .addValue("pension", decimal(record, "pension"))
                    .addValue("privateFund", decimal(record, "privateFund"))
                    .addValue("government", decimal(record, "government"))
                    .addValue("otherCorporation", decimal(record, "otherCorporation"))
                    .addValue("otherForeign", decimal(record, "otherForeign")));
        }
    }

    private void saveResult(long runId, CollectedAnalysisData data, AnalysisResult result) {
        jdbc.update("""
                INSERT INTO an_analysis_result (
                    analysis_run_id, stock_name, market_type, total_score, score_grade,
                    strategy_code, strategy_name, strategy_action, current_price_krw,
                    historical_samples, result_summary, created_at, updated_at
                ) VALUES (
                    :runId, :stockName, :marketType, :totalScore, :scoreGrade,
                    :strategyCode, :strategyName, :strategyAction, :currentPrice,
                    :samples, :summary, SYSTIMESTAMP, SYSTIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("stockName", data.input().stock().stockName())
                .addValue("marketType", data.input().stock().marketCode())
                .addValue("totalScore", result.totalScore())
                .addValue("scoreGrade", grade(result.totalScore()))
                .addValue("strategyCode", result.strategy().strategyCode())
                .addValue("strategyName", result.strategy().strategyName())
                .addValue("strategyAction", result.strategy().action().name())
                .addValue("currentPrice", data.input().stock().currentPrice())
                .addValue("samples", data.input().dailyCandles().size())
                .addValue("summary", result.strategy().strategyName()));
    }

    private void saveFactors(long runId, CollectedAnalysisData data, AnalysisResult result) {
        for (FactorResult factor : result.factors()) {
            BigDecimal contribution = factor.score() == null ? null : factor.score().multiply(factor.weight())
                    .setScale(8, RoundingMode.HALF_UP);
            jdbc.update("""
                    INSERT INTO an_factor_result (
                        analysis_run_id, factor_code, factor_version, raw_value,
                        score, weight, contribution, sample_count, data_status,
                        reason, created_at
                    ) VALUES (
                        :runId, :factorCode, :factorVersion, :rawValue,
                        :score, :weight, :contribution, :sampleCount, :dataStatus,
                        :reason, SYSTIMESTAMP
                    )
                    """, new MapSqlParameterSource()
                    .addValue("runId", runId)
                    .addValue("factorCode", factor.factorCode())
                    .addValue("factorVersion", result.engineVersion())
                    .addValue("dataStatus", factor.score() == null ? "MISSING" : "VALID")
                    .addValue("rawValue", factor.rawValue())
                    .addValue("score", factor.score())
                    .addValue("weight", factor.weight())
                    .addValue("contribution", contribution)
                    .addValue("sampleCount", sampleCount(data, factor.factorCode()))
                    .addValue("reason", factor.reason()));
        }
    }

    private long saveScenario(long runId, AnalysisResult result) {
        BigDecimal entryBasis = isSuitability(result)
                ? result.scenario().entryTo() : result.scenario().entryFrom();
        BigDecimal riskAmount = entryBasis.subtract(result.scenario().stopLoss());
        BigDecimal riskRate = riskAmount.multiply(BigDecimal.valueOf(100))
                .divide(entryBasis, 8, RoundingMode.HALF_UP);
        return insertKey("""
                INSERT INTO an_trade_scenario (
                    analysis_run_id, scenario_code, scenario_name,
                    entry_from_krw, entry_to_krw, stop_loss_krw,
                    risk_amount_krw, risk_rate, rationale, created_at
                ) VALUES (
                    :runId, 'BASE', '기본 시나리오',
                    :entryFrom, :entryTo, :stopLoss,
                    :riskAmount, :riskRate, :rationale, SYSTIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("entryFrom", result.scenario().entryFrom())
                .addValue("entryTo", result.scenario().entryTo())
                .addValue("stopLoss", result.scenario().stopLoss())
                .addValue("riskAmount", riskAmount)
                .addValue("riskRate", riskRate)
                .addValue("rationale", isSuitability(result)
                        ? "관측 가격 구조와 ATR 완충폭; 유형별 상세 근거는 분석 사유 참조; 진입 상단 기준 위험 계산"
                        : "팩터 점수와 최근 14거래일 가격 변동폭을 반영"),
                "SCENARIO_ID");
    }

    private void saveTargets(long scenarioId, AnalysisResult result) {
        BigDecimal risk = (isSuitability(result)
                ? result.scenario().entryTo() : result.scenario().entryFrom()).subtract(result.scenario().stopLoss());
        int order = 1;
        for (BigDecimal target : result.scenario().targets()) {
            BigDecimal expectedReturn = target.subtract(result.scenario().entryTo())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(result.scenario().entryTo(), 8, RoundingMode.HALF_UP);
            BigDecimal rewardRisk = risk.signum() == 0 ? null
                    : target.subtract(result.scenario().entryTo())
                            .divide(risk, 8, RoundingMode.HALF_UP);
            jdbc.update("""
                    INSERT INTO an_target_price (
                        scenario_id, target_order, target_price_krw,
                        expected_return_rate, risk_reward_ratio, created_at
                    ) VALUES (
                        :scenarioId, :targetOrder, :targetPrice,
                        :expectedReturn, :rewardRisk, SYSTIMESTAMP
                    )
                    """, new MapSqlParameterSource()
                    .addValue("scenarioId", scenarioId)
                    .addValue("targetOrder", order++)
                    .addValue("targetPrice", target)
                    .addValue("expectedReturn", expectedReturn)
                    .addValue("rewardRisk", rewardRisk));
        }
    }

    private void saveReasons(long runId, AnalysisResult result) {
        if (result.scenario()!=null && result.scenario().targetBasis()!=null) {
            jdbc.update("""
                    INSERT INTO an_analysis_reason (analysis_run_id, reason_type, reason_code,
                        reason_message, display_order, details_json, created_at)
                    VALUES (:runId, 'TARGET_BASIS', 'TARGET_BASIS_V1', '목표가 산출 근거', 0, :details, SYSTIMESTAMP)
                    """,new MapSqlParameterSource("runId",runId)
                    .addValue("details",json(result.scenario().targetBasis()),java.sql.Types.CLOB));
        }
        if (result.signalReport() != null) {
            jdbc.update("""
                    INSERT INTO an_analysis_reason (analysis_run_id, reason_type, reason_code,
                        reason_message, display_order, details_json, created_at)
                    VALUES (:runId, 'SIGNAL_REPORT', 'SWING_SIGNALS_V1', '매매 신호 스냅샷', 0, :details, SYSTIMESTAMP)
                    """, new MapSqlParameterSource("runId", runId)
                    .addValue("details", json(result.signalReport()), java.sql.Types.CLOB));
        }
        int displayOrder = 1;
        for (String reason : result.strategy().reasons()) {
            insertReason(runId, "REASON", "STRATEGY_" + displayOrder,
                    reason, displayOrder++);
        }
        int warningOrder = 1;
        for (String warning : result.warnings()) {
            insertReason(runId, "WARNING", "WARNING_" + warningOrder,
                    warning, displayOrder + warningOrder - 1);
            warningOrder++;
        }
    }

    private boolean isSuitability(AnalysisResult result) {
        return java.util.Set.of("analysis-v4-swing", "analysis-v5-swing", "analysis-v6-swing", "analysis-v7-swing").contains(result.engineVersion());
    }

    private void insertReason(
            long runId,
            String type,
            String code,
            String message,
            int order
    ) {
        jdbc.update("""
                INSERT INTO an_analysis_reason (
                    analysis_run_id, reason_type, reason_code,
                    reason_message, display_order, created_at
                ) VALUES (
                    :runId, :type, :code, :message, :displayOrder, SYSTIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("type", type)
                .addValue("code", code)
                .addValue("message", message)
                .addValue("displayOrder", order));
    }

    private Optional<Header> findHeader(String sql, Map<String, ?> params) {
        List<Header> rows = jdbc.query(sql, params, (rs, rowNum) -> {
            int historicalSamples = rs.getInt("historical_samples");
            if (rs.wasNull()) historicalSamples = 0;
            return new Header(
                    rs.getLong("analysis_run_id"),
                    rs.getString("stock_code"),
                    rs.getString("stock_name"),
                    AnalysisMode.valueOf(rs.getString("analysis_mode")),
                    AnalysisStatus.valueOf(rs.getString("status")),
                    DataQualityStatus.valueOf(rs.getString("data_quality_status")),
                    rs.getDate("base_date").toLocalDate(),
                    rs.getString("engine_version"),
                    rs.getBigDecimal("total_score"),
                    rs.getString("score_grade"),
                    rs.getString("strategy_code"),
                    rs.getString("strategy_name"),
                    rs.getString("strategy_action"),
                    rs.getBigDecimal("current_price_krw"),
                    rs.getBigDecimal("entry_from_krw"),
                    rs.getBigDecimal("entry_to_krw"),
                    rs.getBigDecimal("stop_loss_krw"),
                    historicalSamples,
                    rs.getString("error_code"),
                    rs.getString("error_message"),
                    instant(rs.getTimestamp("started_at")),
                    instant(rs.getTimestamp("completed_at"))
            );
        });
        return rows.stream().findFirst();
    }

    private AnalysisRunView toView(Header header) {
        List<AnalysisFactorView> factors = jdbc.query("""
                SELECT factor_code, raw_value, score, weight, contribution, reason
                  FROM an_factor_result
                 WHERE analysis_run_id = :runId
                 ORDER BY contribution DESC
                """, Map.of("runId", header.runId()), (rs, rowNum) -> new AnalysisFactorView(
                rs.getString("factor_code"),
                rs.getBigDecimal("raw_value"),
                rs.getBigDecimal("score"),
                rs.getBigDecimal("weight"),
                rs.getBigDecimal("contribution"),
                rs.getString("reason")
        ));
        List<BigDecimal> targets = jdbc.queryForList("""
                SELECT t.target_price_krw
                  FROM an_target_price t
                  JOIN an_trade_scenario s ON s.scenario_id = t.scenario_id
                 WHERE s.analysis_run_id = :runId AND s.scenario_code = 'BASE'
                 ORDER BY t.target_order
                """, Map.of("runId", header.runId()), BigDecimal.class);
        List<ReasonRow> reasonRows = jdbc.query("""
                SELECT reason_type, reason_message
                  FROM an_analysis_reason
                 WHERE analysis_run_id = :runId
                 ORDER BY display_order, analysis_reason_id
                """, Map.of("runId", header.runId()), (rs, rowNum) -> new ReasonRow(
                rs.getString("reason_type"), rs.getString("reason_message")
        ));
        List<String> reasons = reasonRows.stream()
                .filter(reason -> "REASON".equals(reason.type()))
                .map(ReasonRow::message).toList();
        List<String> warnings = reasonRows.stream()
                .filter(reason -> "WARNING".equals(reason.type()))
                .map(ReasonRow::message).toList();
        return new AnalysisRunView(
                header.runId(), header.stockCode(), header.stockName(), header.mode(),
                header.status(), header.quality(), header.baseDate(), header.engineVersion(),
                header.totalScore(), header.scoreGrade(), header.strategyCode(),
                header.strategyName(), header.strategyAction(), header.currentPrice(),
                header.entryFrom(), header.entryTo(), targets, header.stopLoss(),
                header.historicalSamples(), factors, reasons, warnings,
                header.errorCode(), header.errorMessage(), header.startedAt(), header.completedAt(), readSignals(header.runId()), readJudgment(header.runId()),null,readTargetBasis(header.runId(),warnings)
        );
    }

    private com.example.stockanalysis.judgment.TradeJudgment readJudgment(long runId) {
        List<String> reports = jdbc.queryForList("""
                SELECT details_json FROM an_analysis_reason
                 WHERE analysis_run_id = :runId AND reason_type = 'TRADE_JUDGMENT' AND reason_code = 'TRADE_JUDGMENT_V1'
                """, Map.of("runId", runId), String.class);
        if (reports.isEmpty()) return null;
        try { return objectMapper.readValue(reports.get(0), com.example.stockanalysis.judgment.TradeJudgment.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Invalid stored trade judgment", e); }
    }

    private com.example.stockanalysis.engine.model.TradeScenario.TargetBasis readTargetBasis(long runId,List<String> warnings) {
        var saved=jdbc.queryForList("""
                SELECT details_json FROM an_analysis_reason
                 WHERE analysis_run_id = :runId AND reason_type = 'TARGET_BASIS' AND reason_code = 'TARGET_BASIS_V1'
                """,Map.of("runId",runId),String.class);
        if(saved.isEmpty()) return warnings.stream().anyMatch(w->w.startsWith("SYNTHETIC_TARGET:"))
                ?com.example.stockanalysis.engine.model.TradeScenario.TargetBasis.STRUCTURAL_RISK_MULTIPLE:null;
        try { return objectMapper.readValue(saved.get(0),com.example.stockanalysis.engine.model.TradeScenario.TargetBasis.class); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid stored target basis",e); }
    }

    private com.example.stockanalysis.engine.model.SignalReport readSignals(long runId) {
        List<String> reports = jdbc.queryForList("""
                SELECT details_json FROM an_analysis_reason
                 WHERE analysis_run_id = :runId AND reason_type = 'SIGNAL_REPORT' AND reason_code = 'SWING_SIGNALS_V1'
                """, Map.of("runId", runId), String.class);
        if (reports.isEmpty()) return null;
        try { return objectMapper.readValue(reports.get(0), com.example.stockanalysis.engine.model.SignalReport.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Invalid stored signal report", e); }
    }

    private long insertKey(
            String sql,
            MapSqlParameterSource params,
            String keyColumn
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{keyColumn});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("generated key is missing: " + keyColumn);
        }
        return key.longValue();
    }

    private MapSqlParameterSource recordParams(long responseId, BackdataRecord record) {
        return new MapSqlParameterSource()
                .addValue("responseId", responseId)
                .addValue("stockCode", record.stockCode())
                .addValue("observedAt", dbTimestamp(record.observedAt()));
    }

    private BigDecimal decimal(BackdataRecord record, String field) {
        String value = record.value(field);
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value.trim().replace(",", ""));
    }

    private String variant(BackdataRecord record, String field, String fallback) {
        if (record.variant() == null) return fallback;
        for (String part : record.variant().split("\\|")) {
            String prefix = field + "=";
            if (part.startsWith(prefix) && part.length() > prefix.length()) {
                return part.substring(prefix.length());
            }
        }
        return fallback;
    }

    private boolean isRequired(String apiId) {
        return "ka10001".equals(apiId)
                || "ka10081".equals(apiId)
                || "ka10082".equals(apiId)
                || "ka10083".equals(apiId);
    }

    private int sampleCount(CollectedAnalysisData data, String factorCode) {
        return switch (factorCode) {
            case "WEEKLY_TECHNICAL" -> data.input().weeklyCandles().size();
            case "MONTHLY_TECHNICAL" -> data.input().monthlyCandles().size();
            default -> data.input().dailyCandles().size();
        };
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("analysis payload serialization failed", exception);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String grade(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(65)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) return "C";
        return "D";
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime dbTimestamp(Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private record Header(
            long runId,
            String stockCode,
            String stockName,
            AnalysisMode mode,
            AnalysisStatus status,
            DataQualityStatus quality,
            LocalDate baseDate,
            String engineVersion,
            BigDecimal totalScore,
            String scoreGrade,
            String strategyCode,
            String strategyName,
            String strategyAction,
            BigDecimal currentPrice,
            BigDecimal entryFrom,
            BigDecimal entryTo,
            BigDecimal stopLoss,
            int historicalSamples,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {
    }

    private record ReasonRow(String type, String message) {
    }
}
