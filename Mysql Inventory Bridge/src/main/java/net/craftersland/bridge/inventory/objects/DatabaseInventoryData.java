package net.craftersland.bridge.inventory.objects;

import lombok.Getter;
import net.craftersland.bridge.inventory.encoder.DataRetainer;
import net.craftersland.bridge.inventory.encoder.Encoder;
import net.craftersland.bridge.inventory.encoder.EncoderFactory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

@Getter
public class DatabaseInventoryData implements DataRetainer {
	
	private final String rawInventory;
	private final String rawArmor;
	private final long lastSeen;
	private final String codec;

	private final Encoder encoder;

	public DatabaseInventoryData(String rawInventory, String rawArmor, long lastSeen, String codec) {
		this.rawInventory = rawInventory;
		this.rawArmor = rawArmor;
		this.lastSeen = lastSeen;
		this.codec = codec;

		this.encoder = EncoderFactory.getEncoder(codec);
	}


	@Override
	public ItemStack[] getArmor() throws IOException {
		if(encoder == null)
			throw new NullPointerException("Encoder required not found.");
		return encoder.decode(rawArmor);
	}

	@Override
	public ItemStack[] getInventory() throws IOException {
		if(encoder == null)
			throw new NullPointerException("Encoder required not found.");
		return encoder.decode(rawInventory);
	}
}
