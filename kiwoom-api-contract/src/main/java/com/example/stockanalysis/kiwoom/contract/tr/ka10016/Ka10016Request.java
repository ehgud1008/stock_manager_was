package com.example.stockanalysis.kiwoom.contract.tr.ka10016;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신고저가요청(ka10016) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10016Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("ntl_tp")
    private String ntlTp; // 신고저구분

    @JsonProperty("high_low_close_tp")
    private String highLowCloseTp; // 고저종구분

    @JsonProperty("stk_cnd")
    private String stkCnd; // 종목조건

    @JsonProperty("trde_qty_tp")
    private String trdeQtyTp; // 거래량구분

    @JsonProperty("crd_cnd")
    private String crdCnd; // 신용조건

    @JsonProperty("updown_incls")
    private String updownIncls; // 상하한포함

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
