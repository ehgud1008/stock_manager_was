package com.example.stockanalysis.watchlist.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.stockanalysis.watchlist.application.WatchlistQueryService;
import com.example.stockanalysis.watchlist.domain.WatchlistGroup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WatchlistController.class)
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchlistQueryService watchlistQueryService;

    @Test
    void returnsWatchlistGroupsWithoutStockCodeParameter() throws Exception {
        when(watchlistQueryService.getGroups()).thenReturn(List.of(
                new WatchlistGroup("001", "관심종목1"),
                new WatchlistGroup("002", "관심종목2")
        ));

        mockMvc.perform(get("/api/v1/watchlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("001"))
                .andExpect(jsonPath("$.data[0].name").value("관심종목1"))
                .andExpect(jsonPath("$.data[1].id").value("002"));
    }
}
