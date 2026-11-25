package com.example.stockanalysis.kiwoom.contract.tr.ka10052;

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
 * 거래원순간거래량요청(ka10052) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10052Response implements KiwoomResult {

    @JsonProperty("trde_ori_mont_trde_qty")
    private List<TrdeOriMontTrdeQtyItem> trdeOriMontTrdeQty; // 거래원순간거래량

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
    public static class TrdeOriMontTrdeQtyItem {

        @JsonProperty("tm")
        private String tm; // 시간

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("trde_ori_nm")
        private String trdeOriNm; // 거래원명

        @JsonProperty("tp")
        private String tp; // 구분

        @JsonProperty("mont_trde_qty")
        private String montTrdeQty; // 순간거래량

        @JsonProperty("acc_netprps")
        private String accNetprps; // 누적순매수

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율
    }
}
