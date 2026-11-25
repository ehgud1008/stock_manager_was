package com.example.stockanalysis.kiwoom.contract.tr.ka10076;

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
 * 체결요청(ka10076) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10076Response implements KiwoomResult {

    @JsonProperty("cntr")
    private List<CntrItem> cntr; // 체결

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
    public static class CntrItem {

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 주문구분

        @JsonProperty("ord_pric")
        private String ordPric; // 주문가격

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("cntr_pric")
        private String cntrPric; // 체결가

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결량

        @JsonProperty("oso_qty")
        private String osoQty; // 미체결수량

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금

        @JsonProperty("ord_stt")
        private String ordStt; // 주문상태

        @JsonProperty("trde_tp")
        private String trdeTp; // 매매구분

        @JsonProperty("orig_ord_no")
        private String origOrdNo; // 원주문번호

        @JsonProperty("ord_tm")
        private String ordTm; // 주문시간

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("stex_tp_txt")
        private String stexTpTxt; // 거래소구분텍스트

        @JsonProperty("sor_yn")
        private String sorYn; // SOR 여부값

        @JsonProperty("stop_pric")
        private String stopPric; // 스톱가
    }
}
