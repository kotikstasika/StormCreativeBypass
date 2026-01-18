package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.DroppedItemManager;
import ru.kotikstasika.stormcreativebypass.manager.ItemHistoryManager;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.model.CreativeItemInfo;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemListener extends AbstractListener {

    private final ItemManager itemManager;
    private final PermissionManager permissionManager;
    private final AuditService auditService;
    private final DroppedItemManager droppedItemManager;
    private final ConfigManager configManager;
    private final ItemHistoryManager itemHistoryManager;

    public ItemListener(ItemManager itemManager, PermissionManager permissionManager,
                       AuditService auditService, DroppedItemManager droppedItemManager,
                       ConfigManager configManager, ItemHistoryManager itemHistoryManager) {
        this.itemManager = itemManager;
        this.permissionManager = permissionManager;
        this.auditService = auditService;
        this.droppedItemManager = droppedItemManager;
        this.configManager = configManager;
        this.itemHistoryManager = itemHistoryManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (permissionManager.hasBypassPermission(player) || player.getGameMode() != GameMode.CREATIVE) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        handleEvent(item, player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (permissionManager.hasBypassPermission(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();

        if (player.getGameMode() == GameMode.CREATIVE) {
            handleEvent(item, player);
            if (!permissionManager.hasRemoveBypassPermission(player)) {
                itemManager.removeForeignFromContainer(item, player);
            }
        }

        Location dropLocation = event.getItemDrop().getLocation();
        itemHistoryManager.handleItemDrop(item, player, dropLocation);

        boolean isCreativeItem = itemManager.hasCreativeLore(item);
        boolean isMonitoredItem = itemManager.isMonitoredPlayerItem(item);

        if (isCreativeItem || isMonitoredItem) {
            CreativeItemInfo itemInfo = new CreativeItemInfo();
            itemInfo.setItemStack(item.clone());
            itemInfo.setDropperName(player.getName());
            itemInfo.setDropTime(System.currentTimeMillis());
            itemInfo.setDropLocation(dropLocation);

            if (configManager.isEnableNearbyPlayersLogging()) {
                List<String> nearbyPlayers = droppedItemManager.getNearbyPlayersWithDistance(player, configManager.getNearbyRadius());
                itemInfo.setNearbyPlayers(nearbyPlayers);
            }

            droppedItemManager.storeDroppedItem(event.getItemDrop().getUniqueId(), itemInfo);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();


        if (player.getGameMode() == GameMode.CREATIVE && itemManager.hasCreativeLore(item)) {
            String originalOwner = itemManager.getOriginalOwnerFromLore(item);
            if (!originalOwner.equals(player.getName())) {
                itemHistoryManager.handleItemPickup(item, player);
                Bukkit.getScheduler().runTask(ru.kotikstasika.stormcreativebypass.StormCreativeBypass.getInstance(), () -> {
                    updateCreativeItemsInInventory(player);
                });
            } else {
                itemHistoryManager.handleItemPickup(item, player);
            }
        } else {
            itemHistoryManager.handleItemPickup(item, player);
        }
        
        boolean isCreativeItem = itemManager.hasCreativeLore(item);
        boolean isMonitoredItem = itemManager.isMonitoredPlayerItem(item);

        if (isCreativeItem || isMonitoredItem) {
            UUID itemId = event.getItem().getUniqueId();
            CreativeItemInfo itemInfo = droppedItemManager.getDroppedItem(itemId);
            String originalOwner = itemManager.getOriginalOwnerFromLore(item);
            String fromPlayer = itemInfo != null ? itemInfo.getDropperName() : originalOwner;

            auditService.logItemAction(player, "Подобрал", item.getType(), item.getAmount(), 
                    "От игрока: " + fromPlayer);
            
            if (itemInfo != null) {
                droppedItemManager.removeDroppedItem(itemId);
            }
        }
    }
    
    private void updateCreativeItemsInInventory(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) return;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.getType() != Material.AIR && itemManager.hasCreativeLore(invItem)) {
                String originalOwner = itemManager.getOriginalOwnerFromLore(invItem);
                if (!originalOwner.equals(player.getName())) {
                    itemHistoryManager.updateItemLoreInInventory(invItem, player);
                    player.getInventory().setItem(i, invItem);
                }
            }
        }
    }

    private void handleEvent(ItemStack item, Player player) {
        if (item != null &&
                player.getGameMode() == GameMode.CREATIVE &&
                item.getType() != Material.AIR &&
                !itemManager.hasCreativeLore(item)) {
            itemManager.applyCreativeLore(item, player);
            itemManager.checkContainerItems(item, player);
        }
    }
}

