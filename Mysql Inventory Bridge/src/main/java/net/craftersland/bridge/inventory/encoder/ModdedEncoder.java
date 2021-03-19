package net.craftersland.bridge.inventory.encoder;

import com.comphenix.protocol.utility.StreamSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class ModdedEncoder implements Encoder {

    @Override
    public String nameId() {
        return "modded";
    }

    @Override
    public EncodeResult encode(ItemStack[] items) {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0)
                stringBuilder.append(";");

            if (items[i] != null && items[i].getType() != Material.AIR) {
                try {
                    stringBuilder.append(StreamSerializer.getDefault().serializeItemStack(items[i]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return new EncodeResult(stringBuilder.toString(), nameId());
    }

    @Override
    public ItemStack[] decode(String string) throws IOException {

        String[] strings = string.split(";");
        ItemStack[] itemStacks = new ItemStack[strings.length];
        for (int i = 0; i < strings.length; i++) {
            if (!strings[i].isEmpty()) {
                itemStacks[i] = StreamSerializer.getDefault().deserializeItemStack(strings[i]);
            }
        }

        return itemStacks;

    }
}
