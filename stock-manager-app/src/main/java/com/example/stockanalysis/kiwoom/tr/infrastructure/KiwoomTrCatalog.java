package com.example.stockanalysis.kiwoom.tr.infrastructure;

import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class KiwoomTrCatalog {

    private static final String CATALOG_RESOURCE = "kiwoom-tr-catalog.json";
    private static final String CONTRACT_PACKAGE =
            "com.example.stockanalysis.kiwoom.contract.tr.";

    private final Map<String, KiwoomTrDefinition> definitions;

    public KiwoomTrCatalog(ObjectMapper objectMapper) {
        this.definitions = loadDefinitions(objectMapper);
    }

    public KiwoomTrDefinition getRequired(String apiId) {
        String normalizedApiId = normalizeApiId(apiId);
        KiwoomTrDefinition definition = definitions.get(normalizedApiId);
        if (definition == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "요청/응답 DTO가 등록되지 않은 키움 전문입니다: " + normalizedApiId
            );
        }
        return definition;
    }

    public int size() {
        return definitions.size();
    }

    private Map<String, KiwoomTrDefinition> loadDefinitions(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            List<CatalogEntry> entries = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() { }
            );
            Map<String, KiwoomTrDefinition> loaded = new LinkedHashMap<>();
            for (CatalogEntry entry : entries) {
                KiwoomTrDefinition definition = toDefinition(entry);
                if (loaded.put(definition.apiId(), definition) != null) {
                    throw new IllegalStateException(
                            "중복된 키움 전문 메타데이터입니다: " + definition.apiId()
                    );
                }
            }
            return Map.copyOf(loaded);
        } catch (IOException exception) {
            throw new IllegalStateException("키움 전문 카탈로그를 읽을 수 없습니다.", exception);
        }
    }

    private KiwoomTrDefinition toDefinition(CatalogEntry entry) {
        String apiId = normalizeApiId(entry.apiId());
        if (!"POST".equalsIgnoreCase(entry.method())) {
            throw new IllegalStateException("지원하지 않는 키움 전문 HTTP 메서드입니다: " + apiId);
        }
        if (entry.uri() == null || !entry.uri().startsWith("/") || entry.uri().contains("://")) {
            throw new IllegalStateException("유효하지 않은 키움 전문 URI입니다: " + apiId);
        }

        String classPrefix = Character.toUpperCase(apiId.charAt(0)) + apiId.substring(1);
        String packageName = CONTRACT_PACKAGE + apiId + ".";
        Class<?> requestType = loadClass(packageName + classPrefix + "Request", apiId);
        Class<?> rawResponseType = loadClass(packageName + classPrefix + "Response", apiId);
        if (!KiwoomResult.class.isAssignableFrom(rawResponseType)) {
            throw new IllegalStateException("KiwoomResult 응답 DTO가 아닙니다: " + apiId);
        }

        return new KiwoomTrDefinition(
                apiId,
                entry.apiName(),
                entry.uri(),
                requestType,
                rawResponseType.asSubclass(KiwoomResult.class)
        );
    }

    private Class<?> loadClass(String className, String apiId) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("키움 전문 DTO를 찾을 수 없습니다: " + apiId, exception);
        }
    }

    private String normalizeApiId(String apiId) {
        if (apiId == null || apiId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "api-id가 필요합니다.");
        }
        return apiId.trim().toLowerCase(Locale.ROOT);
    }

    private record CatalogEntry(
            String apiId,
            String apiName,
            String method,
            String uri
    ) {
    }
}
