package net.craftersland.bridge.inventory.objects;

import net.craftersland.bridge.inventory.Main;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncCompleteTask extends BukkitRunnable {

    private final Main pd;
    private final Player p;

    public SyncCompleteTask(Main pd, Player player) {
        this.pd = pd;
        this.p = player;
    }

    @Override
    public void run() {
        if (pd.getInventoryDataHandler().isSyncComplete(p)) {

            if (!pd.getConfigHandler().getString("ChatMessages.syncComplete").matches("")) {
                p.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessages.syncComplete"));
            }

            pd.getSoundHandler().sendLevelUpSound(p);
            this.cancel();
        }

    }


}
