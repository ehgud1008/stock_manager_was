package com.example.stockanalysis.kiwoom.contract.tr.ka10006;

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
 * 주식시분요청(ka10006) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10006Response implements KiwoomResult {

    @JsonProperty("date")
    private String date; // 날짜

    @JsonProperty("open_pric")
    private String openPric; // 시가

    @JsonProperty("high_pric")
    private String highPric; // 고가

    @JsonProperty("low_pric")
    private String lowPric; // 저가

    @JsonProperty("close_pric")
    private String closePric; // 종가

    @JsonProperty("pre")
    private String pre; // 대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락률

    @JsonProperty("trde_qty")
    private String trdeQty; // 거래량

    @JsonProperty("trde_prica")
    private String trdePrica; // 거래대금

    @JsonProperty("cntr_str")
    private String cntrStr; // 체결강도

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
