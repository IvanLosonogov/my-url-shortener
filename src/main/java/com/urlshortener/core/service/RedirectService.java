package com.urlshortener.core.service;

import com.urlshortener.core.model.ShortLink;
import java.awt.Desktop;
import java.net.URI;

public class RedirectService {
    private final ShorteningService shorteningService;

    public RedirectService(ShorteningService shorteningService) {
        this.shorteningService = shorteningService;
    }

    public String redirect(String shortCode) {
        ShortLink shortLink = shorteningService.getShortLink(shortCode);

        if (shortLink == null) {
            return "Ошибка: ссылка не найдена";
        }

        if (!shortLink.isActive()) {
            return "Ошибка: ссылка неактивна";
        }

        if (shortLink.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            shortLink.setActive(false);
            return "Ошибка: срок действия ссылки истек";
        }

        if (shortLink.getCurrentClicks() >= shortLink.getMaxClicks()) {
            shortLink.setActive(false);
            return "Ошибка: лимит переходов исчерпан";
        }

        shortLink.setCurrentClicks(shortLink.getCurrentClicks() + 1);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(shortLink.getOriginalUrl()));
                return "Переход выполнен! Открываю: " + shortLink.getOriginalUrl();
            } else {
                return "Не удалось открыть браузер. URL: " + shortLink.getOriginalUrl();
            }
        } catch (Exception e) {
            return "Ошибка при открытии браузера: " + e.getMessage() +
                    "\nURL для ручного перехода: " + shortLink.getOriginalUrl();
        }
    }
}
