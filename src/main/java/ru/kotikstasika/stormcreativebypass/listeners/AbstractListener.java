package ru.kotikstasika.stormcreativebypass.listeners;

import org.bukkit.event.Listener;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;

public abstract class AbstractListener implements Listener {

    public void register() {
        StormCreativeBypass.getInstance().getServer().getPluginManager().registerEvents(this, StormCreativeBypass.getInstance());
    }
}


