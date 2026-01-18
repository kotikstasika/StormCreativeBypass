package ru.kotikstasika.stormcreativebypass.service.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.manager.MessageBatchManager;
import ru.kotikstasika.stormcreativebypass.model.AuditProfile;
import ru.kotikstasika.stormcreativebypass.service.ILogService;
import ru.kotikstasika.stormcreativebypass.service.ITelegramService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditService {

    private final ConfigManager configManager;
    private final ILogService logService;
    private final ITelegramService telegramService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AuditService(ConfigManager configManager, ILogService logService, ITelegramService telegramService) {
        this.configManager = configManager;
        this.logService = logService;
        this.telegramService = telegramService;
    }

    public void logAction(Player player, String actionType, String details, String logType) {
        if (!configManager.isEnableAuditLog()) {
            return;
        }

        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[StormCreativeBypass-AUDIT] %s | %s | %s: %s",
                timestamp, player.getName(), actionType, details);

        if (configManager.isEnableConsoleLogging()) {
            StormCreativeBypass.getInstance().getLogger().info(logMessage);
        }

        logService.logToFile(logMessage);

        if (configManager.isEnableTelegramAlerts() && shouldLogAction(player, logType)) {
            String telegramMessage = formatTelegramMessage(player.getName(), actionType, details, timestamp);
            telegramService.addToPendingMessages(telegramMessage);
        }
    }
    
    public void logItemAction(Player player, String action, Material material, int amount, String additionalInfo) {
        if (!configManager.isEnableAuditLog()) {
            return;
        }

        String timestamp = dateFormat.format(new Date());
        String details = String.format("%s: %s x%d%s", action, material, amount, 
                additionalInfo != null && !additionalInfo.isEmpty() ? ". " + additionalInfo : "");
        
        String logMessage = String.format("[StormCreativeBypass-AUDIT] %s | %s | ITEM_ACTION: %s",
                timestamp, player.getName(), details);

        if (configManager.isEnableConsoleLogging()) {
            StormCreativeBypass.getInstance().getLogger().info(logMessage);
        }

        logService.logToFile(logMessage);

        if (configManager.isEnableTelegramAlerts()) {
            MessageBatchManager batchManager = StormCreativeBypass.getInstance().getMessageBatchManager();
            if (batchManager != null) {
                batchManager.addAction(player.getUniqueId(), player.getName(), action, material, amount, additionalInfo);
            } else {
                String telegramMessage = formatItemTelegramMessage(player.getName(), action, material, amount, additionalInfo, timestamp);
                telegramService.addToPendingMessages(telegramMessage);
            }
        }
    }

    private boolean shouldLogAction(Player player, String logType) {
        if (!configManager.isEnableAuditLog()) {
            return false;
        }

        if (!isMonitoredPlayer(player)) {
            return false;
        }

        for (AuditProfile profile : configManager.getAuditProfiles().values()) {
            if (profile.getPlayers().contains(player.getName())) {
                switch (logType) {
                    case "game-mode": return profile.isLogGameMode();
                    case "item-take": return profile.isLogItemTake();
                    case "item-drop": return profile.isLogItemDrop();
                    case "block-place": return profile.isLogBlockPlace();
                    case "item-pickup": return profile.isLogItemPickup();
                    case "container-items": return profile.isLogContainerItems();
                    case "interact": return profile.isLogInteract();
                    case "death": return true;
                    default: return true;
                }
            }
        }

        return false;
    }

    private boolean isMonitoredPlayer(Player player) {
        return configManager.getMonitoredPlayersSet().contains(player.getName());
    }

    private String formatTelegramMessage(String playerName, String actionType, String details, String timestamp) {
        String emoji = getEmojiForAction(actionType);

        return String.format("%s *StormCreativeBypass Audit* %s\n" +
                        "┌─────────────────────────────\n" +
                        "│ *Игрок:* `%s`\n" +
                        "│ *Действие:* `%s`\n" +
                        "│ *Время:* `%s`\n" +
                        "├─────────────────────────────\n" +
                        "│ *Детали:*\n" +
                        "│ `%s`\n" +
                        "└─────────────────────────────",
                emoji, emoji,
                escapeMarkdown(playerName),
                escapeMarkdown(actionType),
                escapeMarkdown(timestamp),
                escapeMarkdown(details));
    }
    
    private String formatItemTelegramMessage(String playerName, String action, Material material, int amount, String additionalInfo, String timestamp) {
        String emoji = getEmojiForAction("ITEM_" + action.toUpperCase());
        
        StringBuilder message = new StringBuilder();
        message.append(emoji).append(" *StormCreativeBypass* ").append(emoji).append("\n");
        message.append("┌─────────────────────────────\n");
        message.append("│ *Игрок:* `").append(escapeMarkdown(playerName)).append("`\n");
        message.append("│ *Действие:* `").append(escapeMarkdown(action)).append("`\n");
        message.append("│ *Предмет:* `").append(escapeMarkdown(material.toString())).append(" x").append(amount).append("`\n");
        message.append("│ *Время:* `").append(escapeMarkdown(timestamp)).append("`\n");
        
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            message.append("├─────────────────────────────\n");
            message.append("│ *Дополнительно:*\n");
            message.append("│ `").append(escapeMarkdown(additionalInfo)).append("`\n");
        }
        
        message.append("└─────────────────────────────");
        
        return message.toString();
    }

    private String getEmojiForAction(String actionType) {
        switch (actionType) {
            case "ITEM_PICKUP": return "🔼";
            case "ITEM_DROP": return "🔽";
            case "GAMEMODE_CHANGE": return "🔄";
            case "BLOCK_PLACE": return "🧱";
            case "DEATH_ITEM_REMOVAL": return "💀";
            case "FOREIGN_ITEM_REMOVED": return "🚫";
            case "COMMAND_BLOCKED": return "🚫";
            case "CREATIVE_ITEM_COMMAND_BLOCKED": return "🚫";
            default: return "📦";
        }
    }

    private String escapeMarkdown(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}

