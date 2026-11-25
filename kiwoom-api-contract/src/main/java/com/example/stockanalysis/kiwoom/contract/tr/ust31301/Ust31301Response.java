package com.example.stockanalysis.kiwoom.contract.tr.ust31301;

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
 * 환율 조회(ust31301) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust31301Response implements KiwoomResult {

    @JsonProperty("sell_aplc_exrt")
    private String sellAplcExrt; // 매도적용환율

    @JsonProperty("buy_aplc_exrt")
    private String buyAplcExrt; // 매수적용환율

    @JsonProperty("aplc_exrt")
    private String aplcExrt; // 적용환율

    @JsonProperty("exrt_tp_nm")
    private String exrtTpNm; // 환율구분명

    @JsonProperty("spcl_bf_exrt")
    private String spclBfExrt; // 우대율 적용 전 환율

    @JsonProperty("exrt_spcl_rt")
    private String exrtSpclRt; // 환율우대율

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
