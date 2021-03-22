package net.craftersland.bridge.inventory.jedisbridge;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

public class MinecraftRedisListener implements Listener {

    private final InventoryPusher inventoryPusher;

    public MinecraftRedisListener(InventoryPusher inventoryPusher) {
        this.inventoryPusher = inventoryPusher;
    }

    @EventHandler
    public void onPlayerInventoryEvent(InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.PLAYER) {
            updateInventory((Player) e.getWhoClicked());
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void pickUpAnything(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            updateInventory((Player) e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void dropAnything(PlayerDropItemEvent e) {
        updateInventory(e.getPlayer());
    }


    private void updateInventory(Player player) {
        inventoryPusher.addToQueue(player);
    }

}
