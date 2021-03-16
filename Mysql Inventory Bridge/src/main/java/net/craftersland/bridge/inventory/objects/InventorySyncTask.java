package net.craftersland.bridge.inventory.objects;

import net.craftersland.bridge.inventory.Main;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class InventorySyncTask extends BukkitRunnable {
	
	private Main pd;
	private long startTime;
	private Player p;
	private boolean inProgress = false;
	private InventorySyncData syncD; 
	
	public InventorySyncTask(Main pd, long start, Player player, InventorySyncData syncData) {
		this.pd = pd;
		this.startTime = start;
		this.p = player;
		this.syncD = syncData;
	}

	@Override
	public void run() {
		if (inProgress == false) {
			if (p != null) {
				if (p.isOnline() == true) {
					inProgress = true;
					DatabaseInventoryData data = pd.getInvMysqlInterface().getData(p);
					if (data.getSyncStatus().matches("true")) {
						pd.getInventoryDataHandler().setPlayerData(p, data, syncD, true);
						inProgress = false;
						this.cancel();
					} else if (System.currentTimeMillis() - Long.parseLong(data.getLastSeen()) >= 600 * 1000) {
						pd.getInventoryDataHandler().setPlayerData(p, data, syncD, true);
						inProgress = false;
						this.cancel();
					} else if (System.currentTimeMillis() - startTime >= 22 * 1000) {
						pd.getInventoryDataHandler().setPlayerData(p, data, syncD, true);
						inProgress = false;
						this.cancel();
					}
					inProgress = false;
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
