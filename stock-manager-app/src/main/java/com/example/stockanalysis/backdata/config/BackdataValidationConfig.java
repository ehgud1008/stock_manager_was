package com.example.stockanalysis.backdata.config;

import com.example.stockanalysis.backdata.infrastructure.kiwoom.KiwoomBackdataCatalog;
import com.example.stockanalysis.backdata.validation.api.BackdataValidationEngine;
import com.example.stockanalysis.backdata.validation.internal.DefaultBackdataValidationEngine;
import com.example.stockanalysis.backdata.validation.model.BackdataSchemaRegistry;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BackdataValidationConfig {

    @Bean
    public BackdataSchemaRegistry backdataSchemaRegistry(KiwoomBackdataCatalog catalog) {
        return catalog.schemaRegistry();
    }

    @Bean
    public BackdataValidationEngine backdataValidationEngine(
            BackdataSchemaRegistry schemaRegistry
    ) {
        return new DefaultBackdataValidationEngine(schemaRegistry);
    }

    @Bean
    public Clock backdataClock() {
        return Clock.systemUTC();
    }
}
