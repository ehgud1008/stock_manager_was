package com.example.stockanalysis.analysis.infrastructure.kiwoom;

import com.example.stockanalysis.analysis.application.port.out.StockChartDataProvider;
import com.example.stockanalysis.analysis.domain.StockChartPoint;
import com.example.stockanalysis.analysis.domain.StockPricePeriod;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataRecordMapper;
import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataTrDefinition;
import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.kiwoom.tr.api.KiwoomTrExecutionResponse;
import com.example.stockanalysis.kiwoom.tr.application.KiwoomTrExecutionService;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KiwoomStockChartDataProvider implements StockChartDataProvider {

    private static final Logger log = LoggerFactory.getLogger(KiwoomStockChartDataProvider.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter BASIC_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final KiwoomBackdataCatalog catalog;
    private final KiwoomTrExecutionService trExecutionService;
    private final KiwoomBackdataRecordMapper recordMapper;
    private final Clock clock;

    public KiwoomStockChartDataProvider(
            KiwoomBackdataCatalog catalog,
            KiwoomTrExecutionService trExecutionService,
            KiwoomBackdataRecordMapper recordMapper,
            Clock clock
    ) {
        this.catalog = catalog;
        this.trExecutionService = trExecutionService;
        this.recordMapper = recordMapper;
        this.clock = clock;
    }

    @Override
    public List<StockChartPoint> fetch(
            String stockCode,
            StockPricePeriod period,
            int minuteInterval,
            LocalDate baseDate
    ) {
        KiwoomBackdataTrDefinition definition = catalog.getRequired(period.apiId());
        Map<String, Object> request = request(stockCode, period, minuteInterval, baseDate);
        KiwoomTrExecutionResponse result = trExecutionService.executeWithServerAuthorization(
                definition.apiId(),
                "N",
                "",
                request
        );
        List<BackdataRecord> records = recordMapper.map(
                definition,
                request,
                result.body(),
                baseDate,
                clock.instant()
        );

        List<StockChartPoint> prices = records.stream()
                .map(record -> toPoint(record, period))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StockChartPoint::timestamp))
                .toList();
        if (prices.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    period.name() + " 가격 데이터가 없습니다."
            );
        }
        return prices;
    }

    private Map<String, Object> request(
            String stockCode,
            StockPricePeriod period,
            int minuteInterval,
            LocalDate baseDate
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("stk_cd", stockCode);
        request.put("base_dt", baseDate.format(BASIC_DATE));
        request.put("upd_stkpc_tp", "1");
        if (period == StockPricePeriod.MINUTE) {
            request.put("tic_scope", Integer.toString(minuteInterval));
        }
        return request;
    }

    private StockChartPoint toPoint(BackdataRecord record, StockPricePeriod period) {
        try {
            OffsetDateTime timestamp = timestamp(record, period);
            BigDecimal open = positive(record.value("open"));
            BigDecimal high = positive(record.value("high"));
            BigDecimal low = positive(record.value("low"));
            BigDecimal close = positive(record.value("close"));
            BigDecimal volume = decimal(record.value("volume"));
            if (timestamp == null || open == null || high == null || low == null || close == null) {
                return null;
            }
            return new StockChartPoint(
                    timestamp,
                    open,
                    high,
                    low,
                    close,
                    volume == null ? 0L : volume.abs().longValueExact()
            );
        } catch (RuntimeException exception) {
            log.debug(
                    "차트 가격 행 제외. apiId={}, timestamp={}, causeType={}",
                    period.apiId(),
                    record.value("timestamp"),
                    exception.getClass().getSimpleName()
            );
            return null;
        }
    }

    private OffsetDateTime timestamp(BackdataRecord record, StockPricePeriod period) {
        if (period == StockPricePeriod.MINUTE) {
            String value = digits(record.value("timestamp"));
            if (value == null || value.length() < 12) return null;
            String normalized = value.length() >= 14 ? value.substring(0, 14) : value + "00";
            try {
                return LocalDateTime.parse(normalized, BASIC_DATE_TIME)
                        .atZone(SEOUL)
                        .toOffsetDateTime();
            } catch (DateTimeParseException exception) {
                return null;
            }
        }
        return record.businessDate() == null
                ? null
                : record.businessDate().atStartOfDay(SEOUL).toOffsetDateTime();
    }

    private String digits(String value) {
        if (value == null) return null;
        return value.replaceAll("\\D", "");
    }

    private BigDecimal positive(String value) {
        BigDecimal decimal = decimal(value);
        return decimal == null || decimal.signum() == 0 ? null : decimal.abs();
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value.trim().replace(",", ""));
    }
}
