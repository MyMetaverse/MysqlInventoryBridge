package net.craftersland.bridge.inventory.encoder;

import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public interface Encoder {

    String nameId();

    EncodeResult encode(ItemStack[] items);

    ItemStack[] decode(String value) throws IOException;

}
