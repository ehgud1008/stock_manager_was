package com.example.stockanalysis.kiwoom.contract.tr.ka10051;

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
 * 업종별투자자순매수요청(ka10051) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10051Response implements KiwoomResult {

    @JsonProperty("inds_netprps")
    private List<IndsNetprpsItem> indsNetprps; // 업종별순매수

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
    public static class IndsNetprpsItem {

        @JsonProperty("inds_cd")
        private String indsCd; // 업종코드

        @JsonProperty("inds_nm")
        private String indsNm; // 업종명

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pre_smbol")
        private String preSmbol; // 대비부호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt; // 등락율

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("sc_netprps")
        private String scNetprps; // 증권순매수

        @JsonProperty("insrnc_netprps")
        private String insrncNetprps; // 보험순매수

        @JsonProperty("invtrt_netprps")
        private String invtrtNetprps; // 투신순매수

        @JsonProperty("bank_netprps")
        private String bankNetprps; // 은행순매수

        @JsonProperty("jnsinkm_netprps")
        private String jnsinkmNetprps; // 종신금순매수

        @JsonProperty("endw_netprps")
        private String endwNetprps; // 기금순매수

        @JsonProperty("etc_corp_netprps")
        private String etcCorpNetprps; // 기타법인순매수

        @JsonProperty("ind_netprps")
        private String indNetprps; // 개인순매수

        @JsonProperty("frgnr_netprps")
        private String frgnrNetprps; // 외국인순매수

        @JsonProperty("native_trmt_frgnr_netprps")
        private String nativeTrmtFrgnrNetprps; // 내국인대우외국인순매수

        @JsonProperty("natn_netprps")
        private String natnNetprps; // 국가순매수

        @JsonProperty("samo_fund_netprps")
        private String samoFundNetprps; // 사모펀드순매수

        @JsonProperty("orgn_netprps")
        private String orgnNetprps; // 기관계순매수
    }
}
