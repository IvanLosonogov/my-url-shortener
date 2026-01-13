package com.urlshortener.core.service;

import com.urlshortener.core.model.ShortLink;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatisticService {
    private final ShorteningService shorteningService;

    public StatisticService(ShorteningService shorteningService) {
        this.shorteningService = shorteningService;
    }

    public List<ShortLink> getUserLinks(UUID userId) {
        return shorteningService.getAllLinks().values().stream()
                .filter(link -> link.getOwnerId().equals(userId))
                .collect(Collectors.toList());
    }

    public String getLinkInfo(String shortCode, UUID userId) {
        ShortLink link = shorteningService.getShortLink(shortCode);
        if (link == null) {
            return "Ссылка не найдена";
        }

        if (!link.getOwnerId().equals(userId)) {
            return "Эта ссылка принадлежит другому пользователю";
        }

        return String.format(
                "Ссылка: %s\n" +
                        "Оригинальный URL: %s\n" +
                        "Создана: %s\n" +
                        "Действует до: %s\n" +
                        "Переходы: %d/%d\n" +
                        "Статус: %s",
                shortCode,
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getCurrentClicks(),
                link.getMaxClicks(),
                link.isActive() ? "активна" : "неактивна"
        );
    }
}
