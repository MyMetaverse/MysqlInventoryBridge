package net.craftersland.bridge.inventory.events;

import lombok.SneakyThrows;
import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.jedisbridge.BridgeMonitor;
import net.craftersland.bridge.inventory.jedisbridge.BridgeResult;
import net.craftersland.bridge.inventory.objects.SyncCompleteTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class PlayerJoin implements Listener {

    private final Main main;

    public final static Map<UUID, BridgeMonitor> queue = new HashMap<>();

    public PlayerJoin(Main main) {
        this.main = main;
    }

    @SneakyThrows
    @EventHandler
    public void onPlayerPreAsync(AsyncPlayerPreLoginEvent e) {
        // First we need to check for player's inside memory.
        UUID uniqueId = e.getUniqueId();
        BridgeMonitor monitor = new BridgeMonitor();
        queue.put(uniqueId, monitor);

        // We block this thread until we get the information from a message, if we don't after 200ms,
        // we process reading the databases.
        monitor.call();

        BridgeResult bridgeResult = main.getBridge().readPlayer(uniqueId);

        if (bridgeResult.getResult() == BridgeResult.Result.DONE) {
            main.getLogger().info(uniqueId + " loaded from a message.");
            return;
        }

        if (bridgeResult.getResult() == BridgeResult.Result.SAVED) { // If the player is in memory...
            main.getInventoryDataHandler().assignPlayer(uniqueId, bridgeResult); // We load it into the server.
            main.getLogger().info(uniqueId + " loaded from Redis.");
        } else if (bridgeResult.getResult() == BridgeResult.Result.NOT_SAVED) { // If the player is not loaded...
            boolean result = main.getInventoryDataHandler().preLoadPlayer(uniqueId); // We attempt to load it from SQL.
            if (!result) // If we fail to load, we kick the player.
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Failed loading your data, join again.");
            main.getLogger().info(uniqueId + " loaded from SQL.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoinEvent(final PlayerJoinEvent event) {
        if (!Main.isDisabling) {
            final Player player = event.getPlayer();
            // After that we save the player into our database.

            if (player.isOnline()) {
                boolean isSync = main.getInventoryDataHandler().onJoinFunction(player); // We try to sync the player wit cache.
                if (isSync) // If achieved, send message.
                    new SyncCompleteTask(main, player).runTaskAsynchronously(main);
            }

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerPickUpItems(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && !main.getInventoryDataHandler().isSyncComplete((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

}
