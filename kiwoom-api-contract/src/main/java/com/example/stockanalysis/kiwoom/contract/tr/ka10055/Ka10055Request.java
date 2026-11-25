package com.example.stockanalysis.kiwoom.contract.tr.ka10055;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당일전일체결량요청(ka10055) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10055Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tdy_pred")
    private String tdyPred; // 당일전일

}
