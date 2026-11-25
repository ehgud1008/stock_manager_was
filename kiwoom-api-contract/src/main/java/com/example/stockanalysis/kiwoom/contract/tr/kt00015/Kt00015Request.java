package com.example.stockanalysis.kiwoom.contract.tr.kt00015;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 위탁종합거래내역요청(kt00015) 요청 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kt00015Request {

    @JsonProperty("strt_dt")
    private String strtDt; // 시작일자

    @JsonProperty("end_dt")
    private String endDt; // 종료일자

    @JsonProperty("tp")
    private String tp; // 구분

    @JsonProperty("stk_cd")
    private String stkCd; // 종목코드

    @JsonProperty("crnc_cd")
    private String crncCd; // 통화코드

    @JsonProperty("gds_tp")
    private String gdsTp; // 상품구분

    @JsonProperty("frgn_stex_code")
    private String frgnStexCode; // 해외거래소코드

    @JsonProperty("dmst_stex_tp")
    private String dmstStexTp; // 국내거래소구분

    @JsonProperty("qry_sort_tp")
    private String qrySortTp; // 조회정렬구분

}
