package com.example.stockanalysis.watchlist.application.port.out;

import com.example.stockanalysis.watchlist.domain.WatchlistGroup;
import java.util.List;

public interface WatchlistQueryPort {

    List<WatchlistGroup> findGroups();
}
