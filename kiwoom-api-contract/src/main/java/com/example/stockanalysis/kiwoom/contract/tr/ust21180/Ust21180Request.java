package com.example.stockanalysis.kiwoom.contract.tr.ust21180;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 기간별 주문내역(ust21180) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21180Request {

    @JsonProperty("strt_dt")
    private String strtDt; // 시작주문일자

    @JsonProperty("end_dt")
    private String endDt; // 종료주문일자

    @JsonProperty("slby_tp")
    private String slbyTp; // 매도수구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("oppo_trde_tp")
    private String oppoTrdeTp; // 반대매매구분

}
