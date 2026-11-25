package com.example.stockanalysis.screener;

import com.example.stockanalysis.screener.infrastructure.ScreenerStockTypeCatalog;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ScreenerStockTypeCatalogTest {
    @Test void exactStIncludesPreferredSharesAndRejectsEtfAndUnknown() {
        var catalog = new ScreenerStockTypeCatalog(new ClassPathResource("stocks/stock_master.txt"));
        assertThat(catalog.typeOf("005930")).isEqualTo("ST");
        assertThat(catalog.typeOf("005935")).isEqualTo("ST");
        assertThat(catalog.typeOf("069500")).isNotEqualTo("ST");
        catalog.requireStock("005935");
        assertThatThrownBy(() -> catalog.requireStock("069500")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.requireStock("ZZZZZZ")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void classificationMustBePresentAndConsistent() {
        assertThatThrownBy(() -> catalog("005930,삼성전자,코스피,ST\n005930,삼성전자,코스피,ETF"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog("005930,삼성전자,코스피,"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
    private ScreenerStockTypeCatalog catalog(String text) {
        return new ScreenerStockTypeCatalog(new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8)));
    }
}
