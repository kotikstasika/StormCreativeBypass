package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.DeathItemHandler;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.model.AuditProfile;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;
import ru.kotikstasika.stormcreativebypass.service.impl.FileLogService;

public class DeathListener extends AbstractListener {

    private final PermissionManager permissionManager;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final AuditService auditService;
    private final FileLogService fileLogService;

    public DeathListener(PermissionManager permissionManager, ConfigManager configManager,
                         ItemManager itemManager, AuditService auditService, FileLogService fileLogService) {
        this.permissionManager = permissionManager;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.auditService = auditService;
        this.fileLogService = fileLogService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (permissionManager.hasBypassPermission(player)) return;

        if (configManager.isRemoveCreativeItemsOnDeath()) {
            DeathItemHandler globalHandler = new DeathItemHandler(null, auditService, fileLogService, itemManager);
            globalHandler.handleDeath(event, player);
            return;
        }

        DeathItemHandler deathHandler = getDeathItemHandlerForPlayer(player);
        if (deathHandler != null && deathHandler.shouldRemoveCreativeItems()) {
            deathHandler.handleDeath(event, player);
        }
    }

    private DeathItemHandler getDeathItemHandlerForPlayer(Player player) {
        for (AuditProfile profile : configManager.getAuditProfiles().values()) {
            if (profile.getPlayers().contains(player.getName())) {
                return new DeathItemHandler(profile, auditService, fileLogService, itemManager);
            }
        }
        return null;
    }
}

