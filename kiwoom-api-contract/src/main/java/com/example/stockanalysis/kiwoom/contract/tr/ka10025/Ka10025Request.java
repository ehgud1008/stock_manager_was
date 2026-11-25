package com.example.stockanalysis.kiwoom.contract.tr.ka10025;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 매물대집중요청(ka10025) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10025Request {

    @JsonProperty("mrkt_tp")
    private String mrktTp; // 시장구분

    @JsonProperty("prps_cnctr_rt")
    private String prpsCnctrRt; // 매물집중비율

    @JsonProperty("cur_prc_entry")
    private String curPrcEntry; // 현재가진입

    @JsonProperty("prpscnt")
    private String prpscnt; // 매물대수

    @JsonProperty("cycle_tp")
    private String cycleTp; // 주기구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
