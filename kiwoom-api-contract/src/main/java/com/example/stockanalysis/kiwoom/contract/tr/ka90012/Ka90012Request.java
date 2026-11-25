package com.example.stockanalysis.kiwoom.contract.tr.ka90012;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대차거래내역요청(ka90012) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90012Request {

    @JsonProperty("dt")
    private String dt; // 일자

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

}
