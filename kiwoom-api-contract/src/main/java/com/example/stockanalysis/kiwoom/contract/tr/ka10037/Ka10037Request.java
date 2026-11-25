package com.example.stockanalysis.kiwoom.contract.tr.ka10037;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 외국계창구매매상위요청(ka10037) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10037Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("dt")
    private String dt; // 기간

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
