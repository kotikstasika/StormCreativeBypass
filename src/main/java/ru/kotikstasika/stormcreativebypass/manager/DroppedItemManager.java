package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import org.bukkit.entity.Player;
import ru.kotikstasika.stormcreativebypass.config.ConfigManager;
import ru.kotikstasika.stormcreativebypass.model.CreativeItemInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DroppedItemManager {

    private final ConfigManager configManager;
    private final Map<UUID, CreativeItemInfo> droppedCreativeItems = new ConcurrentHashMap<>();

    public DroppedItemManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void storeDroppedItem(UUID itemId, CreativeItemInfo itemInfo) {
        droppedCreativeItems.put(itemId, itemInfo);
    }

    public CreativeItemInfo getDroppedItem(UUID itemId) {
        return droppedCreativeItems.get(itemId);
    }

    public void removeDroppedItem(UUID itemId) {
        droppedCreativeItems.remove(itemId);
    }

    public List<String> getNearbyPlayersWithDistance(Player player, double radius) {
        List<String> nearbyPlayers = new ArrayList<>();
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (!nearbyPlayer.equals(player)) {
                double distance = nearbyPlayer.getLocation().distance(player.getLocation());
                if (distance <= radius) {
                    nearbyPlayers.add(nearbyPlayer.getName() + " (" + Math.round(distance) + "м)");
                }
            }
        }
        return nearbyPlayers;
    }

    public void cleanupOldItems() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, CreativeItemInfo>> iterator = droppedCreativeItems.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, CreativeItemInfo> entry = iterator.next();
            if (currentTime - entry.getValue().getDropTime() > 300000) {
                iterator.remove();
            }
        }
    }
}

