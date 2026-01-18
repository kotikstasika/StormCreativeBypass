package ru.kotikstasika.stormcreativebypass;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.listeners.impl.*;
import ru.kotikstasika.stormcreativebypass.manager.DroppedItemManager;
import ru.kotikstasika.stormcreativebypass.manager.ItemHistoryManager;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.MessageBatchManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.service.ILogService;
import ru.kotikstasika.stormcreativebypass.service.ITelegramService;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;
import ru.kotikstasika.stormcreativebypass.service.impl.FileLogService;
import ru.kotikstasika.stormcreativebypass.service.impl.TelegramService;

@Getter
public final class StormCreativeBypass extends JavaPlugin {

    @Getter
    private static StormCreativeBypass instance;

    private ConfigManager configManager;
    private PermissionManager permissionManager;
    private ItemManager itemManager;
    private DroppedItemManager droppedItemManager;
    private ItemHistoryManager itemHistoryManager;
    private MessageBatchManager messageBatchManager;
    private ILogService logService;
    private ITelegramService telegramService;
    private AuditService auditService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager();
        configManager.load();

        permissionManager = new PermissionManager(configManager);
        logService = new FileLogService();
        telegramService = new TelegramService(configManager);
        auditService = new AuditService(configManager, logService, telegramService);
        itemManager = new ItemManager(configManager, auditService, permissionManager);
        droppedItemManager = new DroppedItemManager(configManager);
        itemHistoryManager = new ItemHistoryManager();
        messageBatchManager = new MessageBatchManager(telegramService);

        registerListeners();

        startScheduledTasks();
        
        testTelegramConnection();
    }
    
    private void testTelegramConnection() {
        if (configManager.isEnableTelegramAlerts()) {
            getLogger().info("[Telegram] Проверка подключения к Telegram...");
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                boolean success = telegramService.sendTestMessage();
                if (!success) {
                    getLogger().warning("[Telegram] ВНИМАНИЕ: Не удалось отправить тестовое сообщение в Telegram. Проверьте настройки bot-token и chat-id в config.yml");
                }
            });
        } else {
            getLogger().info("[Telegram] Telegram уведомления отключены в конфиге");
        }
    }

    @Override
    public void onDisable() {
        if (itemHistoryManager != null) {
            Bukkit.getOnlinePlayers().forEach(itemHistoryManager::sendCachedItemsOnQuit);
        }
        if (telegramService != null) {
            telegramService.processPendingMessages();
        }
        if (itemHistoryManager != null) {
            itemHistoryManager.clearCache();
        }
        if (messageBatchManager != null) {
            messageBatchManager.processBatches();
            messageBatchManager.clearCache();
        }
    }

    private void registerListeners() {
        new GameModeListener(itemManager, permissionManager, auditService).register();
        new DeathListener(permissionManager, configManager, itemManager, auditService, (FileLogService) logService).register();
        new ItemListener(itemManager, permissionManager, auditService, droppedItemManager, configManager, itemHistoryManager).register();
        new InventoryListener(itemManager, permissionManager, itemHistoryManager, auditService).register();
        new BlockListener(itemManager, permissionManager, auditService).register();
        new CommandListener(permissionManager, configManager, itemManager, auditService).register();
        new PlayerQuitListener(itemHistoryManager).register();
    }

    private void startScheduledTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (telegramService != null) {
                telegramService.processPendingMessages();
            }
        }, 100L, 100L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (telegramService instanceof TelegramService) {
                ((TelegramService) telegramService).resetMessageCounts();
            }
        }, 1200L, 1200L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (droppedItemManager != null) {
                droppedItemManager.cleanupOldItems();
            }
        }, 6000L, 6000L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (itemHistoryManager != null) {
                Bukkit.getOnlinePlayers().forEach(itemHistoryManager::checkPlayerDistance);
            }
        }, 200L, 200L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (messageBatchManager != null) {
                messageBatchManager.processBatches();
            }
        }, 500L, 500L);
    }
}
