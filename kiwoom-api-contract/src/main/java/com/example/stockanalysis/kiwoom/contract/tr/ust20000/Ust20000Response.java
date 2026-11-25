package com.example.stockanalysis.kiwoom.contract.tr.ust20000;

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
 * 미국주식 매수 주문(ust20000) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust20000Response implements KiwoomResult {

    @JsonProperty("stk_nm")
    private String stkNm; // 종목명

    @JsonProperty("ord_no")
    private String ordNo; // 주문번호

    @JsonProperty("fc_entra")
    private String fcEntra; // 외화예수금

    @JsonProperty("tdy_rebuy_useda")
    private String tdyRebuyUseda; // 금일재매수사용금액

    @JsonProperty("pred_rebuy_useda")
    private String predRebuyUseda; // 전일재매수사용금액

    @JsonProperty("trst_prof_ch")
    private String trstProfCh; // 사용증거금

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
