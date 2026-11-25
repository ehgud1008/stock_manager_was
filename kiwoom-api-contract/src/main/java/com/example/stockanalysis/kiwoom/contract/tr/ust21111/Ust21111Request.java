package com.example.stockanalysis.kiwoom.contract.tr.ust21111;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 원화출금가능 금액 조회(원화대용 포함)(ust21111) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ust21111Request {

    // 요청 본문 필드 없음

}
