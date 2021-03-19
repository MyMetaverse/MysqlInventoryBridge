package net.craftersland.bridge.inventory.objects;

import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class InventorySyncData {

	private final ItemStack[] backupInv;
	private final ItemStack[] backupAr;
	private Boolean syncComplete;

	public InventorySyncData() {
		this.backupInv = null;
		this.backupAr = null;
		this.syncComplete = false;
	}

	public void setSyncStatus(boolean syncStatus) {
		syncComplete = syncStatus;
	}

	public Boolean getSyncStatus() {
		return syncComplete;
	}

	public ItemStack[] getBackupArmor() {
		return backupAr;
	}

	public ItemStack[] getBackupInventory() {
		return backupInv;
	}

}
