package com.example.stockanalysis.kiwoom.contract.tr.ka10075;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미체결요청(ka10075) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10075Request {

    @JsonProperty("all_stk_tp")
    private String allStkTp; // 전체종목구분

    @JsonProperty("trde_tp")
    private String trdeTp; // 매매구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
