package com.example.stockanalysis.kiwoom.contract.tr.ka10085;

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
 * 계좌수익률요청(ka10085) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10085Response implements KiwoomResult {

    @JsonProperty("acnt_prft_rt")
    private List<AcntPrftRtItem> acntPrftRt; // 계좌수익률

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
    public static class AcntPrftRtItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pur_pric")
        private String purPric; // 매입가

        @JsonProperty("pur_amt")
        private String purAmt; // 매입금액

        @JsonProperty("rmnd_qty")
        private String rmndQty; // 보유수량

        @JsonProperty("tdy_sel_pl")
        private String tdySelPl; // 당일매도손익

        @JsonProperty("tdy_trde_cmsn")
        private String tdyTrdeCmsn; // 당일매매수수료

        @JsonProperty("tdy_trde_tax")
        private String tdyTrdeTax; // 당일매매세금

        @JsonProperty("crd_tp")
        private String crdTp; // 신용구분

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("setl_remn")
        private String setlRemn; // 결제잔고

        @JsonProperty("clrn_alow_qty")
        private String clrnAlowQty; // 청산가능수량

        @JsonProperty("crd_amt")
        private String crdAmt; // 신용금액

        @JsonProperty("crd_int")
        private String crdInt; // 신용이자

        @JsonProperty("expr_dt")
        private String exprDt; // 만기일
    }
}
