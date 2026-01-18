package ru.kotikstasika.stormcreativebypass.listeners.impl;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import ru.kotikstasika.stormcreativebypass.listeners.AbstractListener;
import ru.kotikstasika.stormcreativebypass.manager.ItemManager;
import ru.kotikstasika.stormcreativebypass.manager.PermissionManager;
import ru.kotikstasika.stormcreativebypass.service.impl.AuditService;

public class BlockListener extends AbstractListener {

    private final ItemManager itemManager;
    private final PermissionManager permissionManager;
    private final AuditService auditService;

    public BlockListener(ItemManager itemManager, PermissionManager permissionManager, AuditService auditService) {
        this.itemManager = itemManager;
        this.permissionManager = permissionManager;
        this.auditService = auditService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (permissionManager.hasBypassPermission(player)) return;

        ItemStack item = event.getItemInHand();

        if (item != null && item.getType() != org.bukkit.Material.AIR) {
            ItemStack copy = item.clone();

            if (itemManager.hasCreativeLore(copy) && !permissionManager.hasRemoveBypassPermission(player)) {
                String originalOwner = itemManager.getOriginalOwnerFromLore(copy);
                if (!originalOwner.equals(player.getName())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Вы не можете установить чужой креативный предмет!");
                    auditService.logAction(player, "FOREIGN_ITEM_BLOCKED", "Попытка установить чужой креативный предмет: " +
                            copy.getType() + " (x" + copy.getAmount() + ") от " + originalOwner, "block-place");
                    return;
                }
            }

            itemManager.checkContainerItems(copy, player);
            if (!permissionManager.hasRemoveBypassPermission(player)) {
                itemManager.removeForeignFromContainer(copy, player);
            }

            if (!copy.equals(item)) {
                Block block = event.getBlock();
                if (block.getState() instanceof Container) {
                    Container container = (Container) block.getState();
                    BlockStateMeta newMeta = (BlockStateMeta) copy.getItemMeta();
                    if (newMeta != null) {
                        BlockState newState = newMeta.getBlockState();
                        if (newState instanceof Container) {
                            container.getInventory().setContents(((Container) newState).getInventory().getContents());
                            container.update();
                        }
                    }
                }
            }
            
            if (itemManager.hasCreativeLore(copy)) {
                Location location = event.getBlock().getLocation();
                String locationStr = String.format("Мир: %s, X: %.1f, Y: %.1f, Z: %.1f", 
                        location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
                auditService.logItemAction(player, "Положил", copy.getType(), copy.getAmount(), 
                        "Локация: " + locationStr);
            }
        }
    }
}

