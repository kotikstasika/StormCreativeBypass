package ru.kotikstasika.stormcreativebypass.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AuditProfile {
    private List<String> players = new ArrayList<>();
    private boolean logGameMode = true;
    private boolean logItemTake = true;
    private boolean logItemDrop = true;
    private boolean logBlockPlace = true;
    private boolean logItemPickup = true;
    private boolean logContainerItems = true;
    private boolean logInteract = true;
    private boolean removeCreativeItemsOnDeath = false;
}


