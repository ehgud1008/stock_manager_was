package com.example.stockanalysis.analysis.config;

import com.example.stockanalysis.analysis.application.port.out.AnalysisStore;
import com.example.stockanalysis.analysis.infrastructure.jdbc.JdbcAnalysisStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Resolve DataSource at bean creation, not component-scan time (before JDBC auto-configuration). */
@Configuration(proxyBeanMethods=false)
public class AnalysisPersistenceConfiguration {
    @Bean
    public AnalysisStore analysisStore(DataSource source, ObjectMapper mapper) {
        return new JdbcAnalysisStore(source,mapper);
    }
}
