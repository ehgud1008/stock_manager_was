package com.example.stockanalysis.kiwoom.contract.tr.ka10048;

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
 * ELW일별민감도지표요청(ka10048) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10048Response implements KiwoomResult {

    @JsonProperty("elwdaly_snst_ix")
    private List<ElwdalySnstIxItem> elwdalySnstIx; // ELW일별민감도지표

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
    public static class ElwdalySnstIxItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("iv")
        private String iv; // IV

        @JsonProperty("delta")
        private String delta; // 델타

        @JsonProperty("gam")
        private String gam; // 감마

        @JsonProperty("theta")
        private String theta; // 쎄타

        @JsonProperty("vega")
        private String vega; // 베가

        @JsonProperty("law")
        private String law; // 로

        @JsonProperty("lp")
        private String lp; // LP
    }
}
