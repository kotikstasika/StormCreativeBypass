package ru.kotikstasika.stormcreativebypass;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kotikstasika.stormcreativebypass.listeners.impl.Listener;

public final class StormCreativeBypass extends JavaPlugin {

    @Getter
    private static StormCreativeBypass instance;

    @Override
    public void onEnable() {
        instance = this;
        registerListeners();
    }

    void registerListeners() {
        new Listener().register();
    }
}
