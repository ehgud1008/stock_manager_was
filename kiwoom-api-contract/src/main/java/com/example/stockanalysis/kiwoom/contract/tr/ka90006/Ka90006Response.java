package com.example.stockanalysis.kiwoom.contract.tr.ka90006;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로그램매매차익잔고추이요청(ka90006) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90006Response implements KiwoomResult {

    @JsonProperty("prm_trde_dfrt_remn_trnsn")
    private List<PrmTrdeDfrtRemnTrnsnItem> prmTrdeDfrtRemnTrnsn; // 프로그램매매차익잔고추이

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지


    @Getter
    @Setter
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrmTrdeDfrtRemnTrnsnItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("buy_dfrt_trde_qty")
        private String buyDfrtTrdeQty; // 매수차익거래수량

        @JsonProperty("buy_dfrt_trde_amt")
        private String buyDfrtTrdeAmt; // 매수차익거래금액

        @JsonProperty("buy_dfrt_trde_irds_amt")
        private String buyDfrtTrdeIrdsAmt; // 매수차익거래증감액

        @JsonProperty("sel_dfrt_trde_qty")
        private String selDfrtTrdeQty; // 매도차익거래수량

        @JsonProperty("sel_dfrt_trde_amt")
        private String selDfrtTrdeAmt; // 매도차익거래금액

        @JsonProperty("sel_dfrt_trde_irds_amt")
        private String selDfrtTrdeIrdsAmt; // 매도차익거래증감액
    }
}
