package com.example.stockanalysis.kiwoom.contract.tr.ka90006;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로그램매매차익잔고추이요청(ka90006) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka90006Request {

    @JsonProperty("date")
    private String date; // 날짜

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
