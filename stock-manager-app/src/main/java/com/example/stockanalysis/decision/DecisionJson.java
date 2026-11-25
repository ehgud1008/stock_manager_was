package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DecisionJson {
    private final ObjectMapper mapper;
    public DecisionJson(ObjectMapper mapper) { this.mapper = mapper.copy()
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY); }
    public JsonNode tree(Object value) { return mapper.valueToTree(value); }
    public JsonNode read(String value) {
        try { return mapper.readTree(value); } catch (Exception e) { throw new IllegalStateException("Invalid decision JSON"); }
    }
    public <T> T convert(JsonNode value, Class<T> type) {
        try { return mapper.treeToValue(value, type); } catch (Exception e) { throw new IllegalStateException("Invalid stored decision"); }
    }
    public String write(Object value) {
        try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Cannot serialize decision"); }
    }
    public String canonical(Object value) { return write(sort(tree(value))); }
    private JsonNode sort(JsonNode n) {
        if (n.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            var keys = new TreeSet<String>(); n.fieldNames().forEachRemaining(keys::add);
            keys.forEach(k -> result.set(k, sort(n.get(k)))); return result;
        }
        if (n.isArray()) { ArrayNode result = mapper.createArrayNode(); n.forEach(v -> result.add(sort(v))); return result; }
        if (n.isNumber()) return DecimalNode.valueOf(n.decimalValue().stripTrailingZeros());
        return n;
    }
    public String hash(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Cannot hash decision"); }
    }
}
