package net.craftersland.bridge.inventory.objects;

public class DatabaseInventoryData {
	
	private final String rawInv;
	private final String rawAr;
	private final String syncComplete;
	private final String lastSee;
	
	public DatabaseInventoryData(String rawInventory, String rawArmor, String syncStatus, String lastSeen) {
		this.rawInv = rawInventory;
		this.rawAr = rawArmor;
		this.syncComplete = syncStatus;
		this.lastSee = lastSeen;
	}
	
	public String getLastSeen() {
		return lastSee;
	}
	
	public String getSyncStatus() {
		return syncComplete;
	}
	
	public String getRawArmor() {
		return rawAr;
	}
	
	public String getRawInventory() {
		return rawInv;
	}

}
