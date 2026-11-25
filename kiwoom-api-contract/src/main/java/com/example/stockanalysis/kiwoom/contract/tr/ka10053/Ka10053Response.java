package com.example.stockanalysis.kiwoom.contract.tr.ka10053;

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
 * 당일상위이탈원요청(ka10053) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10053Response implements KiwoomResult {

    @JsonProperty("tdy_upper_scesn_ori")
    private List<TdyUpperScesnOriItem> tdyUpperScesnOri; // 당일상위이탈원

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
    public static class TdyUpperScesnOriItem {

        @JsonProperty("sel_scesn_tm")
        private String selScesnTm; // 매도이탈시간

        @JsonProperty("sell_qty")
        private String sellQty; // 매도수량

        @JsonProperty("sel_upper_scesn_ori")
        private String selUpperScesnOri; // 매도상위이탈원

        @JsonProperty("buy_scesn_tm")
        private String buyScesnTm; // 매수이탈시간

        @JsonProperty("buy_qty")
        private String buyQty; // 매수수량

        @JsonProperty("buy_upper_scesn_ori")
        private String buyUpperScesnOri; // 매수상위이탈원

        @JsonProperty("qry_dt")
        private String qryDt; // 조회일자

        @JsonProperty("qry_tm")
        private String qryTm; // 조회시간
    }
}
