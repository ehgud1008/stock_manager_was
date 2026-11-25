package com.example.stockanalysis.backdata.infrastructure.kiwoom;

import com.example.stockanalysis.backdata.validation.model.BackdataRecord;
import com.example.stockanalysis.kiwoom.contract.common.KiwoomResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class KiwoomBackdataRecordMapper {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> ABSOLUTE_VALUE_FIELDS = Set.of(
            "open", "high", "low", "close", "currentPrice", "lastPrice",
            "volume", "tradingAmount", "listedShares", "marketCap",
            "preMarketVolume", "regularMarketVolume", "afterMarketVolume",
            "foreignHolding", "holdingQuantity", "acquiredQuantity", "foreignLimit",
            "newCredit", "repayment", "creditBalance", "creditAmount",
            "shortVolume", "accumulatedShortVolume", "shortTradingAmount",
            "shortAveragePrice", "loanContracts", "loanRepayments",
            "loanBalance", "loanBalanceAmount", "programSellAmount",
            "programBuyAmount", "programSellQuantity", "programBuyQuantity",
            "stockCount", "risingStockCount", "fallingStockCount"
    );

    private final ObjectMapper objectMapper;

    public KiwoomBackdataRecordMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<BackdataRecord> map(
            KiwoomBackdataTrDefinition definition,
            Map<String, Object> request,
            KiwoomResult response,
            LocalDate asOfDate,
            Instant observedAt
    ) {
        JsonNode root = objectMapper.valueToTree(response);
        JsonNode requestNode = objectMapper.valueToTree(request);
        List<JsonNode> rows = extractRows(definition, root);
        List<BackdataRecord> records = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            String stockCode = firstText(
                    text(row, definition.rowStockCodeField()),
                    text(root, definition.rowStockCodeField()),
                    text(requestNode, definition.requestStockCodeField())
            );
            String referenceCode = firstText(
                    text(row, definition.rowReferenceCodeField()),
                    text(root, definition.rowReferenceCodeField()),
                    text(requestNode, definition.requestReferenceCodeField())
            );
            LocalDate businessDate = definition.businessDateField() == null
                    ? asOfDate
                    : parseDate(text(row, definition.businessDateField()));
            Map<String, String> values = mapValues(definition, row);
            records.add(new BackdataRecord(
                    definition.apiId(),
                    definition.schema().datasetType(),
                    stockCode,
                    referenceCode,
                    variant(definition, requestNode),
                    businessDate,
                    observedAt,
                    values
            ));
        }
        return records;
    }

    private List<JsonNode> extractRows(
            KiwoomBackdataTrDefinition definition,
            JsonNode root
    ) {
        if (!definition.hasRows()) {
            return List.of(root);
        }
        JsonNode rowsNode = root.get(definition.rowField());
        if (rowsNode == null || rowsNode.isNull()) {
            return List.of();
        }
        if (!rowsNode.isArray()) {
            return List.of(rowsNode);
        }
        List<JsonNode> rows = new ArrayList<>();
        rowsNode.forEach(rows::add);
        return rows;
    }

    private Map<String, String> mapValues(
            KiwoomBackdataTrDefinition definition,
            JsonNode row
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        definition.fieldMappings().forEach((source, target) -> {
            String value = text(row, source);
            if (value != null) {
                values.put(target, normalize(target, value));
            }
        });
        return values;
    }

    private String normalize(String targetField, String value) {
        String normalized = collapseRepeatedLeadingSign(
                value.trim().replace(",", "")
        );
        boolean hasConflictingLeadingSigns = normalized.length() > 1
                && isSign(normalized.charAt(0))
                && isSign(normalized.charAt(1));
        if (normalized.startsWith("+") && !hasConflictingLeadingSigns) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("-")
                && !hasConflictingLeadingSigns
                && ABSOLUTE_VALUE_FIELDS.contains(targetField)) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String collapseRepeatedLeadingSign(String value) {
        if (value.length() < 2) {
            return value;
        }

        char sign = value.charAt(0);
        if (sign != '+' && sign != '-') {
            return value;
        }

        int firstDigitIndex = 1;
        while (firstDigitIndex < value.length()
                && value.charAt(firstDigitIndex) == sign) {
            firstDigitIndex++;
        }
        if (firstDigitIndex == 1) {
            return value;
        }
        return sign + value.substring(firstDigitIndex);
    }

    private boolean isSign(char value) {
        return value == '+' || value == '-';
    }

    private String variant(
            KiwoomBackdataTrDefinition definition,
            JsonNode requestNode
    ) {
        if (definition.variantRequestFields().isEmpty()) {
            return null;
        }
        return definition.variantRequestFields().stream()
                .sorted()
                .map(field -> field + "=" + firstText(text(requestNode, field), ""))
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace("-", "").replace(".", "").trim();
        if (normalized.length() > 8) {
            normalized = normalized.substring(0, 8);
        }
        try {
            return LocalDate.parse(normalized, BASIC_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
