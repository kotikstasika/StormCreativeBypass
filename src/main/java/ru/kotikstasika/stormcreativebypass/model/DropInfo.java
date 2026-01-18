package ru.kotikstasika.stormcreativebypass.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
@AllArgsConstructor
public class DropInfo {
    private int amount;
    private Location location;
    private String originalOwner;
}

