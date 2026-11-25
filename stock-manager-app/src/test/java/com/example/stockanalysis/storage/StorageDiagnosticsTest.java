package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;

class StorageDiagnosticsTest {
    @Test void exposesOracleCodeAndLocationWithoutLoggingSqlOrSensitiveMessages(){
        var sql=new SQLException("secret-token-and-message", "42000", 942);
        var error=new BadSqlGrammarException("private task", "SELECT private_message FROM private_table", sql);
        error.setStackTrace(new StackTraceElement[]{new StackTraceElement("com.example.stockanalysis.storage.StorageService","deliver","StorageService.java",123)});
        assertThat(StorageConfiguration.failureDetails(error)).contains("sqlCode=942","sqlState=42000","StorageService.deliver:123")
            .doesNotContain("secret-token","private_message","private_table","private task");
    }
}
