package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.model.DropInfo;
import ru.kotikstasika.stormcreativebypass.model.ItemTransferHistory;
import ru.kotikstasika.stormcreativebypass.model.PlayerItemCache;

import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ItemHistoryManager {

    private final Map<String, ItemTransferHistory> itemHistories = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerItemCache> playerCaches = new ConcurrentHashMap<>();
    private static final NamespacedKey OWNER_KEY = new NamespacedKey(StormCreativeBypass.getInstance(), "kotikstasika");
    private static final NamespacedKey HISTORY_KEY = new NamespacedKey(StormCreativeBypass.getInstance(), "transfer_history");
    private static final double DISTANCE_THRESHOLD = 100.0;

    public void handleItemPickup(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String originalOwner = container.get(OWNER_KEY, PersistentDataType.STRING);

        if (originalOwner == null) return;

        List<String> transferHistory = loadHistoryFromNBT(item);
        if (transferHistory == null) {
            transferHistory = new ArrayList<>();
        }

        if (originalOwner.equals(player.getName())) {
            if (!transferHistory.isEmpty()) {
                transferHistory.add(player.getName());
            }
            removeCreativeLore(item, player);
        } else {
            transferHistory.add(player.getName());
            String lastPlayer = transferHistory.isEmpty() ? originalOwner : transferHistory.get(transferHistory.size() - 1);
            updateItemLoreWithHistory(item, originalOwner, lastPlayer);
            saveHistoryToNBT(item, transferHistory);
        }

        PlayerItemCache cache = playerCaches.computeIfAbsent(player.getUniqueId(), k -> new PlayerItemCache());
        cache.setPlayerName(player.getName());
        cache.setLastLocation(player.getLocation());
        cache.addItemTaken(item.getType(), item.getAmount(), player.getLocation());
    }

    public void handleItemDrop(ItemStack item, Player player, Location dropLocation) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String originalOwner = container.get(OWNER_KEY, PersistentDataType.STRING);
        if (originalOwner == null) {
            originalOwner = "Неизвестно";
        }

        PlayerItemCache cache = playerCaches.computeIfAbsent(player.getUniqueId(), k -> new PlayerItemCache());
        cache.setPlayerName(player.getName());
        cache.setLastLocation(player.getLocation());
        cache.addItemDropped(item.getType(), item.getAmount(), dropLocation != null ? dropLocation : player.getLocation(), originalOwner);
    }

    public void handleItemGive(ItemStack item, Player from, Player to) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String originalOwner = container.get(OWNER_KEY, PersistentDataType.STRING);

        if (originalOwner == null) return;

        List<String> transferHistory = loadHistoryFromNBT(item);
        if (transferHistory == null) {
            transferHistory = new ArrayList<>();
        }

        transferHistory.add(to.getName());
        String lastPlayer = transferHistory.get(transferHistory.size() - 1);
        updateItemLoreWithHistory(item, originalOwner, lastPlayer);
        saveHistoryToNBT(item, transferHistory);

        PlayerItemCache fromCache = playerCaches.computeIfAbsent(from.getUniqueId(), k -> new PlayerItemCache());
        fromCache.setPlayerName(from.getName());
        fromCache.setLastLocation(from.getLocation());
        fromCache.addItemGiven(item.getType(), item.getAmount(), to.getName());

        PlayerItemCache toCache = playerCaches.computeIfAbsent(to.getUniqueId(), k -> new PlayerItemCache());
        toCache.setPlayerName(to.getName());
        toCache.setLastLocation(to.getLocation());
        toCache.addItemTaken(item.getType(), item.getAmount(), to.getLocation());
    }
    
    public String getLastPlayerFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        List<String> history = loadHistoryFromNBT(item);
        if (history == null || history.isEmpty()) return null;
        
        return history.get(history.size() - 1);
    }

    public void checkPlayerDistance(Player player) {
        PlayerItemCache cache = playerCaches.get(player.getUniqueId());
        if (cache == null || cache.getLastLocation() == null) {
            return;
        }

        Location currentLocation = player.getLocation();
        if (cache.getLastLocation().getWorld() != currentLocation.getWorld()) {
            if (cache.hasTakenItems() || cache.hasDroppedItems()) {
                sendCachedItemsToTelegram(player, cache);
                cache.clear();
            }
            cache.setLastLocation(currentLocation);
            return;
        }

        double distance = cache.getLastLocation().distance(currentLocation);

        if (distance > DISTANCE_THRESHOLD && (cache.hasTakenItems() || cache.hasDroppedItems())) {
            sendCachedItemsToTelegram(player, cache);
            cache.clear();
        }

        cache.setLastLocation(currentLocation);
    }
    
    public void sendCachedItemsOnQuit(Player player) {
        PlayerItemCache cache = playerCaches.get(player.getUniqueId());
        if (cache != null && (cache.hasTakenItems() || cache.hasDroppedItems())) {
            sendCachedItemsToTelegram(player, cache);
            cache.clear();
        }
    }

    private void sendCachedItemsToTelegram(Player player, PlayerItemCache cache) {
        boolean hasTaken = cache.hasTakenItems();
        boolean hasDropped = cache.hasDroppedItems();
        
        if (!hasTaken && !hasDropped) return;

        StringBuilder message = new StringBuilder();
        
        if (hasDropped) {
            message.append("🔽 *Игрок выбросил предметы:*\n");
            message.append("┌─────────────────────────────\n");
            message.append("│ *Игрок:* `").append(escapeMarkdown(player.getName())).append("`\n");
            message.append("│ *Предметы:*\n");

            for (Map.Entry<Material, List<DropInfo>> entry : cache.getItemsDropped().entrySet()) {
                Material material = entry.getKey();
                List<DropInfo> drops = entry.getValue();
                
                int totalAmount = drops.stream().mapToInt(DropInfo::getAmount).sum();
                DropInfo firstDrop = drops.get(0);
                Location loc = firstDrop.getLocation();
                
                message.append("│ • `").append(escapeMarkdown(material.toString())).append("` x").append(totalAmount);
                
                if (loc != null) {
                    message.append("\n│   📍 *Локация:* `")
                            .append(escapeMarkdown(loc.getWorld().getName())).append("` ")
                            .append("X: `").append(String.format("%.1f", loc.getX())).append("` ")
                            .append("Y: `").append(String.format("%.1f", loc.getY())).append("` ")
                            .append("Z: `").append(String.format("%.1f", loc.getZ())).append("`");
                }
                
                if (firstDrop.getOriginalOwner() != null && !firstDrop.getOriginalOwner().equals("Неизвестно")) {
                    message.append("\n│   👤 *Оригинальный владелец:* `").append(escapeMarkdown(firstDrop.getOriginalOwner())).append("`");
                }
                
                message.append("\n");
            }

            message.append("└─────────────────────────────\n\n");
        }
        
        if (hasTaken) {
            message.append("🔼 *Игрок взял предметы:*\n");
            message.append("┌─────────────────────────────\n");
            message.append("│ *Игрок:* `").append(escapeMarkdown(player.getName())).append("`\n");
            message.append("│ *Предметы:*\n");

            for (Map.Entry<Material, Integer> entry : cache.getItemsTaken().entrySet()) {
                Material material = entry.getKey();
                int amount = entry.getValue();
                Location loc = cache.getItemsTakenLocations().get(material);
                
                message.append("│ • `").append(escapeMarkdown(material.toString())).append("` x").append(amount);
                
                if (loc != null) {
                    message.append("\n│   📍 *Локация:* `")
                            .append(escapeMarkdown(loc.getWorld().getName())).append("` ")
                            .append("X: `").append(String.format("%.1f", loc.getX())).append("` ")
                            .append("Y: `").append(String.format("%.1f", loc.getY())).append("` ")
                            .append("Z: `").append(String.format("%.1f", loc.getZ())).append("`");
                }
                
                message.append("\n");
            }

            message.append("└─────────────────────────────");
        }

        StormCreativeBypass.getInstance().getTelegramService().addToPendingMessages(message.toString());
    }
    
    private String escapeMarkdown(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("`", "\\`");
    }

    private String getItemKey(ItemStack item, String owner) {
        return owner + "_" + item.getType().toString() + "_" + item.hashCode();
    }

    private void removeCreativeLore(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            if (!line.contains("Креатив") && !line.contains("Владелец")) {
                newLore.add(line);
            }
        }

        meta.setLore(newLore.isEmpty() ? null : newLore);
        item.setItemMeta(meta);
    }

    private void updateItemLoreWithHistory(ItemStack item, String originalOwner, String lastPlayer) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        StormCreativeBypass plugin = StormCreativeBypass.getInstance();
        List<String> template = plugin.getConfigManager().getItemLoreTemplate();
        
        List<String> lore = ru.kotikstasika.stormcreativebypass.util.LoreBuilder.buildLore(template, originalOwner, lastPlayer);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void saveHistoryToNBT(ItemStack item, List<String> history) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String historyString = String.join(",", history);
        container.set(HISTORY_KEY, PersistentDataType.STRING, historyString);
        item.setItemMeta(meta);
    }
    
    private List<String> loadHistoryFromNBT(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String historyString = container.get(HISTORY_KEY, PersistentDataType.STRING);
        if (historyString == null || historyString.isEmpty()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(Arrays.asList(historyString.split(",")));
    }

    public void clearCache() {
        itemHistories.clear();
        playerCaches.clear();
    }

    public boolean isCreativePlayer(Player player) {
        return player.getGameMode() == org.bukkit.GameMode.CREATIVE;
    }
    
    public void updateItemLoreInInventory(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String originalOwner = container.get(OWNER_KEY, PersistentDataType.STRING);

        if (originalOwner == null || originalOwner.equals(player.getName())) return;

        List<String> transferHistory = loadHistoryFromNBT(item);
        if (transferHistory == null) {
            transferHistory = new ArrayList<>();
        }

        if (!transferHistory.contains(player.getName())) {
            transferHistory.add(player.getName());
        }
        
        String lastPlayer = transferHistory.isEmpty() ? originalOwner : transferHistory.get(transferHistory.size() - 1);
        updateItemLoreWithHistory(item, originalOwner, lastPlayer);
        saveHistoryToNBT(item, transferHistory);
    }
}

