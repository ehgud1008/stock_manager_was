package com.example.stockanalysis.backdata.validation.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MapBackdataSchemaRegistry implements BackdataSchemaRegistry {

    private final Map<String, BackdataSchema> schemas;

    public MapBackdataSchemaRegistry(Collection<BackdataSchema> schemas) {
        Map<String, BackdataSchema> indexed = new LinkedHashMap<>();
        for (BackdataSchema schema : Objects.requireNonNull(schemas, "schemas")) {
            BackdataSchema previous = indexed.put(schema.apiId(), schema);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate backdata schema: " + schema.apiId());
            }
        }
        this.schemas = Map.copyOf(indexed);
    }

    @Override
    public Optional<BackdataSchema> find(String apiId) {
        return Optional.ofNullable(schemas.get(apiId));
    }

    @Override
    public Collection<BackdataSchema> all() {
        return List.copyOf(schemas.values());
    }
}
