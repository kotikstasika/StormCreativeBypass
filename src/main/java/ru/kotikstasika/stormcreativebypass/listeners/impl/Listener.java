package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.kotikstasika.stormcreativebypass.listeners.AbsListener;

import java.util.ArrayList;
import java.util.List;

public class Listener extends AbsListener {

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (inCreative(player)) {
            if (!hasPermssion(player)) {
                ItemStack item = event.getCursor();
                if (item != null && item.getType() != Material.AIR) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.getLore();

                        if (lore == null) {
                            lore = new ArrayList<>();
                        }
                        if (lore.stream().anyMatch(line -> line.equals(ChatColor.translateAlternateColorCodes('&', "&7Описание:")))) {
                            return;
                        }
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Описание:"));
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&f &b&l— &fДанный &6предмет &fвзят из гм"));
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&f &b&l— &fВладелец данного предмета &6" + player.getName()));
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Плагин разработан с любовью by kotikstasika "));

                        meta.setLore(lore);
                        item.setItemMeta(meta);

                        event.setCursor(item);
                    }
                }
            }
        }
    }
}
