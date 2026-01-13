package com.urlshortener.core.service;

import com.urlshortener.core.config.AppConfig;
import com.urlshortener.core.model.ShortLink;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LinkLifecycleService {
    private final ShorteningService shorteningService;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;

    private final int checkIntervalMinutes;

    public LinkLifecycleService(ShorteningService shorteningService) {
        this.shorteningService = shorteningService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        this.checkIntervalMinutes = AppConfig.getInstance().getCleanupIntervalMinutes();

    }

    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        System.out.println("Служба очистки ссылок запущена. Проверка каждые " + checkIntervalMinutes + " минут");
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredLinks();
            } catch (Exception e) {
                System.err.println("Ошибка при очистке ссылок: " + e.getMessage());
            }
        }, 0, checkIntervalMinutes, TimeUnit.MINUTES);
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        scheduler.shutdown();
        System.out.println("Служба очистки ссылок остановлена");
    }

    private void cleanupExpiredLinks() {
        LocalDateTime now = LocalDateTime.now();
        int deactivatedCount = 0;
        int expiredCount = 0;

        for (ShortLink link : shorteningService.getAllLinks().values()) {
            boolean shouldDeactivate = false;
            String reason = "";

            if (link.getExpiresAt().isBefore(now)) {
                shouldDeactivate = true;
                reason = "истек срок действия";
                expiredCount++;
            }

            if (link.getCurrentClicks() >= link.getMaxClicks() && link.isActive()) {
                shouldDeactivate = true;
                reason = "исчерпан лимит переходов";
                deactivatedCount++;
            }

            if (shouldDeactivate && link.isActive()) {
                link.setActive(false);
                System.out.println("Ссылка " + link.getShortCode() + " деактивирована: " + reason);
            }
        }

        if (deactivatedCount > 0 || expiredCount > 0) {
            System.out.println("Очистка завершена: " +
                    deactivatedCount + " по лимиту, " +
                    expiredCount + " по сроку");
        }
    }

    public String checkLinkStatus(String shortCode) {
        ShortLink link = shorteningService.getShortLink(shortCode);
        if (link == null) {
            return "Ссылка не найдена";
        }

        LocalDateTime now = LocalDateTime.now();
        StringBuilder status = new StringBuilder();

        if (!link.isActive()) {
            status.append("Ссылка неактивна\n");
        }

        if (link.getExpiresAt().isBefore(now)) {
            status.append("Срок действия истек: ").append(link.getExpiresAt()).append("\n");
        }

        if (link.getCurrentClicks() >= link.getMaxClicks()) {
            status.append("Лимит переходов исчерпан: ")
                    .append(link.getCurrentClicks()).append("/").append(link.getMaxClicks()).append("\n");
        }

        if (status.length() == 0) {
            status.append("Ссылка активна\n");
            status.append("Осталось переходов: ")
                    .append(link.getMaxClicks() - link.getCurrentClicks()).append("\n");
            status.append("Действует до: ").append(link.getExpiresAt());
        }

        return status.toString();
    }
}
