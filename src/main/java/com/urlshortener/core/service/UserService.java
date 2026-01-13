package com.urlshortener.core.service;

import com.urlshortener.core.model.User;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UserService {
    private final Map<UUID, User> userStorage = new HashMap<>();
    private static final String USER_ID_FILE = "current_user.dat";
    private UUID currentUserId;


    public UUID getOrCreateUser() {
        UUID savedUserId = loadCurrentUser();

        if (savedUserId != null) {
            currentUserId = savedUserId;

            if (!userStorage.containsKey(currentUserId)) {
                userStorage.put(currentUserId, new User(currentUserId));
            }
            System.out.println("Загружен существующий пользователь: " + currentUserId);
            return currentUserId;
        }

        return createNewUser();
    }

    public UUID createNewUser() {
        UUID newUserId = UUID.randomUUID();
        User user = new User(newUserId);
        userStorage.put(newUserId, user);
        currentUserId = newUserId;

        saveCurrentUser();
        System.out.println("Создан новый пользователь: " + newUserId);
        return newUserId;
    }

    public void resetUser() {
        try {
            Path path = Paths.get(USER_ID_FILE);
            Files.deleteIfExists(path);
            System.out.println("ID пользователя сброшен");
        } catch (IOException e) {
            System.err.println("Ошибка при сбросе ID: " + e.getMessage());
        }
    }

    public UUID getCurrentUserId() {
        return currentUserId;
    }

    public List<UUID> getAllUsers(ShorteningService shorteningService) {
        Set<UUID> userIds = new HashSet<>();

        if (currentUserId != null) {
            userIds.add(currentUserId);
        }

        shorteningService.getAllLinks().values().stream()
                .map(link -> link.getOwnerId())
                .forEach(userIds::add);

        return new ArrayList<>(userIds);
    }

    public boolean switchUser(UUID userId) {
        currentUserId = userId;
        saveCurrentUser();

        System.out.println("Переключен на пользователя: " + userId);
        return true;
    }

    private void saveCurrentUser() {
        if (currentUserId == null) {
            return;
        }

        try {
            Path path = Paths.get(USER_ID_FILE);
            Files.writeString(path, currentUserId.toString());
        } catch (IOException e) {
            System.err.println("Не удалось сохранить пользователя: " + e.getMessage());
        }
    }

    private UUID loadCurrentUser() {
        try {
            Path path = Paths.get(USER_ID_FILE);
            if (Files.exists(path)) {
                String userIdStr = Files.readString(path).trim();
                return UUID.fromString(userIdStr);
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Не удалось загрузить пользователя: " + e.getMessage());
        }
        return null;
    }

    public User getUserById(UUID userId) {
        return userStorage.get(userId);
    }
}