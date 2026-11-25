package com.example.stockanalysis.unified.portfolio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("unified.portfolio")
public record PortfolioProperties(@DefaultValue("false") boolean enabled,
                                  @DefaultValue("false") boolean operationsEnabled,
                                  @DefaultValue("false") boolean testDeliveryEnabled) {}
