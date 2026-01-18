package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.ItemHistoryManager;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;

public class InventoryListener extends AbstractListener {

    private final ItemManager itemManager;
    private final PermissionManager permissionManager;
    private final ItemHistoryManager itemHistoryManager;
    private final AuditService auditService;

    public InventoryListener(ItemManager itemManager, PermissionManager permissionManager, 
                            ItemHistoryManager itemHistoryManager, AuditService auditService) {
        this.itemManager = itemManager;
        this.permissionManager = permissionManager;
        this.itemHistoryManager = itemHistoryManager;
        this.auditService = auditService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (permissionManager.hasBypassPermission(player) || player.getGameMode() != GameMode.CREATIVE) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            handleEvent(clickedItem, player);
            if (!itemManager.hasCreativeLore(clickedItem)) {
                String lastPlayer = itemHistoryManager.getLastPlayerFromItem(clickedItem);
                itemManager.applyCreativeLore(clickedItem, player, lastPlayer);
                itemManager.checkContainerItems(clickedItem, player);
            }
        }

        if (cursorItem != null && cursorItem.getType() != Material.AIR && clickedInventory != null) {
            if (clickedInventory.getHolder() instanceof Player) {
                Player targetPlayer = (Player) clickedInventory.getHolder();
                if (targetPlayer != player && itemManager.hasCreativeLore(cursorItem)) {
                    if (targetPlayer.getGameMode() == GameMode.CREATIVE) {
                        itemHistoryManager.handleItemGive(cursorItem, player, targetPlayer);
                        itemHistoryManager.updateItemLoreInInventory(cursorItem, targetPlayer);
                    }
                    auditService.logItemAction(player, "Передал", cursorItem.getType(), cursorItem.getAmount(), 
                            "Игроку: " + targetPlayer.getName());
                }
            }
        }
        
        if (player.getGameMode() == GameMode.CREATIVE) {
            Bukkit.getScheduler().runTask(ru.kotikstasika.stormcreativebypass.StormCreativeBypass.getInstance(), () -> {
                updateCreativeItemsInInventory(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (permissionManager.hasBypassPermission(player) || player.getGameMode() != GameMode.CREATIVE) return;

        for (ItemStack item : event.getNewItems().values()) {
            if (item == null || item.getType() == Material.AIR || itemManager.hasCreativeLore(item)) continue;
            String lastPlayer = itemHistoryManager.getLastPlayerFromItem(item);
            itemManager.applyCreativeLore(item, player, lastPlayer);
            itemManager.checkContainerItems(item, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (permissionManager.hasBypassPermission(player) || player.getGameMode() != GameMode.CREATIVE) return;

        ItemStack item = event.getCursor();
        if (item != null && item.getType() != Material.AIR && !itemManager.hasCreativeLore(item)) {
            String lastPlayer = itemHistoryManager.getLastPlayerFromItem(item);
            itemManager.applyCreativeLore(item, player, lastPlayer);
            itemManager.checkContainerItems(item, player);
            event.setCursor(item);
        }
    }

    private void handleEvent(ItemStack item, Player player) {
        if (item != null &&
                player.getGameMode() == GameMode.CREATIVE &&
                item.getType() != Material.AIR &&
                !itemManager.hasCreativeLore(item)) {
            String lastPlayer = itemHistoryManager.getLastPlayerFromItem(item);
            itemManager.applyCreativeLore(item, player, lastPlayer);
            itemManager.checkContainerItems(item, player);
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
}

