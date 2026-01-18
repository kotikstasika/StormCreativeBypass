package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;

public class CommandListener extends AbstractListener {

    private final PermissionManager permissionManager;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final AuditService auditService;

    public CommandListener(PermissionManager permissionManager, ConfigManager configManager,
                          ItemManager itemManager, AuditService auditService) {
        this.permissionManager = permissionManager;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.auditService = auditService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (permissionManager.hasBypassPermission(player)) return;

        String cmd = event.getMessage().toLowerCase();

        if (player.getGameMode() == GameMode.CREATIVE && !permissionManager.hasCommandBypassPermission(player)) {
            for (String blocked : configManager.getBlockingCommands()) {
                if (cmd.startsWith(blocked.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(configManager.getBlockingMessage());
                    auditService.logAction(player, "COMMAND_BLOCKED", "Заблокирована команда в креативе: " + cmd, "interact");
                    return;
                }
            }
        }

        if (!permissionManager.hasCreativeItemCommandBypassPermission(player) && itemManager.hasCreativeItemsInInventory(player)) {
            for (String blocked : configManager.getCreativeItemBlockingCommands()) {
                if (cmd.startsWith(blocked.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(configManager.getCreativeItemBlockingMessage());
                    auditService.logAction(player, "CREATIVE_ITEM_COMMAND_BLOCKED", "Заблокирована команда из-за креативных предметов: " + cmd, "interact");
                    return;
                }
            }
        }
    }
}

