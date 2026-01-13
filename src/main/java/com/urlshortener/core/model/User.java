package com.urlshortener.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {
    private final UUID id;
    private List<String> shortLinkIds;

    public User(UUID id) {
        this.id = id;
        this.shortLinkIds = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public List<String> getShortLinkIds() {
        return shortLinkIds;
    }

    public void addShortLinkId(String shortLinkId) {
        this.shortLinkIds.add(shortLinkId);
    }
}
