package com.example.stockanalysis.screener.infrastructure;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Explicit type whitelist from the same four-column master used by stock search. */
@Component
public class ScreenerStockTypeCatalog {
    private final Map<String, String> types;
    public ScreenerStockTypeCatalog(@Value("${screener.stock-master-location:classpath:stocks/stock_master.txt}") Resource resource) {
        Map<String, String> values = new HashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                String[] fields = line.split(",", -1);
                if (fields.length != 4 || !fields[0].trim().matches("[0-9A-Z]{6}") || fields[3].isBlank())
                    throw new IllegalArgumentException("Invalid screener stock master row");
                String previous = values.putIfAbsent(fields[0].trim(), fields[3].trim());
                if (previous != null && !previous.equals(fields[3].trim())) throw new IllegalArgumentException("Conflicting stock classification");
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot load screener stock master; ST whitelist is required", e); }
        if (!values.containsValue("ST")) throw new IllegalArgumentException("Stock master must contain ST classifications");
        types = Map.copyOf(values);
    }
    public String typeOf(String code) { return types.get(code); }
    public void requireStock(String code) {
        if (!"ST".equals(typeOf(code))) throw new IllegalArgumentException("STOCK_TYPE_NOT_ALLOWED");
    }
}
