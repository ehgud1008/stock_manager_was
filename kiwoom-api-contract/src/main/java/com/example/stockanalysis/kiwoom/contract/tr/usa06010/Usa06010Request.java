package com.example.stockanalysis.kiwoom.contract.tr.usa06010;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 틱 차트(usa06010) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa06010Request {

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tic_scope")
    private String ticScope; // 틱범위

    @JsonProperty("upd_stkpc_tp")
    private String updStkpcTp; // 수정주가구분

    @JsonProperty("exrt_appl_tp")
    private String exrtApplTp; // 환율적용구분

}
