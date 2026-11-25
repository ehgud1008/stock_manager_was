package com.example.stockanalysis.screener;

import com.example.stockanalysis.screener.application.ScreenerStore;
import com.example.stockanalysis.screener.config.ScreenerConfiguration;
import com.example.stockanalysis.screener.infrastructure.JdbcScreenerStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class ScreenerConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ScreenerConfiguration.class)
            .withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());

    @Test void applicationUsesOnlyJdbcStoreWhenDataSourceExists() {
        runner.withBean(DataSource.class, () -> {
            var source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:screener_configuration;MODE=Oracle");
            return source;
        }).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ScreenerStore.class);
            assertThat(context.getBean(ScreenerStore.class)).isInstanceOf(JdbcScreenerStore.class);
            assertThat(context.getBean(ScreenerStore.class).storageMode()).isEqualTo("DATABASE");
        });
    }

    @Test void missingDatabaseFailsStartupInsteadOfFallingBackToMemory() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("memory fallback is disabled");
        });
    }
}
