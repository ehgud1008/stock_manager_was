package com.example.stockanalysis.kiwoom.contract.tr.ka10088;

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
 * 미체결 분할주문 상세(ka10088) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10088Response implements KiwoomResult {

    @JsonProperty("osop")
    private List<OsopItem> osop; // 미체결분할주문리스트

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
    public static class OsopItem {

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("ord_no")
        private String ordNo; // 주문번호

        @JsonProperty("ord_qty")
        private String ordQty; // 주문수량

        @JsonProperty("ord_pric")
        private String ordPric; // 주문가격

        @JsonProperty("osop_qty")
        private String osopQty; // 미체결수량

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 주문구분

        @JsonProperty("trde_tp")
        private String trdeTp; // 매매구분

        @JsonProperty("sell_tp")
        private String sellTp; // 매도/수 구분

        @JsonProperty("cntr_qty")
        private String cntrQty; // 체결량

        @JsonProperty("ord_stt")
        private String ordStt; // 주문상태

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("stex_tp_txt")
        private String stexTpTxt; // 거래소구분텍스트
    }
}
