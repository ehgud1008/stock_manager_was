package com.example.stockanalysis.kiwoom.contract.tr.ka10042;

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
 * 순매수거래원순위요청(ka10042) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10042Response implements KiwoomResult {

    @JsonProperty("netprps_trde_ori_rank")
    private List<NetprpsTrdeOriRankItem> netprpsTrdeOriRank; // 순매수거래원순위

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
    public static class NetprpsTrdeOriRankItem {

        @JsonProperty("rank")
        private String rank; // 순위

        @JsonProperty("mmcm_cd")
        private String mmcmCd; // 회원사코드

        @JsonProperty("mmcm_nm")
        private String mmcmNm; // 회원사명
    }
}
