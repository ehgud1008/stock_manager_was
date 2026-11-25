package com.example.stockanalysis.decision;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.stockanalysis.decision.DecisionModels.AiResponse;
import com.example.stockanalysis.decision.DecisionModels.MarketResponse;
public interface AiReviewPort { AiResponse review(JsonNode frozenInput); MarketResponse assess(JsonNode frozenInput); }
