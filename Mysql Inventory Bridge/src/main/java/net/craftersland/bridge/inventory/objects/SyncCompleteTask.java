package net.craftersland.bridge.inventory.objects;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.events.PlayerJoin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncCompleteTask extends BukkitRunnable {

    private final Main pd;
    private final long startTime;
    private final Player p;
    private boolean inProgress = false;

    public SyncCompleteTask(Main pd, long start, Player player) {
        this.pd = pd;
        this.startTime = start;
        this.p = player;
    }

    @Override
    public void run() {
        if (!inProgress) {
            if (p != null && p.isOnline()) {
                inProgress = true;
                if (pd.getInventoryDataHandler().isSyncComplete(p)) {

                    if (!pd.getConfigHandler().getString("ChatMessages.syncComplete").matches("")) {
                        p.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessages.syncComplete"));
                    }

                    pd.getSoundHandler().sendLevelUpSound(p);
                    this.cancel();
                }

                PlayerJoin.blockedPlayer.remove(p);
            } else {
                //inProgress = false;
                this.cancel();
            }
        }
    }


}
