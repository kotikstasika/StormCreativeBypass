package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import org.bukkit.entity.Player;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;

@Getter
public class PermissionManager {

    private static final String ADMIN_PERMISSION = "StormCreativeBypass.admin";
    
    private final ConfigManager configManager;

    public PermissionManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean hasAdminPermission(Player player) {
        return player.hasPermission(ADMIN_PERMISSION);
    }

    public boolean hasBypassPermission(Player player) {
        return hasAdminPermission(player) || player.hasPermission(configManager.getBypassPermission());
    }

    public boolean hasRemoveBypassPermission(Player player) {
        return hasAdminPermission(player) || player.hasPermission(configManager.getRemoveBypassPermission());
    }

    public boolean hasCommandBypassPermission(Player player) {
        return hasAdminPermission(player) || player.hasPermission(configManager.getCommandBypassPermission());
    }

    public boolean hasCreativeItemCommandBypassPermission(Player player) {
        return hasAdminPermission(player) || player.hasPermission(configManager.getCreativeItemCommandBypassPermission());
    }
}

