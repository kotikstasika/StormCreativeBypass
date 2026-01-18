package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameModeListener extends AbstractListener {

    private final ItemManager itemManager;
    private final PermissionManager permissionManager;
    private final AuditService auditService;
    private final Map<UUID, ItemStack[]> inventories = new HashMap<>();

    public GameModeListener(ItemManager itemManager, PermissionManager permissionManager, AuditService auditService) {
        this.itemManager = itemManager;
        this.permissionManager = permissionManager;
        this.auditService = auditService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (permissionManager.hasBypassPermission(player)) return;

        UUID playerId = player.getUniqueId();

        if (event.getNewGameMode() == GameMode.CREATIVE) {
            inventories.put(playerId, player.getInventory().getContents());

            if (!permissionManager.hasRemoveBypassPermission(player)) {
                itemManager.removeForeignCreativeItems(player, player.getInventory());
            }

            if (isMonitoredPlayer(player)) {
                auditService.logAction(player, "GAMEMODE_CHANGE", "Вошел в КРЕАТИВНЫЙ режим", "game-mode");
            }
        } else if (event.getNewGameMode() == GameMode.SURVIVAL) {
            if (isMonitoredPlayer(player)) {
                auditService.logAction(player, "GAMEMODE_CHANGE", "Вышел из КРЕАТИВНОГО режима", "game-mode");

                ItemStack[] creativeItems = player.getInventory().getContents();
                for (ItemStack item : creativeItems) {
                    if (item != null && item.getType() != Material.AIR && itemManager.hasCreativeLore(item)) {
                        auditService.logAction(player, "ITEM_TAKE", "Вынес из креатива: " +
                                item.getType() + " (x" + item.getAmount() + ")", "item-take");
                    }
                }
            }

            ItemStack[] savedInventory = inventories.get(playerId);
            if (savedInventory != null) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    if (!itemManager.containsItem(savedInventory, item) && !itemManager.hasCreativeLore(item)) {
                        itemManager.applyCreativeLore(item, player, null);
                    }
                    itemManager.checkContainerItems(item, player);
                }

                inventories.remove(playerId);
            }
            
            if (!permissionManager.hasRemoveBypassPermission(player)) {
                itemManager.removeForeignCreativeItems(player, player.getInventory());
            }
        }
    }

    private boolean isMonitoredPlayer(Player player) {
        return StormCreativeBypass.getInstance().getConfigManager().getMonitoredPlayersSet().contains(player.getName());
    }
}

