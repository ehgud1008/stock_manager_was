package com.example.stockanalysis.decision;

import com.fasterxml.jackson.databind.JsonNode;

/** External documents are untrusted data, never instructions for the analysis model. */
public interface NewsEvidenceCollector {
    JsonNode collect(JsonNode frozenInput);
}
