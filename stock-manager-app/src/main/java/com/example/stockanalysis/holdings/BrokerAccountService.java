package com.example.stockanalysis.holdings;

import com.example.stockanalysis.decision.AccountRiskService;
import com.example.stockanalysis.kiwoom.client.account.KiwoomAccountReadClient;
import com.example.stockanalysis.kiwoom.client.account.KiwoomAccountReadClient.Query;
import com.example.stockanalysis.kiwoom.config.KiwoomProperties;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class BrokerAccountService {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    public record Cash(String environment, Instant checkedAt, BigDecimal deposit, BigDecimal cashOnlyOrderAmount,
                       BigDecimal brokerOrderAmount, BigDecimal manualAvailableCash, LocalDate manualDate) {}
    public record Position(String stockCode, String name, String creditType, BigDecimal quantity,
                           BigDecimal sellable, BigDecimal averagePrice, BigDecimal valuationPnl) {}
    public record ManualPosition(String stockCode, BigDecimal quantity, LocalDate balanceDate) {}
    public record Positions(String environment, Instant checkedAt, List<Position> items, String next,
                            String manualStatus, List<ManualPosition> manualPositions, BigDecimal valuationPnl) {}
    public record Fill(String order, String execution, String stockCode, String name, String side, String market,
                       String time, BigDecimal quantity, BigDecimal price) {}
    public record Fills(String environment, Instant checkedAt, LocalDate date, List<Fill> items, String next) {}
    public record DailyPnl(String date, BigDecimal realized, BigDecimal fees, BigDecimal taxes) {}
    public record Pnl(String environment, Instant checkedAt, LocalDate from, LocalDate to,
                      BigDecimal reportedRealized, List<DailyPnl> items, String next) {}

    private final KiwoomAccountReadClient broker;
    private final KiwoomProperties properties;
    private final AccountRiskService accounts;
    private final HoldingService holdings;
    private final Clock clock;

    public BrokerAccountService(KiwoomAccountReadClient broker, KiwoomProperties properties, AccountRiskService accounts,
                                HoldingService holdings, Clock clock) {
        this.broker=broker;
        this.properties=properties;
        this.accounts=accounts;
        this.holdings=holdings;
        this.clock=clock;
    }

    public Cash cash() {
        var data=read(Query.CASH,Map.of("qry_tp","3"),null).body();
        var manual=accounts.get().settings();
        return new Cash(environment(),clock.instant(),number(data,"entr"),number(data,"100stk_ord_alow_amt"),
                number(data,"ord_alow_amt"),manual==null?null:manual.availableCash(),manual==null?null:manual.balanceDate());
    }

    public Positions positions(String cursor) {
        var result=read(Query.HOLDINGS,Map.of("qry_tp","1","dmst_stex_tp","KRX"),cursor);
        var items=new ArrayList<Position>();
        for (var row : rows(result.body(),"acnt_evlt_remn_indv_tot")) {
            items.add(new Position(code(row),value(row,"stk_nm"),value(row,"crd_tp"),number(row,"rmnd_qty"),
                    number(row,"trde_able_qty"),number(row,"pur_pric"),number(row,"evltv_prft")));
        }
        List<ManualPosition> manual=List.of();
        String status="NOT_REQUESTED";
        if (cursor==null) {
            try {
                manual=holdings.list().stream().map(h->new ManualPosition(h.stockCode(),h.quantity(),h.balanceDate())).toList();
                status="AVAILABLE";
            } catch (DataAccessException e) { status="UNAVAILABLE"; }
        }
        return new Positions(environment(),clock.instant(),items,result.next(),status,manual,number(result.body(),"tot_evlt_pl"));
    }

    public Fills fills(LocalDate date, String cursor) {
        validateDates(date,date);
        var result=read(Query.FILLS, Map.of("ord_dt",compact(date),"stk_bond_tp","1","mrkt_tp","0","sell_tp","0",
                "qry_tp","1","dmst_stex_tp","%"),cursor);
        var items=new ArrayList<Fill>();
        for (var row : rows(result.body(),"acnt_ord_cntr_prst_array")) {
            var id=value(row,"cntr_no");
            var quantity=number(row,"cntr_qty");
            if (id==null || id.matches("0+") || quantity==null || quantity.signum()<=0) continue;
            items.add(new Fill(value(row,"ord_no"),id,code(row),value(row,"stk_nm"),value(row,"io_tp_nm"),
                    value(row,"dmst_stex_tp"),value(row,"cntr_tm"),quantity,number(row,"cntr_uv")));
        }
        return new Fills(environment(),clock.instant(),date,items,result.next());
    }

    public Pnl pnl(LocalDate from, LocalDate to, String cursor) {
        validateDates(from,to);
        var result=read(Query.REALIZED,Map.of("strt_dt",compact(from),"end_dt",compact(to)),cursor);
        var items=new ArrayList<DailyPnl>();
        for (var row : rows(result.body(),"dt_rlzt_pl")) {
            items.add(new DailyPnl(value(row,"dt"),number(row,"tdy_sel_pl"),number(row,"tdy_trde_cmsn"),number(row,"tdy_trde_tax")));
        }
        // The broker's period total is used once, rather than summing repeated page summaries.
        return new Pnl(environment(),clock.instant(),from,to,cursor==null?number(result.body(),"rlzt_pl"):null,items,result.next());
    }

    private KiwoomAccountReadClient.Page read(Query query, Map<String,String> body, String cursor) {
        if (cursor!=null && (cursor.isBlank() || cursor.length()>50 || cursor.chars().anyMatch(Character::isISOControl))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"연속 조회 값이 올바르지 않습니다.");
        }
        try {
            return broker.read(query,body,cursor);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,"키움 계좌 조회에 실패했습니다. 연결 환경과 조회 권한을 확인하세요.");
        }
    }

    private String environment() { return properties.isUseMock()?"BROKER_MOCK":"BROKER_LIVE"; }
    private void validateDates(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || to.isAfter(LocalDate.ofInstant(clock.instant(),SEOUL)) || from.getYear()<1900
                || ChronoUnit.DAYS.between(from,to)>30) throw new BusinessException(ErrorCode.INVALID_REQUEST,"오늘까지 최대 31일을 조회할 수 있습니다.");
    }
    private static String compact(LocalDate date) { return date.format(DateTimeFormatter.BASIC_ISO_DATE); }
    private static String value(JsonNode row,String field) {
        String value=row.path(field).asText("").strip();
        return value.isEmpty()?null:value;
    }
    private static String code(JsonNode row) {
        String code=value(row,"stk_cd");
        return code==null?null:code.replaceFirst("^[AJQ](?=[0-9]{6}$)","");
    }
    private static BigDecimal number(JsonNode row,String field) {
        String value=value(row,field);
        if (value==null || !value.matches("[+-]?[0-9]{1,20}(\\.[0-9]{1,8})?")) return null;
        return new BigDecimal(value);
    }
    private static JsonNode rows(JsonNode body,String field) {
        var rows=body.path(field);
        if (!rows.isArray()) throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,"키움이 내역 목록을 반환하지 않아 결과를 확인할 수 없습니다.");
        return rows;
    }
}
