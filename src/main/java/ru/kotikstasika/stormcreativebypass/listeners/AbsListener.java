package ru.kotikstasika.stormcreativebypass.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;

public abstract class AbsListener implements Listener {
    public void register() {
        registerListeners(StormCreativeBypass.getInstance());
    }

    protected void registerListeners(StormCreativeBypass plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected boolean inCreative(Player player) {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    protected boolean hasPermssion(Player player) {
        return player.hasPermission("stormcreativebypass.admin");
    }
}
