package com.example.stockanalysis.kiwoom.contract.tr.ka30010;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ELW잔량순위요청(ka30010) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30010Request {

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("rght_tp")
    private String rghtTp; // 권리구분

    @JsonProperty("trde_end_skip")
    private String trdeEndSkip; // 거래종료제외

}
