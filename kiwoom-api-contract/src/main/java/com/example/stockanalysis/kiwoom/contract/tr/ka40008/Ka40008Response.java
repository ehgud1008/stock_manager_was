package com.example.stockanalysis.kiwoom.contract.tr.ka40008;

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
 * ETF일자별체결요청(ka40008) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40008Response implements KiwoomResult {

    @JsonProperty("cntr_tm")
    private String cntrTm; // 체결시간

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("pre_sig")
    private String preSig; // 대비기호

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("trde_qty")
    private String trdeQty; // 거래량

    @JsonProperty("etfnetprps_qty_array")
    private List<EtfnetprpsQtyArrayItem> etfnetprpsQtyArray; // ETF순매수수량배열

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
    public static class EtfnetprpsQtyArrayItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("cur_prc_n")
        private String curPrcN; // 현재가n

        @JsonProperty("pre_sig_n")
        private String preSigN; // 대비기호n

        @JsonProperty("pred_pre_n")
        private String predPreN; // 전일대비n

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty; // 누적거래량

        @JsonProperty("for_netprps_qty")
        private String forNetprpsQty; // 외인순매수수량

        @JsonProperty("orgn_netprps_qty")
        private String orgnNetprpsQty; // 기관순매수수량
    }
}
