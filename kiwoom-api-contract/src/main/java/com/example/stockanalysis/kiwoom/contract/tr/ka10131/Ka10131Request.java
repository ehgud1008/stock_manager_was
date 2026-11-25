package com.example.stockanalysis.kiwoom.contract.tr.ka10131;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기관외국인연속매매현황요청(ka10131) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10131Request {

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 장구분

    @JsonProperty("netslmt_tp")
    private String netslmtTp; // 순매도수구분

    @JsonProperty("stk_inds_tp")
    private String stkIndsTp; // 종목업종구분

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp; // 금액수량구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
