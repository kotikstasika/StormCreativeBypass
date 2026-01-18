package ru.kotikstasika.stormcreativebypass.service.impl;

import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.service.ITelegramService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramService implements ITelegramService {

    private final ConfigManager configManager;
    private final HttpClient httpClient;
    private final Map<String, List<String>> pendingMessages = new ConcurrentHashMap<>();
    private final Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> messageCounts = new ConcurrentHashMap<>();

    private final long messageCooldown = 25000;
    private final int maxMessagesPerMinute = 20;
    private long lastResetTime = System.currentTimeMillis();
    private long lastBatchSendTime = System.currentTimeMillis();

    public TelegramService(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void addToPendingMessages(String message) {
        String minuteKey = String.valueOf(System.currentTimeMillis() / 60000);
        messageCounts.put(minuteKey, messageCounts.getOrDefault(minuteKey, 0) + 1);

        if (messageCounts.get(minuteKey) > maxMessagesPerMinute) {
            StormCreativeBypass.getInstance().getLogger().warning("Превышен лимит сообщений в минуту. Сообщение отложено.");
            return;
        }

        pendingMessages.computeIfAbsent("default", k -> new ArrayList<>()).add(message);
    }

    @Override
    public void processPendingMessages() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastBatchSendTime < messageCooldown) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : pendingMessages.entrySet()) {
            List<String> messages = entry.getValue();
            if (messages.isEmpty()) continue;

            StringBuilder combinedMessage = new StringBuilder();
            Iterator<String> iterator = messages.iterator();

            while (iterator.hasNext()) {
                combinedMessage.append(iterator.next()).append("\n\n");
                iterator.remove();
            }

            if (combinedMessage.length() > 0) {
                sendMessage(combinedMessage.toString());
                lastBatchSendTime = currentTime;
            }
        }
    }

    @Override
    public void sendMessage(String message) {
        if (!configManager.isEnableTelegramAlerts() || configManager.getTelegramBotToken().isEmpty() || configManager.getTelegramChatId().isEmpty()) {
            return;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", configManager.getTelegramBotToken());
            String jsonBody = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"Markdown\"}", configManager.getTelegramChatId(), message.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    StormCreativeBypass.getInstance().getLogger().info("[Telegram] Сообщение успешно отправлено в Telegram");
                } else {
                    StormCreativeBypass.getInstance().getLogger().warning("[Telegram] Ошибка отправки в Telegram (код " + response.statusCode() + "): " + response.body());
                }
            });

        } catch (Exception e) {
            StormCreativeBypass.getInstance().getLogger().warning("[Telegram] Ошибка при отправке уведомления в Telegram: " + e.getMessage());
        }
    }

    public void resetMessageCounts() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 60000) {
            messageCounts.clear();
            lastResetTime = currentTime;
        }
    }

    @Override
    public boolean sendTestMessage() {
        if (!configManager.isEnableTelegramAlerts() || configManager.getTelegramBotToken().isEmpty() || configManager.getTelegramChatId().isEmpty()) {
            return false;
        }

        try {
            String testMessage = "✅ *StormCreativeBypass*\n\nТестовое сообщение. Плагин успешно подключен к Telegram!";
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", configManager.getTelegramBotToken());
            String jsonBody = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"Markdown\"}", 
                    configManager.getTelegramChatId(), testMessage.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                StormCreativeBypass.getInstance().getLogger().info("[Telegram] Тестовое сообщение успешно отправлено в Telegram");
                return true;
            } else {
                StormCreativeBypass.getInstance().getLogger().warning("[Telegram] Не удалось отправить тестовое сообщение в Telegram (код " + response.statusCode() + "): " + response.body());
                return false;
            }

        } catch (Exception e) {
            StormCreativeBypass.getInstance().getLogger().warning("[Telegram] Ошибка при отправке тестового сообщения в Telegram: " + e.getMessage());
            return false;
        }
    }
}


