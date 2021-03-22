package net.craftersland.bridge.inventory.encoder;

import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public interface DataRetainer {

    ItemStack[] getArmor() throws IOException;

    ItemStack[] getInventory() throws IOException;

}
