package com.example.stockanalysis.kiwoom.contract.tr.ust21150;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 일별 주문체결내역(ust21150) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21150Request {

    @JsonProperty("ord_dt")
    private String ordDt; // 주문일자

    @JsonProperty("query_tp")
    private String queryTp; // 조회구분

    @JsonProperty("slby_tp")
    private String slbyTp; // 매도수구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("oppo_trde_tp")
    private String oppoTrdeTp; // 반대매매구분

    @JsonProperty("fr_ord_no")
    private String frOrdNo; // 시작주문번호

}
