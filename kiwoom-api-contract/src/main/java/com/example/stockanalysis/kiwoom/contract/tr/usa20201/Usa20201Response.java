package com.example.stockanalysis.kiwoom.contract.tr.usa20201;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 미국주식 관심종목 그룹 상세 조회(usa20201) 응답 DTO.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usa20201Response implements KiwoomResult {

    @JsonProperty("rtcd")
    private String rtcd; // 처리 결과

    @JsonProperty("nofj")
    private List<NofjItem> nofj; // 종목 갯수

    @JsonProperty("return_code")
    @JsonAlias({"returnCode"})
    private Integer returnCode; // 반환코드

    @JsonProperty("return_msg")
    @JsonAlias({"returnMsg"})
    private String returnMsg; // 반환메시지


    @Getter
    @Setter
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NofjItem {

        @JsonProperty("cod2")
        private String cod2; // 종목코드

        @JsonProperty("bgb")
        private String bgb; // 북마크 구분

        @JsonProperty("bgb_clr")
        private String bgbClr; // 북마크 컬러

        @JsonProperty("stex_tp")
        private String stexTp; // 거래소구분

        @JsonProperty("memo")
        private String memo; // 메모

        @JsonProperty("memo_opt")
        private String memoOpt; // 메모옵션
    }
}
