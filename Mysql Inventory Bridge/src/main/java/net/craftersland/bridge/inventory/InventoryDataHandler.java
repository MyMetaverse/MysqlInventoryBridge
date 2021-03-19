package net.craftersland.bridge.inventory;

import net.craftersland.bridge.inventory.migrator.EncodeResult;
import net.craftersland.bridge.inventory.migrator.PlayerMigrated;
import net.craftersland.bridge.inventory.objects.DatabaseInventoryData;
import net.craftersland.bridge.inventory.objects.InventorySyncData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InventoryDataHandler {

    private final Main pd;
    private final Set<Player> playersInSync = new HashSet<>();
    private final Set<Player> playersDisconnectSave = new HashSet<>();

    private final HashMap<UUID, DatabaseInventoryData> waitingToLoad = new HashMap<>();

    public InventoryDataHandler(Main pd) {
        this.pd = pd;
    }

    public boolean isSyncComplete(Player p) {
        return playersInSync.contains(p);
    }

    private void dataCleanup(Player p) {
        playersInSync.remove(p);
        playersDisconnectSave.remove(p);
    }

    public void setPlayerData(final Player p, DatabaseInventoryData data, InventorySyncData syncData) {
        if (!playersInSync.contains(p)) {

            setInventory(p, data, syncData);
            if (pd.getConfigHandler().getBoolean("General.syncArmorEnabled")) {
                setArmor(p, data, syncData);
            }

        }
    }

    public void forceMigratedDataSave(PlayerMigrated playerMigrated) {
        pd.getInvMysqlInterface().setData(playerMigrated.getUniqueId(),
                encodeItems(playerMigrated.getInventory()),
                encodeItems(playerMigrated.getArmor()));
    }

    public void onDataSaveFunction(Player p, Boolean datacleanup, ItemStack[] inventoryDisconnect, ItemStack[] armorDisconnect) {
        if (playersDisconnectSave.contains(p)) {
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Save Data - Canceled - " + p.getName());
            }
            return;
        }

        if (datacleanup)
            playersDisconnectSave.add(p);

        boolean isPlayerInSync = playersInSync.contains(p);
        if (isPlayerInSync) {
            EncodeResult inv = null;
            EncodeResult armor = null;
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Save Data - Start - " + p.getName());
            }
            try {
                if (inventoryDisconnect != null) {
                    if (pd.getConfigHandler().getBoolean("Debug.InventorySync"))
                        Main.log.info("Inventory Debug - Set Data - Saving disconnect inventory - " + p.getName());
                    inv = encodeItems(inventoryDisconnect);
                } else {
                    if (pd.getConfigHandler().getBoolean("Debug.InventorySync"))
                        Main.log.info("Inventory Debug - Set Data - Saving inventory - " + p.getName());
                    inv = encodeItems(p.getInventory().getContents());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (pd.getConfigHandler().getBoolean("General.syncArmorEnabled")) {
                try {

                    if (inventoryDisconnect != null)
                        armor = encodeItems(armorDisconnect);
                    else
                        armor = encodeItems(p.getInventory().getArmorContents());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            pd.getInvMysqlInterface().setData(p.getUniqueId(), inv, armor);
        }
        if (datacleanup) {
            dataCleanup(p);
        }
    }

    public void preLoadPlayer(UUID uniqueId) {
        if (Main.isDisabling) return;
        if (pd.getInvMysqlInterface().hasAccount(uniqueId)) {
            // First let's load the player's data.
            DatabaseInventoryData data = pd.getInvMysqlInterface().getData(uniqueId);
            waitingToLoad.put(uniqueId, data);
        } else {
            // If not, just pass null value, we'll handle this later.
            waitingToLoad.put(uniqueId, null);
        }
    }

    public void onJoinFunction(Player player) {
        if (Main.isDisabling) return;

        if (!playersInSync.contains(player)) {
            if (waitingToLoad.containsKey(player.getUniqueId())) {
                DatabaseInventoryData data = waitingToLoad.remove(player.getUniqueId());
                if (data == null) {
                    // The player wasn't saved, proceed saving the player without sync.
                    playersInSync.add(player);
                    onDataSaveFunction(player, false, null, null);
                } else {
                    // The player was saved.

                    // Backup player's inventory in case anything goes wrong while sync.
                    InventorySyncData syncData = new InventorySyncData(
                            player.getInventory().getContents(),
                            player.getInventory().getArmorContents()
                    );

                    player.setItemOnCursor(null);
                    player.getInventory().clear(); // We clear the inventory to overwrite with the custom items.
                    player.updateInventory();

                    // We update the player inventory.
                    // If anything goes wrong, will place again the old player items.
                    setPlayerData(player, data, syncData);
                }
            } else {
                pd.getInvMysqlInterface().getData(player.getUniqueId(), data -> {
                    if (data != null) {
                        // If we get the data, replace the old null value with the actual data.
                        waitingToLoad.put(player.getUniqueId(), data);

                        // Repeat the process.
                        onJoinFunction(player);
                    } else {
                        playersDisconnectSave.remove(player); // We don't want to save the player bugged inventory.
                        // If not, we leave the loop and kick the player.
                        player.kickPlayer("Failed while trying to load your data, try again.");
                    }
                });
            }
        }
    }

    public ItemStack[] getInventory(Player p) {
        return p.getInventory().getContents();
    }

    public ItemStack[] getArmor(Player p) {
        if (pd.getConfigHandler().getBoolean("General.syncArmorEnabled")) {
            return p.getInventory().getArmorContents();
        } else {
            return null;
        }
    }

    private void setInventory(final Player p, DatabaseInventoryData data, InventorySyncData syncData) {
        if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
            Main.log.info("Inventory Debug - Set Data - Start- " + p.getName());
        }
        if (!data.getRawInventory().matches("none")) {
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - Loading inventory - " + p.getName());
            }
            try {
                p.getInventory().setContents(decodeItems(data.getRawInventory(), data.getEncode()));
                if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                    Main.log.info("Inventory Debug - Set Data - Inventory set - " + p.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runBackup(p, syncData);
            }
        } else {
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - Restoring local inventory - " + p.getName());
            }
            p.getInventory().setContents(syncData.getBackupInventory());
        }
        p.updateInventory();
    }

    private void runBackup(Player player, InventorySyncData inventorySyncData) {
        if (inventorySyncData.getBackupInventory() != null) {
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - Loading backup inventory - " + player.getName());
            }
            player.getInventory().setContents(inventorySyncData.getBackupInventory());
            player.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessage.inventorySyncError"));
            pd.getSoundHandler().sendPlingSound(player);
            player.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessage.inventorySyncBackup"));
        } else {
            if (pd.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - No backup inventory found! - " + player.getName());
            }
        }
    }

    private void setArmor(final Player p, DatabaseInventoryData data, InventorySyncData syncData) {
        if (!data.getRawArmor().matches("none")) {
            try {
                p.getInventory().setArmorContents(decodeItems(data.getRawArmor(), data.getEncode()));
            } catch (Exception e) {
                e.printStackTrace();
                p.getInventory().setArmorContents(syncData.getBackupArmor());
                p.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessage.armorSyncError"));
                pd.getSoundHandler().sendPlingSound(p);
                p.sendMessage(pd.getConfigHandler().getStringWithColor("ChatMessage.armorSyncBackup"));
            }
        } else {
            p.getInventory().setArmorContents(syncData.getBackupArmor());
        }
        p.updateInventory();
    }

    public EncodeResult encodeItems(ItemStack[] items) {
        if (pd.useProtocolLib && pd.getConfigHandler().getBoolean("General.enableModdedItemsSupport")) {
            return new EncodeResult(InventoryUtils.saveModdedStacksData(items), "modded");
        } else {
            return new EncodeResult(InventoryUtils.itemStackArrayToBase64(items), "vanilla");
        }
    }

    public ItemStack[] decodeItems(String data) throws Exception {
        if (pd.useProtocolLib && pd.getConfigHandler().getBoolean("General.enableModdedItemsSupport")) {
            ItemStack[] it = InventoryUtils.restoreModdedStacks(data);
            if (it == null) {
                it = InventoryUtils.itemStackArrayFromBase64(data);
            }
            return it;
        } else {
            return InventoryUtils.itemStackArrayFromBase64(data);
        }
    }

    public ItemStack[] decodeItems(String data, String codec) throws Exception {
        // We first try to decode the items with the saved codec.
        if (codec.equals("vanilla")) {
            return InventoryUtils.itemStackArrayFromBase64(data);
        } else if (codec.equals("modded")) {
            ItemStack[] it = InventoryUtils.restoreModdedStacks(data);
            if (it == null) {
                it = InventoryUtils.itemStackArrayFromBase64(data);
            }
            return it;
        } else { // If none option is found, we target to force the server decode type.
            return decodeItems(data);
        }

    }

}
