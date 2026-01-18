package ru.kotikstasika.stormcreativebypass.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreativeItemInfo {
    private ItemStack itemStack;
    private String dropperName;
    private long dropTime;
    private Location dropLocation;
    private List<String> nearbyPlayers = new ArrayList<>();
}


