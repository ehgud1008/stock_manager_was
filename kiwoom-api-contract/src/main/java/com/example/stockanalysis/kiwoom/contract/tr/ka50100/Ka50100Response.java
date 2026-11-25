package com.example.stockanalysis.kiwoom.contract.tr.ka50100;

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
 * 금현물 시세정보(ka50100) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka50100Response implements KiwoomResult {

    @JsonProperty("pred_pre_sig")
    private String predPreSig; // 전일대비기호

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락율

    @JsonProperty("trde_qty")
    private String trdeQty; // 거래량

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("pred_rt")
    private String predRt; // 전일비

    @JsonProperty("upl_pric")
    private String uplPric; // 상한가

    @JsonProperty("lst_pric")
    private String lstPric; // 하한가

    @JsonProperty("pred_close_pric")
    private String predClosePric; // 전일종가

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
