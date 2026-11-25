package com.example.stockanalysis.kiwoom.contract.tr.ka90007;

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
 * 프로그램매매누적추이요청(ka90007) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90007Response implements KiwoomResult {

    @JsonProperty("prm_trde_acc_trnsn")
    private List<PrmTrdeAccTrnsnItem> prmTrdeAccTrnsn; // 프로그램매매누적추이

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
    public static class PrmTrdeAccTrnsnItem {

        @JsonProperty("dt")
        private String dt; // 일자

        @JsonProperty("kospi200")
        private String kospi200; // KOSPI200

        @JsonProperty("basis")
        private String basis; // BASIS

        @JsonProperty("dfrt_trde_tdy")
        private String dfrtTrdeTdy; // 차익거래당일

        @JsonProperty("dfrt_trde_acc")
        private String dfrtTrdeAcc; // 차익거래누적

        @JsonProperty("ndiffpro_trde_tdy")
        private String ndiffproTrdeTdy; // 비차익거래당일

        @JsonProperty("ndiffpro_trde_acc")
        private String ndiffproTrdeAcc; // 비차익거래누적

        @JsonProperty("all_tdy")
        private String allTdy; // 전체당일

        @JsonProperty("all_acc")
        private String allAcc; // 전체누적
    }
}
