package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;
import ru.kotikstasika.stormcreativebypass.util.LoreBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class ItemManager {

    private final ConfigManager configManager;
    private final AuditService auditService;
    private final PermissionManager permissionManager;

    public ItemManager(ConfigManager configManager, AuditService auditService, PermissionManager permissionManager) {
        this.configManager = configManager;
        this.auditService = auditService;
        this.permissionManager = permissionManager;
    }

    public void applyCreativeLore(ItemStack item, Player player) {
        applyCreativeLore(item, player, null);
    }
    
    public void applyCreativeLore(ItemStack item, Player player, String lastPlayer) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey ownerKey = new NamespacedKey(StormCreativeBypass.getInstance(), "kotikstasika");
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getName());

        List<String> lore = LoreBuilder.buildLore(configManager.getItemLoreTemplate(), player.getName(), lastPlayer);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public boolean hasCreativeLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey key = new NamespacedKey(StormCreativeBypass.getInstance(), "kotikstasika");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return true;
        }

        if (!meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;

        for (String loreLine : lore) {
            for (String templateLine : configManager.getItemLoreTemplate()) {
                String expectedLine = templateLine.replace(configManager.getCreativePlaceholder(), "Креатив")
                        .replace(configManager.getPlayerPlaceholder(), ".*");
                String regex = ".*" + Pattern.quote(expectedLine.replace("(", "\\(").replace(")", "\\)")) + ".*";
                if (loreLine.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getOriginalOwnerFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "Неизвестно";

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "Неизвестно";

        NamespacedKey key = new NamespacedKey(StormCreativeBypass.getInstance(), "kotikstasika");
        if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }

        return "Неизвестно";
    }

    public boolean isMonitoredPlayerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String owner = getOriginalOwnerFromLore(item);
        return !owner.equals("Неизвестно") && configManager.getMonitoredPlayersSet().contains(owner);
    }

    public void checkContainerItems(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta() || player.getGameMode() != org.bukkit.GameMode.CREATIVE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            BlockState blockState = blockStateMeta.getBlockState();

            if (blockState instanceof Container) {
                Container container = (Container) blockState;
                Inventory inventory = container.getInventory();
                boolean changed = false;

                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack containerItem = inventory.getItem(i);
                    if (containerItem != null && containerItem.getType() != Material.AIR &&
                            !hasCreativeLore(containerItem)) {
                        ItemStack newItem = containerItem.clone();
                        applyCreativeLore(newItem, player, null);
                        inventory.setItem(i, newItem);
                        changed = true;
                    }
                }

                if (changed) {
                    blockStateMeta.setBlockState((BlockState) container);
                    item.setItemMeta(blockStateMeta);
                }
            }
        }
    }

    public void removeForeignCreativeItems(Player player, Inventory inventory) {
        List<ItemStack> itemsToRemove = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR && hasCreativeLore(item)) {
                String originalOwner = getOriginalOwnerFromLore(item);
                if (!originalOwner.equals(player.getName())) {
                    itemsToRemove.add(item);
                    auditService.logAction(player, "FOREIGN_ITEM_REMOVED", "Удален чужой креативный предмет: " +
                            item.getType() + " (x" + item.getAmount() + ") от " + originalOwner, "item-take");
                }
            }
            if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                BlockState blockState = blockStateMeta.getBlockState();
                if (blockState instanceof Container) {
                    Container container = (Container) blockState;
                    Inventory containerInventory = container.getInventory();
                    List<ItemStack> containerItemsToRemove = new ArrayList<>();
                    for (int j = 0; j < containerInventory.getSize(); j++) {
                        ItemStack containerItem = containerInventory.getItem(j);
                        if (containerItem != null && containerItem.getType() != Material.AIR && hasCreativeLore(containerItem)) {
                            String originalOwner = getOriginalOwnerFromLore(containerItem);
                            if (!originalOwner.equals(player.getName())) {
                                containerItemsToRemove.add(containerItem);
                                auditService.logAction(player, "FOREIGN_ITEM_REMOVED", "Удален чужой креативный предмет в контейнере: " +
                                        containerItem.getType() + " (x" + containerItem.getAmount() + ") от " + originalOwner, "container-items");
                            }
                        }
                    }
                    for (ItemStack containerItem : containerItemsToRemove) {
                        containerInventory.remove(containerItem);
                    }
                    if (!containerItemsToRemove.isEmpty()) {
                        blockStateMeta.setBlockState(blockState);
                        item.setItemMeta(blockStateMeta);
                    }
                }
            }
        }
        for (ItemStack item : itemsToRemove) {
            inventory.remove(item);
        }
        if (!itemsToRemove.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Чужие креативные предметы были удалены из вашего инвентаря!");
        }
    }

    public void removeForeignFromContainer(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            BlockState blockState = blockStateMeta.getBlockState();

            if (blockState instanceof Container) {
                Container container = (Container) blockState;
                Inventory inventory = container.getInventory();
                List<ItemStack> toRemove = new ArrayList<>();

                for (ItemStack containerItem : inventory.getContents()) {
                    if (containerItem != null && hasCreativeLore(containerItem)) {
                        String originalOwner = getOriginalOwnerFromLore(containerItem);
                        if (!originalOwner.equals(player.getName())) {
                            toRemove.add(containerItem);
                            auditService.logAction(player, "FOREIGN_ITEM_REMOVED", "Удален чужой креативный предмет из контейнера: " +
                                    containerItem.getType() + " (x" + containerItem.getAmount() + ") от " + originalOwner, "container-items");
                        }
                    }
                }

                for (ItemStack r : toRemove) {
                    inventory.remove(r);
                }

                if (!toRemove.isEmpty()) {
                    blockStateMeta.setBlockState(blockState);
                    item.setItemMeta(blockStateMeta);
                }
            }
        }
    }

    public boolean hasCreativeItemsInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR && hasCreativeLore(item)) {
                return true;
            }
            if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                BlockState blockState = blockStateMeta.getBlockState();
                if (blockState instanceof Container) {
                    Container container = (Container) blockState;
                    for (ItemStack containerItem : container.getInventory().getContents()) {
                        if (containerItem != null && containerItem.getType() != Material.AIR && hasCreativeLore(containerItem)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean containsItem(ItemStack[] inventory, ItemStack item) {
        for (ItemStack invItem : inventory) {
            if (invItem == null || !invItem.isSimilar(item)) continue;
            return true;
        }
        return false;
    }
}

