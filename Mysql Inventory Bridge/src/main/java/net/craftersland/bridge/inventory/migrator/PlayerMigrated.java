package net.craftersland.bridge.inventory.migrator;

import lombok.Getter;
import lombok.SneakyThrows;
import net.craftersland.bridge.inventory.InventoryUtils;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerMigrated {

    private final File playerFile;

    private final UUID uniqueId;

    public PlayerMigrated(File playerFile) {
        this.playerFile = playerFile;

        if(!playerFile.getName().endsWith(".dat"))
            throw new IllegalArgumentException("Malformed file extension.");

        this.uniqueId = UUID.fromString(playerFile.getName().split("\\.")[0]);
    }

    org.bukkit.inventory.ItemStack[] inventory = new org.bukkit.inventory.ItemStack[41];
    org.bukkit.inventory.ItemStack[] armor     = new org.bukkit.inventory.ItemStack[4];

    @SneakyThrows
    public void extractFile() {
        net.minecraft.server.v1_16_R3.NBTTagCompound nbtTagCompound = NBTCompressedStreamTools.a(playerFile);

        if (nbtTagCompound.hasKey("Inventory")) {
            Field f = nbtTagCompound.getClass().getField("map");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, NBTBase> map = (Map<String, NBTBase>) f.get(nbtTagCompound);
            NBTTagList inventoryNBTBase = (NBTTagList) map.get("Inventory");

            for (NBTBase nbtBase : inventoryNBTBase) {
                ItemStack itemStack = ItemStack.a((NBTTagCompound) nbtBase);
                int slot = Byte.toUnsignedInt(((NBTTagCompound) nbtBase).getByte("Slot"));
                org.bukkit.inventory.ItemStack foundItem = CraftItemStack.asBukkitCopy(itemStack);

                if (slot < 100) {
                    inventory[slot] = foundItem;
                } else if(slot < 104){
                    armor[slot - 100] = foundItem;
                } else {
                    inventory[40] = foundItem;
                }

            }

        }
    }

    public String getBase64Inventory() {
        return InventoryUtils.itemStackArrayToBase64(inventory);
    }

    public String getBase64Armor() {
        return InventoryUtils.itemStackArrayToBase64(armor);
    }

}
