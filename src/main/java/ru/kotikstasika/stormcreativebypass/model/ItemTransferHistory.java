package ru.kotikstasika.stormcreativebypass.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ItemTransferHistory {
    private String originalOwner;
    private List<String> transferHistory = new ArrayList<>();
    private long lastTransferTime;
    private Location lastTransferLocation;
    
    public void addTransfer(String playerName) {
        if (!transferHistory.contains(playerName)) {
            transferHistory.add(playerName);
        }
        lastTransferTime = System.currentTimeMillis();
    }
    
    public String getLastTransferPlayer() {
        return transferHistory.isEmpty() ? originalOwner : transferHistory.get(transferHistory.size() - 1);
    }
}

