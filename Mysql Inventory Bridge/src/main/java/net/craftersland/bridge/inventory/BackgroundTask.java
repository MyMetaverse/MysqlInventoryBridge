package net.craftersland.bridge.inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BackgroundTask {
	
	private final Main m;
	
	public BackgroundTask(Main m) {
		this.m = m;
		runTask();
	}
	
	private void runTask() {
		if (m.getConfigHandler().getBoolean("General.saveDataTask.enabled")) {
			Main.log.info("Data save task is enabled.");
			Bukkit.getScheduler().runTaskTimerAsynchronously(m, this::runSaveData,
					m.getConfigHandler().getInteger("General.saveDataTask.interval") * 60 * 20L,
					m.getConfigHandler().getInteger("General.saveDataTask.interval") * 60 * 20L);
		} else {
			Main.log.info("Data save task is disabled.");
		}
	}
	
	private void runSaveData() {
		if (m.getConfigHandler().getBoolean("General.saveDataTask.enabled")) {
			if (!Bukkit.getOnlinePlayers().isEmpty()) {
				List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
				if (!m.getConfigHandler().getBoolean("General.saveDataTask.hideLogMessages")) {
					Main.log.info("Saving online players data...");
				}

				m.getInventoryDataHandler().saveMultiplePlayers(onlinePlayers, false);

				if (!m.getConfigHandler().getBoolean("General.saveDataTask.hideLogMessages")) {
					Main.log.info("Data save complete for " + onlinePlayers.size() + " players.");
				}

				onlinePlayers.clear();
			}
		}
	}
	
	public void onShutDownDataSave() {
		Main.log.info("Saving online players data...");
		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

		m.getInventoryDataHandler().saveMultiplePlayers(onlinePlayers, false);

		Main.log.info("Data save complete for " + onlinePlayers.size() + " players.");
	}

}
