package com.example.stockanalysis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/oracle/ddl/08_screener.sql,file:../database/oracle/ddl/11_unified_analysis.sql,file:../database/oracle/ddl/14_daily_market_report.sql,file:../database/oracle/ddl/18_snapshot_storage.sql",
        "storage.snapshots.monitoring-enabled=false",
        "storage.snapshots.archive-enabled=false",
        "storage.snapshots.archive-directory="
})
@ActiveProfiles("test")
class StockAnalysisApplicationTest {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.stockanalysis.analysis.application.port.out.AnalysisStore analysisStore;

    @Test
    void contextLoads() {
        org.assertj.core.api.Assertions.assertThat(analysisStore)
                .isInstanceOf(com.example.stockanalysis.analysis.infrastructure.jdbc.JdbcAnalysisStore.class);
    }
}
