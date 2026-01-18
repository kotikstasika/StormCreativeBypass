package ru.kotikstasika.stormcreativebypass.manager;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.kotikstasika.stormcreativebypass.StormCreativeBypass;
import ru.kotikstasika.stormcreativebypass.model.AuditProfile;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;
import ru.kotikstasika.stormcreativebypass.service.impl.FileLogService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class DeathItemHandler {

    private final AuditProfile profile;
    private final AuditService auditService;
    private final FileLogService fileLogService;
    private final ItemManager itemManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DeathItemHandler(AuditProfile profile, AuditService auditService, FileLogService fileLogService, ItemManager itemManager) {
        this.profile = profile;
        this.auditService = auditService;
        this.fileLogService = fileLogService;
        this.itemManager = itemManager;
    }

    public boolean shouldRemoveCreativeItems() {
        return profile != null && profile.isRemoveCreativeItemsOnDeath();
    }

    public void handleDeath(PlayerDeathEvent event, Player player) {
        if (profile != null && !shouldRemoveCreativeItems()) {
            return;
        }

        List<ItemStack> itemsToRemove = new ArrayList<>();
        List<ItemStack> keptItems = new ArrayList<>();

        for (ItemStack item : event.getDrops()) {
            if (item != null && (itemManager.hasCreativeLore(item) || itemManager.isMonitoredPlayerItem(item))) {
                itemsToRemove.add(item);
                logItemRemoval(player, item);
            } else {
                keptItems.add(item);
            }
        }

        if (!itemsToRemove.isEmpty()) {
            event.getDrops().removeAll(itemsToRemove);
            auditService.logAction(player, "DEATH_ITEM_REMOVAL",
                    "Удалено креативных предметов при смерти: " + itemsToRemove.size() +
                            ". Сохранено обычных предметов: " + keptItems.size(), "death");
            player.sendMessage(ChatColor.RED + "Креативные предметы были удалены при вашей смерти!");
        }
    }

    private void logItemRemoval(Player player, ItemStack item) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[StormCreativeBypass-DEATH] %s | Удален предмет при смерти: %s (x%d)",
                timestamp, item.getType(), item.getAmount());
        StormCreativeBypass.getInstance().getLogger().info(logMessage);
        fileLogService.logToFile(logMessage);
    }
}

