package com.example.stockanalysis.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

@JsonTest
@Import(JacksonConfig.class)
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesLocalDateWithConfiguredFormat() throws Exception {
        assertThat(objectMapper.writeValueAsString(LocalDate.of(2026, 7, 21)))
                .isEqualTo("\"2026-07-21\"");
    }

    @Test
    void serializesLocalDateTimeWithConfiguredFormat() throws Exception {
        assertThat(objectMapper.writeValueAsString(LocalDateTime.of(2026, 7, 21, 13, 14, 15)))
                .isEqualTo("\"2026-07-21T13:14:15\"");
    }

    @Test
    void serializesBigDecimalWithoutPrecisionLoss() throws Exception {
        BigDecimal amount = new BigDecimal("1234567890.123456789");

        assertThat(objectMapper.writeValueAsString(amount)).isEqualTo("1234567890.123456789");
    }

    @Test
    void includesNullFieldsAndIgnoresUnknownInputFields() throws Exception {
        assertThat(objectMapper.writeValueAsString(new NullableValue(null))).isEqualTo("{\"value\":null}");
        NullableValue value = objectMapper.readValue("{\"value\":\"ok\",\"unknown\":true}", NullableValue.class);
        assertThat(value.value()).isEqualTo("ok");
    }

    private record NullableValue(String value) {
    }
}
