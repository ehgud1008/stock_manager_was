package com.example.stockanalysis.watchlist.infrastructure.kiwoom;

import com.example.stockanalysis.kiwoom.client.watchlist.KiwoomWatchlistClient;
import com.example.stockanalysis.kiwoom.contract.tr.ka01300.Ka01300Response.NofiItem;
import com.example.stockanalysis.watchlist.application.port.out.WatchlistQueryPort;
import com.example.stockanalysis.watchlist.domain.WatchlistGroup;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KiwoomWatchlistAdapter implements WatchlistQueryPort {

    private final KiwoomWatchlistClient client;

    public KiwoomWatchlistAdapter(KiwoomWatchlistClient client) {
        this.client = client;
    }

    @Override
    public List<WatchlistGroup> findGroups() {
        List<NofiItem> groups = client.getGroups().body().getNofi();
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .map(group -> new WatchlistGroup(group.getGcod(), group.getName()))
                .toList();
    }
}
