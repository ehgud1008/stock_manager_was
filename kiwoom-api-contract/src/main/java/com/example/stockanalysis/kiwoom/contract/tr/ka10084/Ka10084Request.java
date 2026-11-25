package com.example.stockanalysis.kiwoom.contract.tr.ka10084;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당일전일체결요청(ka10084) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10084Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("tdy_pred")
    private String tdyPred; // 당일전일

    @JsonProperty("tic_min")
    private String ticMin; // 틱분

    @JsonProperty("tm")
    private String tm; // 시간

}
