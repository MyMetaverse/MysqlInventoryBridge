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

import java.util.HashSet;
import java.util.Set;

public class PlayerJoin implements Listener {

	public static Set<Player> blockedPlayer = new HashSet<>();

	private final Main main;
	
	public PlayerJoin(Main main) {
		this.main = main;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerLoginEvent(AsyncPlayerPreLoginEvent e) {
		if (!Main.isDisabling) {
			main.getInventoryDataHandler().preLoadPlayer(e.getUniqueId());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerJoinEvent(final PlayerJoinEvent event) {
		if (!Main.isDisabling) {
			final Player p = event.getPlayer();
			blockedPlayer.add(p);

			Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
				if (p.isOnline()) {
					main.getInventoryDataHandler().onJoinFunction(p);
					new SyncCompleteTask(main, System.currentTimeMillis(), p).runTaskTimerAsynchronously(main, 5L, 20L);
				}
			}, 5L);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerPickUpItems(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof  Player && blockedPlayer.contains((Player) event.getEntity())) {
			event.setCancelled(true);
		}
	}

}
