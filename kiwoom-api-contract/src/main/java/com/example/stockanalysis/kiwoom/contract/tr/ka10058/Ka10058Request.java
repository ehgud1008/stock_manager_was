package com.example.stockanalysis.kiwoom.contract.tr.ka10058;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 투자자별일별매매종목요청(ka10058) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10058Request {

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("invsr_tp")
    private String invsrTp; // 투자자구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
