package com.example.stockanalysis.kiwoom.contract.tr.ka40001;

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
 * ETF수익율요청(ka40001) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka40001Response implements KiwoomResult {

    @JsonProperty("etfprft_rt_lst")
    private List<EtfprftRtLstItem> etfprftRtLst; // ETF수익율

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
    public static class EtfprftRtLstItem {

        @JsonProperty("etfprft_rt")
        private String etfprftRt; // ETF수익률

        @JsonProperty("cntr_prft_rt")
        private String cntrPrftRt; // 체결수익률

        @JsonProperty("for_netprps_qty")
        private String forNetprpsQty; // 외인순매수수량

        @JsonProperty("orgn_netprps_qty")
        private String orgnNetprpsQty; // 기관순매수수량
    }
}
