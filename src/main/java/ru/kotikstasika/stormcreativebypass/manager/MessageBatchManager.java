package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import ru.kotikstasika.stormcreativebypass.model.PlayerActionLog;
import ru.kotikstasika.stormcreativebypass.service.ITelegramService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MessageBatchManager {

    private final Map<UUID, PlayerActionLog> playerLogs = new ConcurrentHashMap<>();
    private final ITelegramService telegramService;
    private static final long BATCH_INTERVAL = 25000;

    public MessageBatchManager(ITelegramService telegramService) {
        this.telegramService = telegramService;
    }

    public void addAction(UUID playerId, String playerName, String actionType, org.bukkit.Material material, int amount, String additionalInfo) {
        PlayerActionLog log = playerLogs.computeIfAbsent(playerId, k -> {
            PlayerActionLog newLog = new PlayerActionLog();
            newLog.setPlayerName(playerName);
            return newLog;
        });
        log.addAction(actionType, material, amount, additionalInfo);
    }

    public void processBatches() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, PlayerActionLog> entry : playerLogs.entrySet()) {
            PlayerActionLog log = entry.getValue();
            if (log.isEmpty()) continue;

            boolean shouldSend = false;
            if (!log.getActions().isEmpty()) {
                PlayerActionLog.ActionEntry lastAction = log.getActions().get(log.getActions().size() - 1);
                if (currentTime - lastAction.getTimestamp() >= BATCH_INTERVAL) {
                    shouldSend = true;
                }
            }

            if (shouldSend) {
                sendBatchMessage(log);
                log.clear();
            }
        }
    }

    private void sendBatchMessage(PlayerActionLog log) {
        if (log.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        message.append("📦 *Действия игрока:*\n");
        message.append("┌─────────────────────────────\n");
        message.append("│ *Игрок:* `").append(escapeMarkdown(log.getPlayerName())).append("`\n");
        message.append("│ *Действия:*\n");

        for (PlayerActionLog.ActionEntry action : log.getActions()) {
            message.append("│ • ").append(escapeMarkdown(action.getActionType()))
                    .append(": `").append(escapeMarkdown(action.getMaterial().toString()))
                    .append("` x").append(action.getAmount());
            if (action.getAdditionalInfo() != null && !action.getAdditionalInfo().isEmpty()) {
                message.append(" (").append(escapeMarkdown(action.getAdditionalInfo())).append(")");
            }
            message.append("\n");
        }

        message.append("└─────────────────────────────");

        telegramService.addToPendingMessages(message.toString());
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("`", "\\`");
    }

    public void clearCache() {
        playerLogs.clear();
    }
}

