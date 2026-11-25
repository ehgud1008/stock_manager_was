package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.stockanalysis.backdata.validation.model.BackdataDatasetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class KiwoomBackdataCatalogTest {

    private final KiwoomBackdataCatalog catalog = new KiwoomBackdataCatalog();

    @Test
    void registersAllCommonAndExtendedBacktestDataTrs() {
        assertThat(catalog.all())
                .hasSize(21)
                .extracting(KiwoomBackdataTrDefinition::apiId)
                .containsExactlyInAnyOrderElementsOf(Set.of(
                        "ka10099", "ka10100", "ka10001", "ka10101",
                        "ka10080", "ka10081", "ka10082", "ka10083",
                        "ka10086", "ka10015", "ka20006", "ka10059",
                        "ka10008", "ka10013", "ka10014", "ka20068",
                        "ka90013", "ka10045", "ka10047", "ka90001", "ka90002"
                ));
    }

    @Test
    void exposesDailyCandleSchema() {
        KiwoomBackdataTrDefinition definition = catalog.getRequired("ka10081");

        assertThat(definition.schema().datasetType())
                .isEqualTo(BackdataDatasetType.DAILY_CANDLE);
        assertThat(definition.schema().historicalBackfillSupported()).isTrue();
        assertThat(definition.schema().requiredFields())
                .containsExactlyInAnyOrder("open", "high", "low", "close", "volume");
    }

    @Test
    void everyCatalogFieldMatchesGeneratedContractDto() {
        for (KiwoomBackdataTrDefinition definition : catalog.all()) {
            Class<?> rowType = responseRowType(definition);
            Set<String> responseFields = jsonProperties(rowType);
            assertThat(responseFields)
                    .as(definition.apiId() + " response field mappings")
                    .containsAll(definition.fieldMappings().keySet());

            Set<String> requestFields = jsonProperties(definition.requestType());
            assertOptionalField(requestFields, definition.requestStockCodeField(), definition.apiId());
            assertOptionalField(requestFields, definition.requestReferenceCodeField(), definition.apiId());
            assertThat(requestFields)
                    .as(definition.apiId() + " variant request fields")
                    .containsAll(definition.variantRequestFields());
        }
    }

    private Class<?> responseRowType(KiwoomBackdataTrDefinition definition) {
        if (!definition.hasRows()) {
            return definition.responseType();
        }
        Field rowField = Arrays.stream(definition.responseType().getDeclaredFields())
                .filter(field -> {
                    JsonProperty property = field.getAnnotation(JsonProperty.class);
                    return property != null && property.value().equals(definition.rowField());
                })
                .findFirst()
                .orElseThrow();
        ParameterizedType type = (ParameterizedType) rowField.getGenericType();
        return (Class<?>) type.getActualTypeArguments()[0];
    }

    private Set<String> jsonProperties(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getAnnotation(JsonProperty.class))
                .filter(property -> property != null)
                .map(JsonProperty::value)
                .collect(Collectors.toSet());
    }

    private void assertOptionalField(Set<String> fields, String field, String apiId) {
        if (field != null) {
            assertThat(fields).as(apiId + " request field").contains(field);
        }
    }
}
