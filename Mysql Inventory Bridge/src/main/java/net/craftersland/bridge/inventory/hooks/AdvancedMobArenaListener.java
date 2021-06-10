package net.craftersland.bridge.inventory.hooks;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.api.SaveInventoryEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import su.nightexpress.ama.AMA;
import su.nightexpress.ama.arena.api.events.objects.ArenaJoinEvent;

public class AdvancedMobArenaListener implements Listener {

    private final Main main;

    public AdvancedMobArenaListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void joinArenaEvent(ArenaJoinEvent e) {
        Player player = e.getArenaPlayer().getPlayer();
        main.getInventoryDataHandler().savePlayer(player, main.getInventoryDataHandler().getInventory(player), main.getInventoryDataHandler().getArmor(player));
    }

    @EventHandler
    public void inventorySaveEvent(SaveInventoryEvent e) {
        boolean playing = AMA.getInstance().getArenaManager().isPlaying(e.getPlayer());
        if (playing) {
            e.setCancelled(true);
        }
    }

}
