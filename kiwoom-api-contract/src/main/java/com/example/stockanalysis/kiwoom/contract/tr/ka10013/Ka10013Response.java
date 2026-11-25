package com.example.stockanalysis.kiwoom.contract.tr.ka10013;

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
 * 신용매매동향요청(ka10013) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10013Response implements KiwoomResult {

    @JsonProperty("crd_trde_trend")
    private List<CrdTrdeTrendItem> crdTrdeTrend; // 신용매매동향

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
    public static class CrdTrdeTrendItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("cur_prc")
        private String curPrc; // 현재가

        @JsonProperty("pred_pre_sig")
        private String predPreSig; // 전일대비기호

        @JsonProperty("pred_pre")
        private String predPre; // 전일대비

        @JsonProperty("trde_qty")
        private String trdeQty; // 거래량

        @JsonProperty("new")
        private String _new; // 신규

        @JsonProperty("rpya")
        private String rpya; // 상환

        @JsonProperty("remn")
        private String remn; // 잔고

        @JsonProperty("amt")
        private String amt; // 금액

        @JsonProperty("pre")
        private String pre; // 대비

        @JsonProperty("shr_rt")
        private String shrRt; // 공여율

        @JsonProperty("remn_rt")
        private String remnRt; // 잔고율
    }
}
