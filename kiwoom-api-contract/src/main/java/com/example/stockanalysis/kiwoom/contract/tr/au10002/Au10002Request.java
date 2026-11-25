package com.example.stockanalysis.kiwoom.contract.tr.au10002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접근토큰폐기(au10002) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Au10002Request {

    @JsonProperty("appkey")
    private String appkey; // 앱키

    @JsonProperty("secretkey")
    private String secretkey; // 시크릿키

    @JsonProperty("token")
    private String token; // 접근토큰

}
