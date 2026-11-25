package com.example.stockanalysis.watchlist.api;

import com.example.stockanalysis.watchlist.domain.WatchlistGroup;

public record WatchlistGroupResponse(String id, String name) {

    public static WatchlistGroupResponse from(WatchlistGroup group) {
        return new WatchlistGroupResponse(group.id(), group.name());
    }
}
