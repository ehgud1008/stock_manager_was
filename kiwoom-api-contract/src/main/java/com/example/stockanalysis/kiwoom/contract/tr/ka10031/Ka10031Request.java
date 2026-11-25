package com.example.stockanalysis.kiwoom.contract.tr.ka10031;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전일거래량상위요청(ka10031) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10031Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("qry_tp")
    private String qryTp; // 조회구분

    @JsonProperty("rank_strt")
    private String rankStrt; // 순위시작

    @JsonProperty("rank_end")
    private String rankEnd; // 순위끝

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
