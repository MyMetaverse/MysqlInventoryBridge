package net.craftersland.bridge.inventory.events;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.objects.SyncCompleteTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {
	
	private Main main;
	
	public PlayerJoin(Main main) {
		this.main = main;
	}
	
	@EventHandler
	public void onLogin(final PlayerJoinEvent event) {		
		if (Main.isDisabling == false) {
			final Player p = event.getPlayer();
			Bukkit.getScheduler().runTaskLaterAsynchronously(main, new Runnable() {

				@Override
				public void run() {
					if (p != null) {
						if (p.isOnline() == true) {
							main.getInventoryDataHandler().onJoinFunction(p);
							new SyncCompleteTask(main, System.currentTimeMillis(), p).runTaskTimerAsynchronously(main, 5L, 20L);
						}
					}
				}
				
			}, 5L);
		}
	}

}
