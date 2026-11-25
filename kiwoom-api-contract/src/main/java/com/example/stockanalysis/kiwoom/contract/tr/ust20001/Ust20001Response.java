package com.example.stockanalysis.kiwoom.contract.tr.ust20001;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 매도 주문(ust20001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust20001Response implements KiwoomResult {

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("ord_no")
    private String ordNo; // 주문번호

    @JsonProperty("poss_qty")
    private String possQty; // 보유수량

    @JsonProperty("tdy_resel_usedq")
    private String tdyReselUsedq; // 금일재매도사용수량

    @JsonProperty("pred_resel_usedq")
    private String predReselUsedq; // 전일재매도사용수량

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
