package com.example.stockanalysis.kiwoom.contract.tr.ka30009;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ELW등락율순위요청(ka30009) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka30009Request {

    @JsonProperty("sort_tp")
    private String sortTp; // 정렬구분

    @JsonProperty("rght_tp")
    private String rghtTp; // 권리구분

    @JsonProperty("trde_end_skip")
    private String trdeEndSkip; // 거래종료제외

}
