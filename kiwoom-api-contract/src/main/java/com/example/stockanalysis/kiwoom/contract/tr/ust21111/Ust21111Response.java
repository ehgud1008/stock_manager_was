package com.example.stockanalysis.kiwoom.contract.tr.ust21111;

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
 * 원화출금가능 금액 조회(원화대용 포함)(ust21111) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21111Response implements KiwoomResult {

    @JsonProperty("rgst_abnd_tp")
    private String rgstAbndTp; // 원화대용등록해지구분

    @JsonProperty("krw_pymn_alow_amt")
    private String krwPymnAlowAmt; // 원화출금가능금액

    @JsonProperty("krw_repl_abnd_amt")
    private String krwReplAbndAmt; // 원화대용해지가능금액

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
