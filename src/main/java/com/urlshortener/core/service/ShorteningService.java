package com.urlshortener.core.service;

import com.urlshortener.core.config.AppConfig;
import com.urlshortener.core.model.ShortLink;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShorteningService {
    private Map<String, ShortLink> linkStorage;
    private static final String STORAGE_FILE = "url_shortener_links.txt";

    public ShorteningService() {
        this.linkStorage = new ConcurrentHashMap<>();
        loadFromFile();
        System.out.println("Сервис ссылок инициализирован. Загружено: " +
                linkStorage.size() + " ссылок");
    }

    private synchronized void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();

            for (ShortLink link : linkStorage.values()) {
                String line = String.join("|",
                        link.getShortCode(),
                        link.getOriginalUrl(),
                        link.getOwnerId().toString(),
                        link.getCreatedAt().toString(),
                        link.getExpiresAt().toString(),
                        String.valueOf(link.getMaxClicks()),
                        String.valueOf(link.getCurrentClicks()),
                        String.valueOf(link.isActive())
                );
                lines.add(line);
            }

            Path filePath = Paths.get(STORAGE_FILE);
            Files.write(filePath, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

        } catch (IOException e) {
            System.err.println("Ошибка сохранения ссылок: " + e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        Path filePath = Paths.get(STORAGE_FILE);

        if (!Files.exists(filePath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            int loadedCount = 0;

            for (String line : lines) {
                try {
                    String[] parts = line.split("\\|", 8);
                    if (parts.length != 8) {
                        System.err.println("Пропущена некорректная строка: " + line);
                        continue;
                    }

                    ShortLink link = new ShortLink(
                            parts[0],
                            parts[1],
                            UUID.fromString(parts[2]),
                            LocalDateTime.parse(parts[3]),
                            LocalDateTime.parse(parts[4]),
                            Integer.parseInt(parts[5]),
                            Integer.parseInt(parts[6]),
                            Boolean.parseBoolean(parts[7])
                    );

                    linkStorage.put(parts[0], link);
                    loadedCount++;

                } catch (Exception e) {
                    System.err.println("Ошибка парсинга строки: " + line);
                    System.err.println("Причина: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла ссылок: " + e.getMessage());
        }
    }

    public String createShortLink(String originalUrl, UUID ownerId) {
        if (originalUrl.length() > AppConfig.getInstance().getUrlMaxLength()) {
            throw new IllegalArgumentException("URL слишком длинный");
        }

        String shortCode;
        do {
            shortCode = generateShortCode(originalUrl, ownerId);
        } while (linkStorage.containsKey(shortCode));

        AppConfig config = AppConfig.getInstance();

        LocalDateTime now = LocalDateTime.now();
        ShortLink shortLink = new ShortLink(
                shortCode,
                originalUrl,
                ownerId,
                now,
                now.plusHours(config.getDefaultTtlHours()),
                config.getDefaultMaxClicks(),
                0,
                true
        );

        linkStorage.put(shortCode, shortLink);

        saveToFile();

        return shortCode;
    }

    public ShortLink getShortLink(String shortCode) {
        return linkStorage.get(shortCode);
    }

    public boolean updateLink(String shortCode, UUID ownerId,
                              Integer newMaxClicks, Integer newTtlHours) {
        ShortLink link = linkStorage.get(shortCode);

        if (link == null) {
            return false;
        }

        if (!link.getOwnerId().equals(ownerId)) {
            return false;
        }

        boolean updated = false;

        if (newMaxClicks != null) {
            if (newMaxClicks <= 0) {
                return false;
            }

            if (newMaxClicks < link.getCurrentClicks()) {
                return false;
            }

            link.setMaxClicks(newMaxClicks);
            updated = true;
        }

        if (newTtlHours != null) {
            if (newTtlHours <= 0) {
                return false;
            }

            link.setExpiresAt(LocalDateTime.now().plusHours(newTtlHours));
            updated = true;
        }

        if (!link.isActive() && link.getCurrentClicks() < link.getMaxClicks()) {
            link.setActive(true);
        }

        if (updated) {
            saveToFile();
        }

        return updated;
    }

    public boolean deleteLink(String shortCode, UUID ownerId) {
        ShortLink link = linkStorage.get(shortCode);

        if (link == null) {
            return false;
        }

        if (!link.getOwnerId().equals(ownerId)) {
            return false;
        }

        linkStorage.remove(shortCode);
        saveToFile();
        return true;
    }

    public Map<String, ShortLink> getAllLinks() {
        return new HashMap<>(linkStorage);
    }

    private String generateShortCode(String originalUrl, UUID ownerId) {
        String input = originalUrl + ownerId.toString() + System.currentTimeMillis();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());

            int codeLength = AppConfig.getInstance().getShortCodeLength();
            String hash = bytesToHex(digest);

            if (codeLength > hash.length()) {
                codeLength = hash.length();
            }

            return hash.substring(0, codeLength);
        } catch (Exception e) {
            return UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, Math.min(AppConfig.getInstance().getShortCodeLength(), 32));
        }
    }
    public void incrementClickCount(String shortCode) {
        ShortLink link = linkStorage.get(shortCode);
        if (link != null) {
            link.setCurrentClicks(link.getCurrentClicks() + 1);
            saveToFile();
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();

    }
}