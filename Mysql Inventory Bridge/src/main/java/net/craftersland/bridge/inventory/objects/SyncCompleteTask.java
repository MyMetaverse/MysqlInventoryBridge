package net.craftersland.bridge.inventory.objects;

import net.craftersland.bridge.inventory.Main;
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
			if (p != null) {
				if (p.isOnline()) {
					inProgress = true;
					if (pd.getInventoryDataHandler().isSyncComplete(p)) {
						if (!pd.getConfigHandler().getString("ChatMessages.syncComplete").matches("")) {
							p.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessages.syncComplete"));
						}
						pd.getSoundHandler().sendLevelUpSound(p);
						this.cancel();
					} else {
						if (System.currentTimeMillis() - startTime >= 20 * 1000) {
							//Set sync to true in database to force sync data after 20 sec
							pd.getInvMysqlInterface().setSyncStatus(p, "true");
						} else if (System.currentTimeMillis() - startTime >= 40 * 1000) {
							//Stop task after 40 sec
							this.cancel();
						}
						
					}
				} else {
					//inProgress = false;
					this.cancel();
				}
			} else {
				//inProgress = false;
				this.cancel();
			}
		}
	}
	
	

}
