package com.example.stockanalysis.kiwoom.tr.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.stockanalysis.kiwoom.contract.tr.ka10080.Ka10080Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10080.Ka10080Response;
import com.example.stockanalysis.kiwoom.contract.tr.ka10081.Ka10081Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10082.Ka10082Request;
import com.example.stockanalysis.kiwoom.contract.tr.ka10083.Ka10083Request;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class KiwoomTrCatalogTest {

    private final KiwoomTrCatalog catalog = new KiwoomTrCatalog(new ObjectMapper());

    @Test
    void loadsEveryGeneratedRestContract() {
        assertThat(catalog.size()).isEqualTo(306);
    }

    @Test
    void resolvesMinuteDailyWeeklyAndMonthlyChartContracts() {
        List<Class<?>> requestTypes = List.of(
                catalog.getRequired("ka10080").requestType(),
                catalog.getRequired("ka10081").requestType(),
                catalog.getRequired("ka10082").requestType(),
                catalog.getRequired("ka10083").requestType()
        );

        assertThat(requestTypes).containsExactly(
                Ka10080Request.class,
                Ka10081Request.class,
                Ka10082Request.class,
                Ka10083Request.class
        );
        KiwoomTrDefinition minute = catalog.getRequired("KA10080");
        assertThat(minute.uri()).isEqualTo("/api/dostk/chart");
        assertThat(minute.responseType()).isEqualTo(Ka10080Response.class);
    }

    @Test
    void rejectsApiIdWithoutGeneratedContract() {
        assertThatThrownBy(() -> catalog.getRequired("not-exists"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DTO가 등록되지 않은");
    }
}
