package net.craftersland.bridge.inventory.objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DatabaseInventoryData {
	
	private final String rawInventory;
	private final String rawArmor;
	private final long lastSeen;
	private final String encode;

}
