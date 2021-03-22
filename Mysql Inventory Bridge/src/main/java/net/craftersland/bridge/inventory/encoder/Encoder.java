package net.craftersland.bridge.inventory.encoder;

import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.Serializable;

public interface Encoder extends Serializable {

    String nameId();

    EncodeResult encode(ItemStack[] items);

    ItemStack[] decode(String value) throws IOException;

}
