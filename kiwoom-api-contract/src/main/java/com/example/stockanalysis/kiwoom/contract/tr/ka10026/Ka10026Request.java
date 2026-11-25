package com.example.stockanalysis.kiwoom.contract.tr.ka10026;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고저PER요청(ka10026) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ka10026Request {

    @JsonProperty("pertp")
    private String pertp; // PER구분

    @JsonProperty("stex_tp")
    private String stexTp; // 거래소구분

}
