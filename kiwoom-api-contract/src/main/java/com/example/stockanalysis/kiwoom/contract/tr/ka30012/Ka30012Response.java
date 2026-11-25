package com.example.stockanalysis.kiwoom.contract.tr.ka30012;

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
 * ELW종목상세정보요청(ka30012) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30012Response implements KiwoomResult {

    @JsonProperty("aset_cd")
    private String asetCd; // 자산코드

    @JsonProperty("cur_prc")
    private String curPrc; // 현재가

    @JsonProperty("pred_pre_sig")
    private String predPreSig; // 전일대비기호

    @JsonProperty("pred_pre")
    private String predPre; // 전일대비

    @JsonProperty("flu_rt")
    private String fluRt; // 등락율

    @JsonProperty("lpmmcm_nm")
    private String lpmmcmNm; // LP회원사명

    @JsonProperty("lpmmcm_nm_1")
    private String lpmmcmNm1; // LP회원사명1

    @JsonProperty("lpmmcm_nm_2")
    private String lpmmcmNm2; // LP회원사명2

    @JsonProperty("elwrght_cntn")
    private String elwrghtCntn; // ELW권리내용

    @JsonProperty("elwexpr_evlt_pric")
    private String elwexprEvltPric; // ELW만기평가가격

    @JsonProperty("elwtheory_pric")
    private String elwtheoryPric; // ELW이론가

    @JsonProperty("dispty_rt")
    private String disptyRt; // 괴리율

    @JsonProperty("elwinnr_vltl")
    private String elwinnrVltl; // ELW내재변동성

    @JsonProperty("exp_rght_pric")
    private String expRghtPric; // 예상권리가

    @JsonProperty("elwpl_qutr_rt")
    private String elwplQutrRt; // ELW손익분기율

    @JsonProperty("elwexec_pric")
    private String elwexecPric; // ELW행사가

    @JsonProperty("elwcnvt_rt")
    private String elwcnvtRt; // ELW전환비율

    @JsonProperty("elwcmpn_rt")
    private String elwcmpnRt; // ELW보상율

    @JsonProperty("elwpric_rising_part_rt")
    private String elwpricRisingPartRt; // ELW가격상승참여율

    @JsonProperty("elwrght_type")
    private String elwrghtType; // ELW권리유형

    @JsonProperty("elwsrvive_dys")
    private String elwsrviveDys; // ELW잔존일수

    @JsonProperty("stkcnt")
    private String stkcnt; // 상장주식수

    @JsonProperty("elwlpord_pos")
    private String elwlpordPos; // ELWLP주문가능

    @JsonProperty("lpposs_rt")
    private String lppossRt; // LP보유비율

    @JsonProperty("lprmnd_qty")
    private String lprmndQty; // LP보유수량

    @JsonProperty("elwspread")
    private String elwspread; // ELW스프레드

    @JsonProperty("elwprty")
    private String elwprty; // ELW패리티

    @JsonProperty("elwgear")
    private String elwgear; // ELW기어링

    @JsonProperty("elwflo_dt")
    private String elwfloDt; // ELW상장일

    @JsonProperty("elwfin_trde_dt")
    private String elwfinTrdeDt; // ELW최종거래일

    @JsonProperty("expr_dt")
    private String exprDt; // 만기일

    @JsonProperty("exec_dt")
    private String execDt; // 행사일

    @JsonProperty("lpsuply_end_dt")
    private String lpsuplyEndDt; // LP공급종료일

    @JsonProperty("elwpay_dt")
    private String elwpayDt; // ELW지급일

    @JsonProperty("elwinvt_ix_comput")
    private String elwinvtIxComput; // ELW투자지표산출

    @JsonProperty("elwpay_agnt")
    private String elwpayAgnt; // ELW지급대리인

    @JsonProperty("elwappr_way")
    private String elwapprWay; // ELW결재방법

    @JsonProperty("elwrght_exec_way")
    private String elwrghtExecWay; // ELW권리행사방식

    @JsonProperty("elwpblicte_orgn")
    private String elwpblicteOrgn; // ELW발행기관

    @JsonProperty("dcsn_pay_amt")
    private String dcsnPayAmt; // 확정지급액

    @JsonProperty("kobarr")
    private String kobarr; // KO베리어

    @JsonProperty("iv")
    private String iv; // IV

    @JsonProperty("clsprd_end_elwocr")
    private String clsprdEndElwocr; // 종기종료ELW발생

    @JsonProperty("bsis_aset_1")
    private String bsisAset1; // 기초자산1

    @JsonProperty("bsis_aset_comp_rt_1")
    private String bsisAsetCompRt1; // 기초자산구성비율1

    @JsonProperty("bsis_aset_2")
    private String bsisAset2; // 기초자산2

    @JsonProperty("bsis_aset_comp_rt_2")
    private String bsisAsetCompRt2; // 기초자산구성비율2

    @JsonProperty("bsis_aset_3")
    private String bsisAset3; // 기초자산3

    @JsonProperty("bsis_aset_comp_rt_3")
    private String bsisAsetCompRt3; // 기초자산구성비율3

    @JsonProperty("bsis_aset_4")
    private String bsisAset4; // 기초자산4

    @JsonProperty("bsis_aset_comp_rt_4")
    private String bsisAsetCompRt4; // 기초자산구성비율4

    @JsonProperty("bsis_aset_5")
    private String bsisAset5; // 기초자산5

    @JsonProperty("bsis_aset_comp_rt_5")
    private String bsisAsetCompRt5; // 기초자산구성비율5

    @JsonProperty("fr_dt")
    private String frDt; // 평가시작일자

    @JsonProperty("to_dt")
    private String toDt; // 평가종료일자

    @JsonProperty("fr_tm")
    private String frTm; // 평가시작시간

    @JsonProperty("evlt_end_tm")
    private String evltEndTm; // 평가종료시간

    @JsonProperty("evlt_pric")
    private String evltPric; // 평가가격

    @JsonProperty("evlt_fnsh_yn")
    private String evltFnshYn; // 평가완료여부

    @JsonProperty("all_hgst_pric")
    private String allHgstPric; // 전체최고가

    @JsonProperty("all_lwst_pric")
    private String allLwstPric; // 전체최저가

    @JsonProperty("imaf_hgst_pric")
    private String imafHgstPric; // 직후최고가

    @JsonProperty("imaf_lwst_pric")
    private String imafLwstPric; // 직후최저가

    @JsonProperty("sndhalf_mrkt_hgst_pric")
    private String sndhalfMrktHgstPric; // 후반장최고가

    @JsonProperty("sndhalf_mrkt_lwst_pric")
    private String sndhalfMrktLwstPric; // 후반장최저가

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지

}
