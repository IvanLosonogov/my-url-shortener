package com.urlshortener.cli;

import com.urlshortener.core.config.AppConfig;
import com.urlshortener.core.model.ShortLink;
import com.urlshortener.core.service.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleApplication {
    private final UserService userService = new UserService();
    private final ShorteningService shorteningService = new ShorteningService();
    private final RedirectService redirectService = new RedirectService(shorteningService);
    private final StatisticService statisticService = new StatisticService(shorteningService);
    private final LinkLifecycleService lifecycleService = new LinkLifecycleService(shorteningService);
    private UUID currentUserId;

    public void run() {
        lifecycleService.start();

        // Останавливаем службу при завершении
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lifecycleService.stop();
        }));

        Scanner scanner = new Scanner(System.in);
        currentUserId = userService.getOrCreateUser();

        System.out.println("=== Сервис сокращения ссылок ===");
        System.out.println("Ваш ID: " + currentUserId);
        printHelp();

        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine().trim();

            if (commandLine.isEmpty()) continue;

            String[] parts = commandLine.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "create":
                    handleCreateCommand(parts);
                    break;

                case "go":
                    handleGoCommand(parts);
                    break;

                case "stats":
                    handleStatsCommand();
                    break;

                case "info":
                    handleInfoCommand(parts);
                    break;

                case "status":
                    handleStatusCommand(parts);
                    break;

                case "help":
                case "?":
                    printHelp();
                    break;

                case "edit":
                    handleEditCommand(parts);
                    break;

                case "users":
                    handleUsersCommand();
                    break;

                case "switch":
                    handleSwitchCommand(parts);
                    break;

                case "newuser":
                    handleNewUserCommand();
                    break;

                case "reset-user":
                    handleResetUserCommand();
                    break;

                case "delete":
                    handleDeleteCommand(parts);
                    break;

                case "config":
                    handleConfigCommand();
                    break;

                case "exit":
                    System.out.println("До свидания!");
                    scanner.close();
                    return;

                default:
                    System.out.println("Неизвестная команда. Введите 'help' для справки.");
            }
        }
    }

    private void handleCreateCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: укажите URL. Пример: create https://example.com");
            return;
        }

        String url = parts[1];
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            System.out.println("Ошибка: URL должен начинаться с http:// или https://");
            return;
        }

        if (url.length() > AppConfig.getInstance().getUrlMaxLength()) {
            System.out.println("Ошибка: URL слишком длинный. Максимальная длина: " +
                    AppConfig.getInstance().getUrlMaxLength() + " символов");
            return;
        }

        try {
            String shortCode = shorteningService.createShortLink(url, currentUserId);
            System.out.println("Короткая ссылка создана!");
            System.out.println("Код: " + shortCode);

            String domain = AppConfig.getInstance().getShortLinkDomain();
            System.out.println("Полная ссылка: http://" + domain + "/" + shortCode);

            // Показываем информацию о новой ссылке
            System.out.println("\n" + lifecycleService.checkLinkStatus(shortCode));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("Ошибка при создании ссылки: " + e.getMessage());
        }
    }

    private void handleGoCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: укажите код ссылки. Пример: go abc123");
            return;
        }

        String shortCode = parts[1].trim();
        String result = redirectService.redirect(shortCode);
        System.out.println(result);
    }

    private void handleStatsCommand() {
        System.out.println("Ваши ссылки:");
        List<ShortLink> userLinks = statisticService.getUserLinks(currentUserId);

        if (userLinks.isEmpty()) {
            System.out.println("  У вас пока нет созданных ссылок");
            return;
        }

        for (ShortLink link : userLinks) {
            String statusIcon = link.isActive() ? "yes" : "no";
            String shortUrl = link.getOriginalUrl();
            if (shortUrl.length() > 40) {
                shortUrl = shortUrl.substring(0, 37) + "...";
            }

            System.out.printf("  %s %s -> %s%n", statusIcon, link.getShortCode(), shortUrl);
            System.out.printf("     Переходы: %d/%d, Действует до: %s%n%n",
                    link.getCurrentClicks(),
                    link.getMaxClicks(),
                    link.getExpiresAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }
    }

    private void handleResetUserCommand() {
        System.out.print(" ! Вы уверены, что хотите сбросить текущего пользователя? (y/N): ");
        Scanner tempScanner = new Scanner(System.in);
        String confirmation = tempScanner.nextLine().trim().toLowerCase();

        if (confirmation.equals("y") || confirmation.equals("yes")) {
            userService.resetUser();
            currentUserId = userService.getOrCreateUser(); // Создаст нового
            System.out.println("Пользователь сброшен");
            System.out.println("Новый пользователь: " + currentUserId);
        } else {
            System.out.println("Сброс отменен");
        }
    }
    private void handleInfoCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: укажите код ссылки. Пример: info abc123");
            return;
        }

        String shortCode = parts[1].trim();

        String linkInfo = statisticService.getLinkInfo(shortCode, currentUserId);
        System.out.println(linkInfo);

        String linkStatus = lifecycleService.checkLinkStatus(shortCode);
        System.out.println("\n Текущий статус:");
        System.out.println(linkStatus);
    }

    private void handleStatusCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: укажите код ссылки. Пример: status abc123");
            return;
        }

        String shortCode = parts[1].trim();
        String status = lifecycleService.checkLinkStatus(shortCode);
        System.out.println(status);
    }
    private void handleConfigCommand() {
        AppConfig config = AppConfig.getInstance();

        System.out.println("\n Текущая конфигурация:");
        System.out.println("================================");
        System.out.printf("Домен для ссылок: %s\n", config.getShortLinkDomain());
        System.out.printf("Длина кода: %d символов\n", config.getShortCodeLength());
        System.out.printf("Время жизни ссылки: %d часов\n", config.getDefaultTtlHours());
        System.out.printf("Лимит переходов: %d\n", config.getDefaultMaxClicks());
        System.out.printf("Интервал очистки: %d минут\n", config.getCleanupIntervalMinutes());
        System.out.printf("Макс. длина URL: %d символов\n", config.getUrlMaxLength());
        System.out.println("================================\n");
    }

    private void handleEditCommand(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Неверный формат команды.");
            System.out.println("Используйте: edit <код> limit <N>  или  edit <код> ttl <N>");
            System.out.println("Примеры:");
            System.out.println("  edit abc123 limit 50");
            System.out.println("  edit abc123 ttl 72");
            return;
        }

        String shortCode = parts[1].trim();
        String editType = parts[2].toLowerCase().trim();
        String valueStr = parts[3].trim();

        try {
            Integer newMaxClicks = null;
            Integer newTtlHours = null;

            if (editType.equals("limit")) {
                newMaxClicks = Integer.parseInt(valueStr);
            } else if (editType.equals("ttl")) {
                newTtlHours = Integer.parseInt(valueStr);
            } else {
                System.out.println("Неизвестный тип редактирования: '" + editType + "'");
                System.out.println("Доступно: 'limit' или 'ttl'");
                return;
            }

            System.out.println("Обновление ссылки: " + shortCode);
            boolean success = shorteningService.updateLink(shortCode, currentUserId,
                    newMaxClicks, newTtlHours);

            if (success) {
                System.out.println("Параметры ссылки обновлены!");
                System.out.println(statisticService.getLinkInfo(shortCode, currentUserId));
            }

        } catch (NumberFormatException e) {
            System.out.println("Неверный формат числа: '" + valueStr + "'");
        }
    }

    private void handleUsersCommand() {
        System.out.println("👥 Пользователи в системе:");
        List<UUID> allUsers = userService.getAllUsers(shorteningService);

        if (allUsers.isEmpty()) {
            System.out.println("  Нет пользователей");
            return;
        }

        for (UUID userId : allUsers) {
            String currentMarker = userId.equals(currentUserId) ? " ← текущий" : "";

            long linkCount = statisticService.getUserLinks(userId).size();

            System.out.printf("  %s (ссылок: %d)%s%n",
                    userId, linkCount, currentMarker);
        }
    }

    private void handleSwitchCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Укажите ID пользователя.");
            System.out.println("Используйте: switch <UUID>");
            System.out.println("Пример: switch 123e4567-e89b-12d3-a456-426614174000");
            System.out.println("Список пользователей: users");
            return;
        }

        try {
            UUID targetUserId = UUID.fromString(parts[1].trim());
            boolean success = userService.switchUser(targetUserId);

            if (success) {
                currentUserId = targetUserId;
                System.out.println("Переключение успешно!");
                System.out.println("Текущий пользователь: " + currentUserId);
            } else {
                System.out.println("Не удалось переключиться");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Неверный формат UUID");
            System.out.println("Пример: 123e4567-e89b-12d3-a456-426614174000");
        }
    }

    private void handleNewUserCommand() {
        UUID newUserId = userService.createNewUser();
        currentUserId = newUserId;
        System.out.println("Создан новый пользователь: " + newUserId);
        System.out.println("Автоматически переключен на него");
    }

    private void handleDeleteCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Укажите код ссылки для удаления.");
            System.out.println("Используйте: delete <код>");
            System.out.println("Пример: delete abc123");
            return;
        }

        String shortCode = parts[1].trim();

        System.out.print("Вы уверены, что хотите удалить ссылку '" + shortCode + "'? (y/N): ");
        Scanner tempScanner = new Scanner(System.in);
        String confirmation = tempScanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("y") && !confirmation.equals("yes")) {
            System.out.println("Удаление отменено");
            return;
        }

        boolean success = shorteningService.deleteLink(shortCode, currentUserId);

        if (success) {
            System.out.println("Ссылка успешно удалена");
        }
    }

    private void printHelp() {
        System.out.println("\nДоступные команды:");
        System.out.println("  create <URL>              - создать короткую ссылку");
        System.out.println("  go <код>                  - перейти по короткой ссылке");
        System.out.println("  stats                     - показать все мои ссылки");
        System.out.println("  info <код>                - подробная информация о ссылке");
        System.out.println("  status <код>              - проверить статус ссылки");
        System.out.println("  edit <код> limit <N>      - изменить лимит переходов на N");
        System.out.println("  edit <код> ttl <N>        - изменить время жизни на N часов");
        System.out.println("  delete <код>              - удалить ссылку");
        System.out.println("  users                     - список всех пользователей");
        System.out.println("  switch <UUID>             - переключиться на пользователя");
        System.out.println("  newuser                   - создать нового пользователя");
        System.out.println("  reset-user                - сбросить ID (для тестирования)");
        System.out.println("  config                    - показать текущую конфигурацию");
        System.out.println("  help или ?                - справка");
        System.out.println("  exit                      - выход");
        System.out.println("=================================\n");
    }

    public static void main(String[] args) {
        new ConsoleApplication().run();
    }
}