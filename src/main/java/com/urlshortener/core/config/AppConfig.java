package com.urlshortener.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
        private static AppConfig instance;

        private final Properties properties;

        private static final String CONFIG_FILE = "application.properties";

        private AppConfig() {
            properties = new Properties();
            loadConfiguration();
        }

        public static synchronized AppConfig getInstance() {
            if (instance == null) {
                instance = new AppConfig();
            }
            return instance;
        }

        private void loadConfiguration() {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (input == null) {
                    System.err.println("Файл конфигурации '" + CONFIG_FILE + "' не найден.");
                    System.err.println("   Используются значения по умолчанию.");
                    setDefaultValues();
                    return;
                }

                properties.load(input);

            } catch (IOException e) {
                System.err.println("Ошибка при загрузке конфигурации: " + e.getMessage());
                System.err.println("   Используются значения по умолчанию.");
                setDefaultValues();
            }
        }

        private void setDefaultValues() {
            properties.setProperty("link.default.ttl.hours", "24");
            properties.setProperty("link.default.max.clicks", "10");
            properties.setProperty("cleanup.interval.minutes", "5");
            properties.setProperty("shortcode.length", "8");
            properties.setProperty("shortlink.domain", "localhost");
            properties.setProperty("url.max.length", "2048");
        }

        public String getProperty(String key) {
            return properties.getProperty(key);
        }

        public int getIntProperty(String key) {
            try {
                return Integer.parseInt(properties.getProperty(key));
            } catch (NumberFormatException e) {
                System.err.println("Неверный формат числа для свойства '" + key + "'. Используется значение по умолчанию.");
                return getDefaultValue(key);
            }
        }

        private int getDefaultValue(String key) {
            switch (key) {
                case "link.default.ttl.hours": return 24;
                case "link.default.max.clicks": return 10;
                case "cleanup.interval.minutes": return 5;
                case "shortcode.length": return 8;
                case "url.max.length": return 2048;
                default: return 0;
            }
        }

        public int getDefaultTtlHours() {
            return getIntProperty("link.default.ttl.hours");
        }

        public int getDefaultMaxClicks() {
            return getIntProperty("link.default.max.clicks");
        }

        public int getCleanupIntervalMinutes() {
            return getIntProperty("cleanup.interval.minutes");
        }

        public int getShortCodeLength() {
            return getIntProperty("shortcode.length");
        }

        public String getShortLinkDomain() {
            return getProperty("shortlink.domain");
        }

        public int getUrlMaxLength() {
            return getIntProperty("url.max.length");
        }

        public void printAllSettings() {
            System.out.println("\n=== Текущая конфигурация ===");
            properties.forEach((key, value) -> {
                System.out.println(key + " = " + value);
            });
            System.out.println("============================\n");
        }
    }
