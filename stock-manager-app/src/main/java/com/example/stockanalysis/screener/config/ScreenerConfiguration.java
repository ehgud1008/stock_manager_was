package com.example.stockanalysis.screener.config;

import com.example.stockanalysis.engine.screener.DefaultScreenerEngine;
import com.example.stockanalysis.screener.application.ScreenerStore;
import com.example.stockanalysis.screener.infrastructure.JdbcScreenerStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScreenerProperties.class)
public class ScreenerConfiguration {
    @Bean public DefaultScreenerEngine screenerEngine(ScreenerProperties properties) { return new DefaultScreenerEngine(properties.calculation()); }
    @Bean public ScreenerStore screenerStore(ObjectProvider<DataSource> source,ObjectMapper mapper,ObjectProvider<com.example.stockanalysis.storage.SnapshotRepository> snapshots) {
        DataSource dataSource = source.getIfAvailable();
        if (dataSource == null) {
            throw new IllegalStateException("Screener requires a database DataSource. Configure DB_URL, DB_USERNAME and DB_PASSWORD; memory fallback is disabled.");
        }
        return new JdbcScreenerStore(dataSource,mapper,snapshots.getIfAvailable());
    }
    @Bean(name = "screenerExecutor", destroyMethod = "shutdownNow")
    public ExecutorService screenerExecutor(ScreenerProperties properties) {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(properties.queueCapacity()),
                task -> { Thread thread = new Thread(task, "screener-worker"); thread.setDaemon(true); return thread; }, new ThreadPoolExecutor.AbortPolicy());
    }
}
