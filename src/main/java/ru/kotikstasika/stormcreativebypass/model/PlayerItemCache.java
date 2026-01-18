package ru.kotikstasika.stormcreativebypass.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PlayerItemCache {
    private String playerName;
    private Location lastLocation;
    private Map<Material, Integer> itemsTaken = new HashMap<>();
    private Map<Material, List<DropInfo>> itemsDropped = new HashMap<>();
    private Map<Material, Integer> itemsGiven = new HashMap<>();
    private Map<Material, Location> itemsTakenLocations = new HashMap<>();
    private long lastActivityTime;
    
    public void addItemTaken(Material material, int amount, Location location) {
        itemsTaken.put(material, itemsTaken.getOrDefault(material, 0) + amount);
        if (location != null) {
            itemsTakenLocations.put(material, location);
        }
        lastActivityTime = System.currentTimeMillis();
    }
    
    public void addItemDropped(Material material, int amount, Location location, String originalOwner) {
        itemsDropped.computeIfAbsent(material, k -> new ArrayList<>())
                .add(new DropInfo(amount, location, originalOwner));
        lastActivityTime = System.currentTimeMillis();
    }
    
    public void addItemGiven(Material material, int amount, String toPlayer) {
        itemsGiven.put(material, itemsGiven.getOrDefault(material, 0) + amount);
        lastActivityTime = System.currentTimeMillis();
    }
    
    public void clear() {
        itemsTaken.clear();
        itemsDropped.clear();
        itemsGiven.clear();
        itemsTakenLocations.clear();
    }
    
    public boolean hasDroppedItems() {
        return !itemsDropped.isEmpty();
    }
    
    public boolean hasTakenItems() {
        return !itemsTaken.isEmpty();
    }
}

