package com.example.stockanalysis.backdata.validation.model;

import java.util.Collection;
import java.util.Optional;

public interface BackdataSchemaRegistry {

    Optional<BackdataSchema> find(String apiId);

    Collection<BackdataSchema> all();
}
