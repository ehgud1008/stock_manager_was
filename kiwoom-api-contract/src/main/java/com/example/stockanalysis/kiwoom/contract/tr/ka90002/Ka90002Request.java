package com.example.stockanalysis.kiwoom.contract.tr.ka90002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 테마구성종목요청(ka90002) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90002Request {

    @JsonProperty("date_tp")
    private String dateTp; // 날짜구분

    @JsonProperty("thema_grp_cd")
    private String themaGrpCd; // 테마그룹코드

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
