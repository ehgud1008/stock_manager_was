package com.example.stockanalysis.decision;

import static com.example.stockanalysis.decision.AccountRiskModels.*;
import com.example.stockanalysis.shared.exception.*;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.dao.*;

/** Single-owner manual account totals. Reading never changes settings or reserves cash. */
@Service
public class AccountRiskService {
    private final JdbcTemplate jdbc;private final DecisionSettings settings;private final Clock clock;
    public AccountRiskService(JdbcTemplate jdbc,DecisionSettings settings,Clock clock){this.jdbc=jdbc;this.settings=settings;this.clock=clock;}
    private Settings row(java.sql.ResultSet r,int n)throws java.sql.SQLException {
        return new Settings(r.getBigDecimal("total_assets"),r.getBigDecimal("available_cash"),r.getBigDecimal("max_loss_per_trade"),
                r.getBigDecimal("max_position_percent"),r.getDate("balance_date").toLocalDate(),r.getLong("revision"),Instant.ofEpochMilli(r.getLong("updated_epoch")));
    }
    public View get() {
        try {
            var rows=jdbc.query("SELECT * FROM td_account_risk WHERE owner_scope=?",this::row,settings.ownerScope());
            return rows.isEmpty()?new View("NOT_CONFIGURED",null,clock.instant(),"계좌 위험 설정을 등록하세요."):
                    new View("AVAILABLE",rows.get(0),clock.instant(),"수정한 설정은 다음 통합 판단부터 적용됩니다.");
        } catch(BadSqlGrammarException e){return new View("UNAVAILABLE",null,clock.instant(),"계좌 위험 설정 테이블이 필요합니다. 20261117_01_account_risk.sql을 적용하세요.");}
        catch(DataAccessException e){return new View("UNAVAILABLE",null,clock.instant(),"계좌 위험 설정을 조회하지 못했습니다.");}
    }
    public Settings save(Edit request) {
        validate(request);Instant now=clock.instant();long next=request.expectedRevision()+1;
        try {
            if(request.expectedRevision()==0)jdbc.update("INSERT INTO td_account_risk(owner_scope,total_assets,available_cash,max_loss_per_trade,max_position_percent,balance_date,revision,updated_epoch) VALUES(?,?,?,?,?,?,1,?)",
                    settings.ownerScope(),request.totalAssets(),request.availableCash(),request.maxLossPerTrade(),request.maxPositionPercent(),java.sql.Date.valueOf(request.balanceDate()),now.toEpochMilli());
            else if(jdbc.update("UPDATE td_account_risk SET total_assets=?,available_cash=?,max_loss_per_trade=?,max_position_percent=?,balance_date=?,revision=revision+1,updated_epoch=? WHERE owner_scope=? AND revision=?",
                    request.totalAssets(),request.availableCash(),request.maxLossPerTrade(),request.maxPositionPercent(),java.sql.Date.valueOf(request.balanceDate()),now.toEpochMilli(),settings.ownerScope(),request.expectedRevision())!=1)throw conflict();
        }catch(DuplicateKeyException e){throw conflict();}
        catch(BadSqlGrammarException e){throw new BusinessException(ErrorCode.DECISION_CONFLICT,"20261117_01_account_risk.sql을 적용한 후 다시 저장하세요.");}
        return new Settings(request.totalAssets(),request.availableCash(),request.maxLossPerTrade(),request.maxPositionPercent(),request.balanceDate(),next,now);
    }
    private void validate(Edit r) {
        var today=LocalDate.ofInstant(clock.instant(),DecisionService.SEOUL);
        if(r==null || !money(r.totalAssets()) || !money(r.availableCash()) || !money(r.maxLossPerTrade())
                || r.maxPositionPercent()==null || r.maxPositionPercent().signum()<0 || r.maxPositionPercent().compareTo(new BigDecimal("100"))>0
                || r.maxPositionPercent().stripTrailingZeros().scale()>2 || r.balanceDate()==null || r.balanceDate().isAfter(today)
                || r.expectedRevision()<0 || r.expectedRevision()==Long.MAX_VALUE || r.availableCash().compareTo(r.totalAssets())>0
                || r.maxLossPerTrade().compareTo(r.totalAssets())>0)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"총자산·현금·허용 손실·최대 비중·기준일을 확인하세요. 현금과 허용 손실은 총자산 이하여야 합니다.");
    }
    private boolean money(BigDecimal value){return value!=null && value.signum()>=0
            && value.compareTo(new BigDecimal("999999999999.99"))<=0 && value.stripTrailingZeros().scale()<=2;}
    private BusinessException conflict(){return new BusinessException(ErrorCode.DECISION_CONFLICT,"계좌 위험 설정이 변경되었습니다. 다시 조회한 뒤 저장하세요.");}
}
