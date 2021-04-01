package net.craftersland.bridge.inventory;

import net.craftersland.bridge.inventory.encoder.*;
import net.craftersland.bridge.inventory.jedisbridge.BridgeResult;
import net.craftersland.bridge.inventory.migrator.PlayerMigrated;
import net.craftersland.bridge.inventory.objects.BlackListedItem;
import net.craftersland.bridge.inventory.objects.DatabaseInventoryData;
import net.craftersland.bridge.inventory.objects.InventorySyncData;
import net.craftersland.bridge.inventory.wallethook.WalletHandler;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryDataHandler {

    private final Main main;
    private final Set<UUID> playersInSync = new HashSet<>();
    private final Set<Player> playersDisconnectSave = new HashSet<>();

    private final HashMap<UUID, DataRetainer> waitingToLoad = new HashMap<>();

    public InventoryDataHandler(Main main) {
        this.main = main;
    }

    public boolean isSyncComplete(Player p) {
        return playersInSync.contains(p.getUniqueId());
    }

    private void dataCleanup(Player p) {
        playersInSync.remove(p.getUniqueId());
        playersDisconnectSave.remove(p);
    }

    public void setPlayerData(final Player p, DataRetainer data, InventorySyncData syncData) {

        setInventory(p, data, syncData);
        if (main.getConfigHandler().getBoolean("General.syncArmorEnabled")) {
            setArmor(p, data, syncData);
        }

    }

    public void forceMigratedDataSave(PlayerMigrated playerMigrated) {
        main.getInvMysqlInterface().setData(playerMigrated.getUniqueId(),
                encodeItems(playerMigrated.getInventory()),
                encodeItems(playerMigrated.getArmor()));
    }

    public EncodeResult[] convertData(Player p, ItemStack[] inventoryDisconnect, ItemStack[] armorDisconnect) {
        EncodeResult inv = null;
        EncodeResult armor = null;
        if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
            Main.log.info("Inventory Debug - Save Data - Start - " + p.getName());
        }
        try {
            if (inventoryDisconnect != null) {
                if (main.getConfigHandler().getBoolean("Debug.InventorySync"))
                    Main.log.info("Inventory Debug - Set Data - Saving disconnect inventory - " + p.getName());
                inv = encodeItems(inventoryDisconnect);
            } else {
                if (main.getConfigHandler().getBoolean("Debug.InventorySync"))
                    Main.log.info("Inventory Debug - Set Data - Saving inventory - " + p.getName());
                inv = encodeItems(this.getInventory(p));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (main.getConfigHandler().getBoolean("General.syncArmorEnabled")) {
            try {

                if (inventoryDisconnect != null)
                    armor = encodeItems(armorDisconnect);
                else
                    armor = encodeItems(this.getArmor(p));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new EncodeResult[]{inv, armor};
    }


    public void saveMultiplePlayers(Collection<Player> players, Boolean datacleanup) {

        if (!WalletHandler.getInstance().getStatus())
            Main.log.warning("Wallet dependency not found. Items won't be blacklisted anymore.");

        Flux.fromIterable(players)
                .filter(player -> !playersDisconnectSave.contains(player))
                .map(p -> {
                    ItemStack[] inventoryDisconnect = this.getInventory(p);
                    ItemStack[] armorDisconnect = this.getArmor(p);

                    if (datacleanup)
                        playersDisconnectSave.add(p);

                    boolean isPlayerInSync = playersInSync.contains(p.getUniqueId());
                    if (isPlayerInSync) {
                        EncodeResult[] results = convertData(p, inventoryDisconnect, armorDisconnect);
                        if (results != null)
                            return new Object[]{p.getUniqueId(), results[0], results[1]};
                    }
                    if (datacleanup) {
                        dataCleanup(p);
                    }
                    return new Object[0];
                })
                .filter(objects -> objects.length > 0)
                .collect(Collectors.toList())
                .subscribe(main.getInvMysqlInterface()::setData);

    }

    public void onDataSaveFunction(Player p, Boolean datacleanup, ItemStack[] inventoryDisconnect, ItemStack[] armorDisconnect) {
        if (playersDisconnectSave.contains(p)) {
            if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Save Data - Canceled - " + p.getName());
            }
            return;
        }

        if (datacleanup)
            playersDisconnectSave.add(p);

        boolean isPlayerInSync = playersInSync.contains(p.getUniqueId());
        if (isPlayerInSync) {
            EncodeResult[] results = convertData(p, inventoryDisconnect, armorDisconnect);
            if(results != null)
                main.getInvMysqlInterface().setData(p.getUniqueId(), results[0], results[1]);
        }
        if (datacleanup) {
            dataCleanup(p);
        }
    }


    public boolean preLoadPlayer(UUID uniqueId) {
        if (Main.isDisabling || main.getInvMysqlInterface() == null) return false;

        return main.getConnectionHandler().execute(connection -> {
            if (main.getInvMysqlInterface().hasAccount(connection, uniqueId)) {
                // First let's load the player's data.
                DatabaseInventoryData data = main.getInvMysqlInterface().getData(uniqueId);
                waitingToLoad.put(uniqueId, data);
            } else {
                // If not, just pass null value, we'll handle this later.
                waitingToLoad.put(uniqueId, null);
            }
            return true;
        });

    }

    public void assignPlayer(UUID uniqueId, BridgeResult bridgeResult) {
        waitingToLoad.put(uniqueId, bridgeResult);
    }

    public boolean onJoinFunction(Player player) {
        return onJoinFunction(player, false);
    }

    /**
     * Execute this when a player joins into the server.
     *
     * @param player the player
     * @return If the player was sync into memory.
     */
    public boolean onJoinFunction(Player player, boolean force) {
        if (Main.isDisabling) return false;

        if (!playersInSync.contains(player.getUniqueId()) || force) {
            if (waitingToLoad.containsKey(player.getUniqueId())) {
                DataRetainer data = waitingToLoad.remove(player.getUniqueId());
                if (data == null) {
                    // The player wasn't saved, proceed saving the player without sync.
                    playersInSync.add(player.getUniqueId());
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
                    playersInSync.add(player.getUniqueId());

                }
                return true;
            }
        } else return true;

        return false;
    }

    public ItemStack[] getInventory(Player p) {

        ItemStack[] playerInventory = p.getInventory().getContents();
        Set<BlackListedItem> itemsBlacklist = Optional.ofNullable(WalletHandler.getInstance().getBlacklistItems())
                .orElse(Collections.emptySet());

        Arrays.stream(playerInventory).forEach(item -> {
            BlackListedItem currentItem = new BlackListedItem(item);
            if (itemsBlacklist.contains(currentItem)) {
                playerInventory[ArrayUtils.indexOf(playerInventory, item)] = null;
            }
        });

        return playerInventory;

    }

    public ItemStack[] getArmor(Player p) {

        if (main.getConfigHandler().getBoolean("General.syncArmorEnabled")) {

            ItemStack[] playerArmor = p.getInventory().getArmorContents();
            Set<BlackListedItem> itemsBlacklist = Optional.ofNullable(WalletHandler.getInstance().getBlacklistItems())
                    .orElse(Collections.emptySet());

            Arrays.stream(playerArmor).forEach(item -> {
                BlackListedItem currentItem = new BlackListedItem(item);
                if (itemsBlacklist.contains(currentItem)) {
                    playerArmor[ArrayUtils.indexOf(playerArmor, item)] = null;
                }
            });

            return playerArmor;

        } else {
            return null;
        }

    }

    private void setInventory(final Player p, DataRetainer data, InventorySyncData syncData) {
        if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
            Main.log.info("Inventory Debug - Set Data - Start- " + p.getName());
        }

        try {
            ItemStack[] inventory = data.getInventory();

            if (inventory != null) {
                if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                    Main.log.info("Inventory Debug - Set Data - Loading inventory - " + p.getName());
                }
                p.getInventory().setContents(inventory);
                if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                    Main.log.info("Inventory Debug - Set Data - Inventory set - " + p.getName());
                }
            } else {
                if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                    Main.log.info("Inventory Debug - Set Data - Restoring local inventory - " + p.getName());
                }
                p.getInventory().setContents(syncData.getBackupInventory());
            }
        } catch (IOException e) {
            e.printStackTrace();
            runBackup(p, syncData);
        }


        p.updateInventory();
    }

    private void runBackup(Player player, InventorySyncData inventorySyncData) {
        if (inventorySyncData.getBackupInventory() != null) {
            if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - Loading backup inventory - " + player.getName());
            }
            player.getInventory().setContents(inventorySyncData.getBackupInventory());
            player.sendMessage(main.getConfigHandler().getStringWithColor("ChatMessage.inventorySyncError"));
            main.getSoundHandler().sendPlingSound(player);
            player.sendMessage(main.getConfigHandler().getStringWithColor("ChatMessage.inventorySyncBackup"));
        } else {
            if (main.getConfigHandler().getBoolean("Debug.InventorySync")) {
                Main.log.info("Inventory Debug - Set Data - No backup inventory found! - " + player.getName());
            }
        }
    }

    private void setArmor(final Player p, DataRetainer data, InventorySyncData syncData) {
        try {
            ItemStack[] armor = data.getArmor();

            if (armor != null) {
                p.getInventory().setArmorContents(armor);
            } else {
                p.getInventory().setArmorContents(syncData.getBackupArmor());
            }
        } catch (IOException e) {
            e.printStackTrace();
            p.getInventory().setArmorContents(syncData.getBackupArmor());
            p.sendMessage(main.getConfigHandler().getStringWithColor("ChatMessage.armorSyncError"));
            main.getSoundHandler().sendPlingSound(p);
            p.sendMessage(main.getConfigHandler().getStringWithColor("ChatMessage.armorSyncBackup"));
        }
        p.updateInventory();
    }

    public EncodeResult encodeItems(ItemStack[] items) {
        if (main.useProtocolLib && main.getConfigHandler().getBoolean("General.enableModdedItemsSupport")) {
            return new ModdedEncoder().encode(items);
        } else {
            return new VanillaEncoder().encode(items);
        }
    }

    public ItemStack[] decodeItems(String data) throws Exception {
        if (main.useProtocolLib && main.getConfigHandler().getBoolean("General.enableModdedItemsSupport")) {
            ItemStack[] it = new ModdedEncoder().decode(data);
            if (it == null) {
                it = new VanillaEncoder().decode(data);
            }
            return it;
        } else {
            return new VanillaEncoder().decode(data);
        }
    }

    public ItemStack[] decodeItems(String data, String codec) throws Exception {
        // We first try to decode the items with the saved codec.
        Encoder encoder = EncoderFactory.getEncoder(codec);
        if (encoder != null) {
            ItemStack[] result = encoder.decode(data);
            // If anything goes wrong we can try to decide with vanilla decoder.
            return result != null ? result : new VanillaEncoder().decode(data);
        } else return decodeItems(data); // If no encoder found, we try to decode with the customized encoder.

    }

}
