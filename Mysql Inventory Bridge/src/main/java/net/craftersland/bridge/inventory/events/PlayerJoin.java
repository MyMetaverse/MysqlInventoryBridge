package net.craftersland.bridge.inventory.events;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.objects.SyncCompleteTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {


	private final Main main;
	
	public PlayerJoin(Main main) {
		this.main = main;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerLoginEvent(AsyncPlayerPreLoginEvent e) {
		if (!Main.isDisabling) {
			boolean result = main.getInventoryDataHandler().preLoadPlayer(e.getUniqueId());
			if (!result) {
				e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "We couldn't load your data.");
			}
		} else e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Server closed..");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerJoinEvent(final PlayerJoinEvent event) {
		if (!Main.isDisabling) {
			final Player p = event.getPlayer();

			Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
				if (p.isOnline()) {
					main.getInventoryDataHandler().onJoinFunction(p);
					new SyncCompleteTask(main, p).runTaskAsynchronously(main);
				}
			}, 5L);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerPickUpItems(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof  Player && !main.getInventoryDataHandler().isSyncComplete((Player) event.getEntity())) {
			event.setCancelled(true);
		}
	}

}
