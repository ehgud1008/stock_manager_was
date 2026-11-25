package com.example.stockanalysis.kiwoom.contract.tr.ka90001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 테마그룹별요청(ka90001) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90001Request {

    @JsonProperty("qry_tp")
    private String qryTp; // 검색구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("date_tp")
    private String dateTp; // 날짜구분

    @JsonProperty("thema_nm")
    private String themaNm; // 테마명

    @JsonProperty("flu_pl_amt_tp")
    private String fluPlAmtTp; // 등락수익구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
