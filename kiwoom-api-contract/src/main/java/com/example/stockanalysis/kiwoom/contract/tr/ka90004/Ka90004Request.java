package com.example.stockanalysis.kiwoom.contract.tr.ka90004;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 종목별프로그램매매현황요청(ka90004) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90004Request {

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
