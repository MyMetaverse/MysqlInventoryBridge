package net.craftersland.bridge.inventory.events;

import net.craftersland.bridge.inventory.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerQuit implements Listener {
	
	private Main main;
	
	public PlayerQuit(Main main) {
		this.main = main;
	}
	
	@EventHandler
	public void onDisconnect(final PlayerQuitEvent event) {
		if (Main.isDisabling == false) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(main, new Runnable() {

				@Override
				public void run() {
					if (event.getPlayer() != null) {
						Player p = event.getPlayer();
						ItemStack[] inventory = main.getInventoryDataHandler().getInventory(p);
						ItemStack[] armor = main.getInventoryDataHandler().getArmor(p);
						main.getInventoryDataHandler().onDataSaveFunction(p, true, "true", inventory, armor);
					}
				}
				
			}, 2L);
		}
	}

}
