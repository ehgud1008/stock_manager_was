package com.example.stockanalysis.kiwoom.contract.tr.ka10074;

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
 * 일자별실현손익요청(ka10074) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10074Response implements KiwoomResult {

    @JsonProperty("tot_buy_amt")
    private String totBuyAmt; // 총매수금액

    @JsonProperty("tot_sell_amt")
    private String totSellAmt; // 총매도금액

    @JsonProperty("rlzt_pl")
    private String rlztPl; // 실현손익

    @JsonProperty("trde_cmsn")
    private String trdeCmsn; // 매매수수료

    @JsonProperty("trde_tax")
    private String trdeTax; // 매매세금

    @JsonProperty("dt_rlzt_pl")
    private List<DtRlztPlItem> dtRlztPl; // 일자별실현손익

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
    public static class DtRlztPlItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("buy_amt")
        private String buyAmt; // 매수금액

        @JsonProperty("sell_amt")
        private String sellAmt; // 매도금액

        @JsonProperty("tdy_sel_pl")
        private String tdySelPl; // 당일매도손익

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금
    }
}
