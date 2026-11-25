package com.example.stockanalysis.analysis.infrastructure.kiwoom;

import com.example.stockanalysis.analysis.application.model.AnalysisSourcePayload;
import com.example.stockanalysis.analysis.application.model.CollectedAnalysisData;
import com.example.stockanalysis.analysis.application.port.out.AnalysisDataProvider;
import com.example.stockanalysis.analysis.domain.DataQualityStatus;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCallResult;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataClient;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataRecordMapper;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataTrDefinition;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.engine.factor.internal.SupplyDemandFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.RiskStabilityFactorCalculator;
import com.example.stockanalysis.engine.factor.internal.ValueQualityFactorCalculator;
import com.example.stockanalysis.engine.model.AnalysisInput;
import com.example.stockanalysis.engine.model.AnalysisMode;
import com.example.stockanalysis.engine.model.DailyCandle;
import com.example.stockanalysis.engine.model.StockSnapshot;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KiwoomAnalysisDataProvider implements AnalysisDataProvider {

    private static final Logger log = LoggerFactory.getLogger(KiwoomAnalysisDataProvider.class);
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final KiwoomBackdataCatalog catalog;
    private final KiwoomBackdataClient client;
    private final KiwoomBackdataRecordMapper recordMapper;
    private final Clock clock;

    public KiwoomAnalysisDataProvider(
            KiwoomBackdataCatalog catalog,
            KiwoomBackdataClient client,
            KiwoomBackdataRecordMapper recordMapper,
            Clock clock
    ) {
        this.catalog = catalog;
        this.client = client;
        this.recordMapper = recordMapper;
        this.clock = clock;
    }

    @Override
    public CollectedAnalysisData collect(
            String stockCode,
            AnalysisMode mode,
            LocalDate baseDate
    ) {
        return collect(stockCode, mode, baseDate, null);
    }

    @Override
    public CollectedAnalysisData collectDaily(String stockCode, LocalDate baseDate) {
        var source = dailySource(stockCode, baseDate);
        var warnings = new ArrayList<String>();
        var candles = toCandles(source.records(), baseDate, warnings, "일봉");
        if (candles.isEmpty()) throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "분석 가능한 일봉 데이터가 없습니다.");
        var latest = candles.get(candles.size() - 1);
        var input = new AnalysisInput(new StockSnapshot(stockCode, stockCode, "KRX", latest.close()),
                AnalysisMode.SWING, baseDate, clock.instant(), candles, Map.of());
        return new CollectedAnalysisData(input, List.of(source), warnings,
                warnings.isEmpty() ? DataQualityStatus.COMPLETE : DataQualityStatus.PARTIAL);
    }

    @Override
    public CollectedAnalysisData completeDaily(CollectedAnalysisData daily) {
        var input = daily.input();
        if (input.mode() != AnalysisMode.SWING) throw new IllegalArgumentException("Expected swing daily sample");
        var sources = daily.sources().stream().filter(s -> "ka10081".equals(s.apiId())).toList();
        if (sources.size() != 1) throw new IllegalArgumentException("Expected one verified daily source");
        var source = sources.get(0);
        if (!input.stock().stockCode().equals(source.request().get("stk_cd"))
                || !input.baseDate().format(BASIC_DATE).equals(source.request().get("base_dt")))
            throw new IllegalArgumentException("Daily sample identity mismatch");
        return collect(input.stock().stockCode(), input.mode(), input.baseDate(), source);
    }

    private AnalysisSourcePayload dailySource(String stockCode, LocalDate baseDate) {
        return call("ka10081", "DAILY_CANDLE", Map.of("stk_cd", stockCode,
                "base_dt", baseDate.format(BASIC_DATE), "upd_stkpc_tp", "1"), baseDate);
    }

    private CollectedAnalysisData collect(String stockCode, AnalysisMode mode, LocalDate baseDate,
            AnalysisSourcePayload verifiedDaily) {
        List<AnalysisSourcePayload> sources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        AnalysisSourcePayload basic = call(
                "ka10001",
                "STOCK_BASIC",
                Map.of("stk_cd", stockCode),
                baseDate
        );
        sources.add(basic);

        AnalysisSourcePayload candles = verifiedDaily == null ? dailySource(stockCode, baseDate) : verifiedDaily;
        sources.add(candles);

        AnalysisSourcePayload weeklyCandlesSource = call(
                "ka10082",
                "WEEKLY_CANDLE",
                Map.of(
                        "stk_cd", stockCode,
                        "base_dt", baseDate.format(BASIC_DATE),
                        "upd_stkpc_tp", "1"
                ),
                baseDate
        );
        sources.add(weeklyCandlesSource);

        AnalysisSourcePayload monthlyCandlesSource = call(
                "ka10083",
                "MONTHLY_CANDLE",
                Map.of(
                        "stk_cd", stockCode,
                        "base_dt", baseDate.format(BASIC_DATE),
                        "upd_stkpc_tp", "1"
                ),
                baseDate
        );
        sources.add(monthlyCandlesSource);

        collectOptional(
                sources,
                warnings,
                "ka10086",
                "DAILY_PRICE_FLOW",
                Map.of(
                        "stk_cd", stockCode,
                        "qry_dt", baseDate.format(BASIC_DATE),
                        "indc_tp", "0"
                ),
                baseDate
        );
        collectOptional(
                sources,
                warnings,
                "ka10059",
                "INVESTOR_FLOW",
                Map.of(
                        "dt", baseDate.format(BASIC_DATE),
                        "stk_cd", stockCode,
                        "amt_qty_tp", "2",
                        "trde_tp", "0",
                        "unit_tp", "1"
                ),
                baseDate
        );

        List<DailyCandle> dailyCandles = toCandles(
                candles.records(), baseDate, warnings, "일봉"
        );
        if (dailyCandles.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "분석 가능한 일봉 데이터가 없습니다."
            );
        }
        List<DailyCandle> weeklyCandles = toCandles(
                weeklyCandlesSource.records(), baseDate, warnings, "주봉"
        );
        List<DailyCandle> monthlyCandles = toCandles(
                monthlyCandlesSource.records(), baseDate, warnings, "월봉"
        );
        if (mode == AnalysisMode.SWING) {
            if (dailyCandles.size() < 60 || weeklyCandles.size() < 20)
                warnings.add("스윙 핵심 표본 부족: 일봉 60개·주봉 20개가 필요합니다. 분석을 보류합니다.");
            if (monthlyCandles.size() < 20) warnings.add(DataQualityStatus.MONTHLY_REFERENCE_SHORTAGE);
        } else {
            requireCandles(weeklyCandles, "주봉");
            requireCandles(monthlyCandles, "월봉");
            warnIfShort(warnings, dailyCandles, 180, "일봉");
            warnIfShort(warnings, weeklyCandles, 40, "주봉");
            warnIfShort(warnings, monthlyCandles, 24, "월봉");
        }

        BackdataRecord basicRecord = basic.records().stream().findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "종목 기본정보 응답이 비어 있습니다."
                ));
        DailyCandle latestCandle = dailyCandles.stream()
                .max(Comparator.comparing(DailyCandle::businessDate))
                .orElseThrow();
        BigDecimal currentPrice = decimal(basicRecord.value("currentPrice"));
        if (currentPrice == null || currentPrice.signum() <= 0) {
            currentPrice = latestCandle.close();
            warnings.add("현재가가 없어 최신 일봉 종가를 사용했습니다.");
        }

        Map<String, BigDecimal> observations = observations(basicRecord, sources, dailyCandles);
        String stockName = textOrDefault(basicRecord.value("name"), stockCode);
        Instant cutoffAt = clock.instant();
        AnalysisInput input = new AnalysisInput(
                new StockSnapshot(stockCode, stockName, "KRX", currentPrice),
                mode,
                baseDate,
                cutoffAt,
                dailyCandles,
                weeklyCandles,
                monthlyCandles,
                observations
        );
        DataQualityStatus qualityStatus = DataQualityStatus.hasBlockingWarnings(mode, warnings)
                ? DataQualityStatus.PARTIAL
                : DataQualityStatus.COMPLETE;
        return new CollectedAnalysisData(input, sources, warnings, qualityStatus);
    }

    private void collectOptional(
            List<AnalysisSourcePayload> sources,
            List<String> warnings,
            String apiId,
            String sourceRole,
            Map<String, Object> request,
            LocalDate baseDate
    ) {
        try {
            sources.add(call(apiId, sourceRole, request, baseDate));
        } catch (RuntimeException exception) {
            log.warn("선택 분석 전문 수집 실패. apiId={}, causeType={}", apiId,
                    exception.getClass().getSimpleName());
            warnings.add(apiId + " 데이터가 없어 일부 팩터는 중립값으로 계산했습니다.");
        }
    }

    private AnalysisSourcePayload call(
            String apiId,
            String sourceRole,
            Map<String, Object> request,
            LocalDate baseDate
    ) {
        KiwoomBackdataTrDefinition definition = catalog.getRequired(apiId);
        Instant requestedAt = clock.instant();
        KiwoomBackdataCallResult result = client.execute(definition, request, "N", "");
        Instant receivedAt = clock.instant();
        List<BackdataRecord> records = recordMapper.map(
                definition,
                request,
                result.body(),
                baseDate,
                receivedAt
        );
        return new AnalysisSourcePayload(
                apiId,
                definition.responseType().getName(),
                sourceRole,
                request,
                result.body(),
                result.continuation(),
                records,
                requestedAt,
                receivedAt
        );
    }

    private List<DailyCandle> toCandles(
            List<BackdataRecord> records,
            LocalDate baseDate,
            List<String> warnings,
            String label
    ) {
        List<DailyCandle> candles = new ArrayList<>();
        for (BackdataRecord record : records) {
            if (record.businessDate() == null || record.businessDate().isAfter(baseDate)) {
                continue;
            }
            try {
                BigDecimal open = requiredPositive(record, "open");
                BigDecimal high = requiredPositive(record, "high");
                BigDecimal low = requiredPositive(record, "low");
                BigDecimal close = requiredPositive(record, "close");
                BigDecimal volumeValue = decimal(record.value("volume"));
                long volume = volumeValue == null ? 0L : volumeValue.abs().longValueExact();
                candles.add(new DailyCandle(
                        record.businessDate(), open, high, low, close, volume
                ));
            } catch (RuntimeException exception) {
                log.debug("분석 제외 일봉. date={}, causeType={}", record.businessDate(),
                        exception.getClass().getSimpleName());
            }
        }
        if (candles.size() < records.size()) {
            warnings.add("형식이 올바르지 않은 일부 " + label + "을 분석에서 제외했습니다.");
        }
        return candles.stream()
                .sorted(Comparator.comparing(DailyCandle::businessDate))
                .toList();
    }

    private void requireCandles(List<DailyCandle> candles, String label) {
        if (candles.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "분석 가능한 " + label + " 데이터가 없습니다."
            );
        }
    }

    private void warnIfShort(
            List<String> warnings,
            List<DailyCandle> candles,
            int recommendedCount,
            String label
    ) {
        if (candles.size() < recommendedCount) {
            warnings.add(
                    label + " 표본이 " + candles.size() + "개로 권장 "
                            + recommendedCount + "개보다 적습니다. 일부 기술지표는 중립 처리됩니다."
            );
        }
    }

    private Map<String, BigDecimal> observations(
            BackdataRecord basic,
            List<AnalysisSourcePayload> sources,
            List<DailyCandle> candles
    ) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        putIfPresent(values, ValueQualityFactorCalculator.ROE, decimal(basic.value("roe")));
        putIfPresent(values, ValueQualityFactorCalculator.PER, decimal(basic.value("per")));
        putIfPresent(values, ValueQualityFactorCalculator.PBR, decimal(basic.value("pbr")));

        List<BackdataRecord> flowRecords = sourceRecords(sources, "ka10059");
        if (flowRecords.isEmpty()) {
            flowRecords = sourceRecords(sources, "ka10086");
        }
        BigDecimal foreign = sum(flowRecords, "foreigner", "foreignNetBuy");
        BigDecimal institution = sum(flowRecords, "institution", "institutionNetBuy");
        values.put(SupplyDemandFactorCalculator.FOREIGN_NET_BUY, foreign);
        values.put(SupplyDemandFactorCalculator.INSTITUTION_NET_BUY, institution);
        List<DailyCandle> recent = candles.stream()
                .sorted(Comparator.comparing(DailyCandle::businessDate).reversed())
                .limit(20)
                .toList();
        BigDecimal averageVolume = recent.stream()
                .map(candle -> BigDecimal.valueOf(candle.volume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recent.size()), 8, java.math.RoundingMode.HALF_UP);
        values.put(SupplyDemandFactorCalculator.AVERAGE_VOLUME, averageVolume);
        addSwingFlow(values, sources, recent);

        sourceRecords(sources, "ka10086").stream()
                .map(record -> decimal(record.value("creditBalanceRatio")))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .ifPresent(value -> values.put(
                        RiskStabilityFactorCalculator.CREDIT_BALANCE_RATIO,
                        value
                ));
        return values;
    }

    void addSwingFlow(Map<String, BigDecimal> values, List<AnalysisSourcePayload> sources,
                              List<DailyCandle> recent) {
        if (recent.size() != 20) return;
        // ka10059 is explicitly requested in quantity units. Do not mix fallback units.
        Map<LocalDate, BackdataRecord> byDate = new LinkedHashMap<>();
        sources.stream().filter(s -> "ka10059".equals(s.apiId()))
                .flatMap(s -> s.records().stream()).filter(r -> r.businessDate() != null)
                .forEach(r -> byDate.putIfAbsent(r.businessDate(), r));
        BigDecimal net = BigDecimal.ZERO, volume = BigDecimal.ZERO;
        int positiveDays = 0;
        for (DailyCandle candle : recent) {
            BackdataRecord row = byDate.get(candle.businessDate());
            if (row == null) return;
            BigDecimal foreign = decimal(row.value("foreigner"));
            BigDecimal institution = decimal(row.value("institution"));
            if (foreign == null || institution == null) return;
            BigDecimal dayNet = foreign.add(institution);
            net = net.add(dayNet);
            volume = volume.add(BigDecimal.valueOf(candle.volume()));
            if (dayNet.signum() > 0) positiveDays++;
        }
        values.put("swingNetBuy20", net);
        values.put("swingFlowVolume20", volume);
        values.put("swingPositiveFlowDays20", BigDecimal.valueOf(positiveDays));
    }

    private List<BackdataRecord> sourceRecords(
            List<AnalysisSourcePayload> sources,
            String apiId
    ) {
        return sources.stream()
                .filter(source -> source.apiId().equals(apiId))
                .flatMap(source -> source.records().stream())
                .sorted(Comparator.comparing(
                        BackdataRecord::businessDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(20)
                .toList();
    }

    private BigDecimal sum(List<BackdataRecord> records, String primary, String fallback) {
        return records.stream()
                .map(record -> {
                    BigDecimal value = decimal(record.value(primary));
                    return value == null ? decimal(record.value(fallback)) : value;
                })
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal requiredPositive(BackdataRecord record, String field) {
        BigDecimal value = decimal(record.value(field));
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " is not positive");
        }
        return value.abs();
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim().replace(",", ""));
    }

    private void putIfPresent(Map<String, BigDecimal> target, String key, BigDecimal value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
