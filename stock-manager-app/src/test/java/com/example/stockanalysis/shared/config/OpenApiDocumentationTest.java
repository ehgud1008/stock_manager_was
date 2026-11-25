package com.example.stockanalysis.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/oracle/ddl/08_screener.sql,file:../database/oracle/ddl/11_unified_analysis.sql,file:../database/oracle/ddl/14_daily_market_report.sql,file:../database/oracle/ddl/18_snapshot_storage.sql",
        "storage.snapshots.monitoring-enabled=false",
        "storage.snapshots.archive-enabled=false",
        "storage.snapshots.archive-directory="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping mappings;

    @Test
    void documentsEveryImplementedApiWithResolvableSchemas() throws Exception {
        JsonNode document = document();
        assertThat(document.path("info").path("title").asText()).isEqualTo("Stock Manager API");
        Set<String> operationIds = new HashSet<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("com.example.stockanalysis")) return;
            for (String path : mapping.getPatternValues()) {
                if (!path.startsWith("/api/v1/")) continue;
                mapping.getMethodsCondition().getMethods().forEach(method -> {
                    JsonNode operation = document.path("paths").path(path).path(method.name().toLowerCase(Locale.ROOT));
                    assertThat(operation.isMissingNode()).as(method + " " + path).isFalse();
                    assertThat(operation.path("summary").asText()).as(path + " summary").isNotBlank();
                    assertThat(operation.path("tags").isEmpty()).as(path + " tags").isFalse();
                    assertThat(operationIds.add(operation.path("operationId").asText())).as("unique operation ID").isTrue();
                    assertThat(operation.path("responses").has("default")).isTrue();
                });
            }
        });
        assertThat(operationIds).hasSizeGreaterThanOrEqualTo(72);
        assertReferencesResolve(document, document);
    }

    @Test
    void preservesStatusCodesHeadersQueryParametersAndDistinctNestedRequestSchemas() throws Exception {
        JsonNode doc = document();
        JsonNode paths = doc.path("paths");
        assertThat(paths.has("/api/v1/analysis-scans")).isFalse();
        JsonNode unified = paths.path("/api/v1/unified-analysis/runs").path("post");
        assertThat(unified.path("responses").has("202")).isTrue();
        assertThat(parameter(unified, "Idempotency-Key").path("required").asBoolean()).isTrue();
        assertThat(parameter(paths.path("/api/v1/unified-analysis/runs/{id}/items").path("get"), "setup").path("in").asText()).isEqualTo("query");
        JsonNode unifiedRequest = resolve(doc, unified.path("requestBody").path("content").path("application/json").path("schema"));
        assertThat(unifiedRequest.path("properties").has("market")).isTrue();
        assertThat(unifiedRequest.path("properties").has("baseDate")).isFalse();
        assertThat(unifiedRequest.path("properties").has("priceMode")).isTrue();
        JsonNode screener = paths.path("/api/v1/screener/runs").path("post");
        assertThat(parameter(screener, "Idempotency-Key").path("required").asBoolean()).isFalse();
        JsonNode search = paths.path("/api/v1/screener").path("get");
        assertThat(parameter(search, "mode").path("in").asText()).isEqualTo("query");
        assertThat(parameter(search, "watchedCodes").path("in").asText()).isEqualTo("query");
        JsonNode decision = paths.path("/api/v1/stocks/{code}/analysis-snapshots").path("post");
        assertThat(parameter(decision, "Idempotency-Key").path("required").asBoolean()).isTrue();
        assertThat(decision.path("responses").has("200")).isTrue();
        assertThat(decision.path("responses").has("202")).isTrue();
        assertThat(decision.path("responses").path("202").path("headers").has("Location")).isTrue();
        assertThat(paths.path("/api/v1/backdata/validation-runs/kiwoom").path("post").path("responses").has("201")).isTrue();
        JsonNode healthResponse = resolve(doc, paths.path("/api/v1/health").path("get").path("responses")
                .path("200").path("content").path("*/*").path("schema"));
        assertThat(healthResponse.path("properties").has("data")).isTrue();
    }

    @Test
    void servesSwaggerUiAndYaml() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
    }

    private JsonNode document() throws Exception {
        return mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
    }

    private JsonNode parameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) return parameter;
        }
        throw new AssertionError("Missing parameter: " + name);
    }

    private JsonNode resolve(JsonNode document, JsonNode schema) {
        return schema.has("$ref") ? document.at(schema.path("$ref").asText().substring(1)) : schema;
    }

    private void assertReferencesResolve(JsonNode document, JsonNode node) {
        if (node.isObject() && node.has("$ref")) {
            String ref = node.path("$ref").asText();
            assertThat(ref).startsWith("#/");
            assertThat(document.at(ref.substring(1)).isMissingNode()).as(ref).isFalse();
        }
        node.elements().forEachRemaining(child -> assertReferencesResolve(document, child));
    }
}
