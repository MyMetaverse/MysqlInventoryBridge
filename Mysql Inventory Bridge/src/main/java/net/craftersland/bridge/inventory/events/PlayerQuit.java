package net.craftersland.bridge.inventory.events;

import net.craftersland.bridge.inventory.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerQuit implements Listener {
	
	private final Main main;
	
	public PlayerQuit(Main main) {
		this.main = main;
	}
	
	@EventHandler
	public void onDisconnect(final PlayerQuitEvent event) {
		if (!Main.isDisabling) {
			final Player p = event.getPlayer();
			Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
				ItemStack[] inventory = main.getInventoryDataHandler().getInventory(p);
				ItemStack[] armor = main.getInventoryDataHandler().getArmor(p);

				// First we need to save the player into redis.
				main.getBridge().cachePlayer(p.getUniqueId(),
						main.getInventoryDataHandler().encodeItems(inventory), // We encode data according to our configuration.
						main.getInventoryDataHandler().encodeItems(armor),
						true
				);

				main.getInventoryDataHandler().onDataSaveFunction(p, true, inventory, armor);
				main.getLogger().info(p.getUniqueId() + " inventory was saved into database.");
			});
		}
	}

}
