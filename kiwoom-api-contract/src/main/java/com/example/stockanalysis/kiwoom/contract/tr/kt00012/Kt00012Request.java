package com.example.stockanalysis.kiwoom.contract.tr.kt00012;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신용보증금율별주문가능수량조회요청(kt00012) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00012Request {

    @JsonProperty("stk_cd")
    private String stkCd; // 종목번호

    @JsonProperty("uv")
    private String uv; // 매수가격

}
