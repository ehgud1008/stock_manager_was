package com.example.stockanalysis.kiwoom.contract.tr.kt00015;

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
 * 위탁종합거래내역요청(kt00015) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00015Response implements KiwoomResult {

    @JsonProperty("trst_ovrl_trde_prps_array")
    private List<TrstOvrlTrdePrpsArrayItem> trstOvrlTrdePrpsArray; // 위탁종합거래내역배열

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
    public static class TrstOvrlTrdePrpsArrayItem {

        @JsonProperty("trde_dt")
        private String trdeDt; // 거래일자

        @JsonProperty("trde_no")
        private String trdeNo; // 거래번호

        @JsonProperty("rmrk_nm")
        private String rmrkNm; // 적요명

        @JsonProperty("crd_deal_tp_nm")
        private String crdDealTpNm; // 신용거래구분명

        @JsonProperty("exct_amt")
        private String exctAmt; // 정산금액

        @JsonProperty("loan_amt_rpya")
        private String loanAmtRpya; // 대출금상환

        @JsonProperty("fc_trde_amt")
        private String fcTrdeAmt; // 거래금액(외)

        @JsonProperty("fc_exct_amt")
        private String fcExctAmt; // 정산금액(외)

        @JsonProperty("entra_remn")
        private String entraRemn; // 예수금잔고

        @JsonProperty("crnc_cd")
        private String crncCd; // 통화코드

        @JsonProperty("trde_ocr_tp")
        private String trdeOcrTp; // 거래종류구분

        @JsonProperty("trde_kind_nm")
        private String trdeKindNm; // 거래종류명

        @JsonProperty("stk_nm")
        private String stkNm; // 종목명

        @JsonProperty("trde_amt")
        private String trdeAmt; // 거래금액

        @JsonProperty("trde_agri_tax")
        private String trdeAgriTax; // 거래및농특세

        @JsonProperty("rpy_diffa")
        private String rpyDiffa; // 상환차금

        @JsonProperty("fc_trde_tax")
        private String fcTrdeTax; // 거래세(외)

        @JsonProperty("dly_sum")
        private String dlySum; // 연체합

        @JsonProperty("fc_entra")
        private String fcEntra; // 외화예수금잔고

        @JsonProperty("mdia_tp_nm")
        private String mdiaTpNm; // 매체구분명

        @JsonProperty("io_tp")
        private String ioTp; // 입출구분

        @JsonProperty("io_tp_nm")
        private String ioTpNm; // 입출구분명

        @JsonProperty("orig_deal_no")
        private String origDealNo; // 원거래번호

        @JsonProperty("stk_cd")
        private String stkCd; // 종목코드

        @JsonProperty("trde_qty_jwa_cnt")
        private String trdeQtyJwaCnt; // 거래수량/좌수

        @JsonProperty("cmsn")
        private String cmsn; // 수수료

        @JsonProperty("int_ls_usfe")
        private String intLsUsfe; // 이자/대주이용

        @JsonProperty("fc_cmsn")
        private String fcCmsn; // 수수료(외)

        @JsonProperty("fc_dly_sum")
        private String fcDlySum; // 연체합(외)

        @JsonProperty("vlbl_nowrm")
        private String vlblNowrm; // 유가금잔

        @JsonProperty("proc_tm")
        private String procTm; // 처리시간

        @JsonProperty("isin_cd")
        private String isinCd; // ISIN코드

        @JsonProperty("stex_cd")
        private String stexCd; // 거래소코드

        @JsonProperty("stex_nm")
        private String stexNm; // 거래소명

        @JsonProperty("trde_unit")
        private String trdeUnit; // 거래단가/환율

        @JsonProperty("incm_resi_tax")
        private String incmResiTax; // 소득/주민세

        @JsonProperty("loan_dt")
        private String loanDt; // 대출일

        @JsonProperty("uncl_ocr")
        private String unclOcr; // 미수(원/주)

        @JsonProperty("rpym_sum")
        private String rpymSum; // 변제합

        @JsonProperty("cntr_dt")
        private String cntrDt; // 체결일

        @JsonProperty("rcpy_no")
        private String rcpyNo; // 출납번호

        @JsonProperty("prcsr")
        private String prcsr; // 처리자

        @JsonProperty("proc_brch")
        private String procBrch; // 처리점

        @JsonProperty("trde_stle")
        private String trdeStle; // 매매형태

        @JsonProperty("txon_base_pric")
        private String txonBasePric; // 과세기준가

        @JsonProperty("tax_sum_cmsn")
        private String taxSumCmsn; // 세금수수료합

        @JsonProperty("frgn_pay_txam")
        private String frgnPayTxam; // 외국납부세액(외)

        @JsonProperty("fc_uncl_ocr")
        private String fcUnclOcr; // 미수(외)

        @JsonProperty("rpym_sum_fr")
        private String rpymSumFr; // 변제합(외)

        @JsonProperty("rcpmnyer")
        private String rcpmnyer; // 입금자

        @JsonProperty("trde_prtc_tp")
        private String trdePrtcTp; // 거래내역구분
    }
}
