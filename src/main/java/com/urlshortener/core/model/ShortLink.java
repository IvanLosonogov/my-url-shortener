package com.urlshortener.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ShortLink {
    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;
    private final LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int maxClicks;
    private int currentClicks;
    private boolean isActive;

    public ShortLink(String shortCode, String originalUrl, UUID ownerId,
                     LocalDateTime createdAt, LocalDateTime expiresAt,
                     int maxClicks, int currentClicks, boolean isActive) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxClicks = maxClicks;
        this.currentClicks = currentClicks;
        this.isActive = isActive;
    }

    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public UUID getOwnerId() { return ownerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public int getMaxClicks() { return maxClicks; }
    public void setMaxClicks(int maxClicks) { this.maxClicks = maxClicks; }
    public int getCurrentClicks() { return currentClicks; }
    public void setCurrentClicks(int currentClicks) { this.currentClicks = currentClicks; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
