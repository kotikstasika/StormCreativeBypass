package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.ItemHistoryManager;

public class PlayerQuitListener extends AbstractListener {

    private final ItemHistoryManager itemHistoryManager;

    public PlayerQuitListener(ItemHistoryManager itemHistoryManager) {
        this.itemHistoryManager = itemHistoryManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        itemHistoryManager.sendCachedItemsOnQuit(player);
    }
}

