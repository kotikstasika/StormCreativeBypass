package ru.kotikstasika.stormcreativebypass.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PlayerActionLog {
    private String playerName;
    private List<ActionEntry> actions = new ArrayList<>();
    
    @Getter
    @Setter
    public static class ActionEntry {
        private String actionType;
        private Material material;
        private int amount;
        private String additionalInfo;
        private long timestamp;
        
        public ActionEntry(String actionType, Material material, int amount, String additionalInfo) {
            this.actionType = actionType;
            this.material = material;
            this.amount = amount;
            this.additionalInfo = additionalInfo;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public void addAction(String actionType, Material material, int amount, String additionalInfo) {
        actions.add(new ActionEntry(actionType, material, amount, additionalInfo));
    }
    
    public void clear() {
        actions.clear();
    }
    
    public boolean isEmpty() {
        return actions.isEmpty();
    }
}

