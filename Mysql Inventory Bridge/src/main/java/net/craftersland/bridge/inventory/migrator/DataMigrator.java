package net.craftersland.bridge.inventory.migrator;

import lombok.RequiredArgsConstructor;
import net.craftersland.bridge.inventory.Main;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.SavedFile;
import org.apache.commons.io.FileUtils;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class DataMigrator {

    private final Plugin plugin;

    public File[] getPlayerFiles() {
        MinecraftServer minecraftServer = ((CraftWorld) (plugin.getServer().getWorlds().get(0))).getHandle().getMinecraftServer();
        return minecraftServer.a(SavedFile.PLAYERDATA).toFile().listFiles((dir, name) -> name.endsWith(".dat"));
    }

    public boolean createBackup(long millis) {
        MinecraftServer minecraftServer = ((CraftWorld) (plugin.getServer().getWorlds().get(0))).getHandle().getMinecraftServer();
        File file = minecraftServer.a(SavedFile.PLAYERDATA).toFile();
        try {
            File target = new File(plugin.getDataFolder(), "playerDataBackup");
            if(!target.exists())
                target.createNewFile();

            FileUtils.copyDirectoryToDirectory(file, new File(target.toURI().resolve(String.valueOf(millis))));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void migrateServer(Main plugin, long currentUnixTime) {
        if (System.currentTimeMillis() - currentUnixTime >= 5000) {
            throw new IllegalArgumentException("Please introduce a valid unix time, " +
                    "should be max 5 seconds above the one when the action is executed.");
        }

        plugin.getLogger().info("---------------------------------------------------------");
        plugin.getLogger().info("");
        plugin.getLogger().info("Starting migration process.");

        DataMigrator dataMigrator = new DataMigrator(plugin);

        plugin.getLogger().info("Creating data backup.");
        dataMigrator.createBackup(currentUnixTime);
        plugin.getLogger().info("Backup finished.");

        plugin.getLogger().info("Starting migration to SQL.");
        for (File playerFile : dataMigrator.getPlayerFiles()) {
            plugin.getLogger().info("");
            String user = playerFile.getName().split("\\.")[0];

            plugin.getLogger().info("Migrating user " + user);

            PlayerMigrated playerMigrated = new PlayerMigrated(playerFile);

            plugin.getLogger().info("Extracting file.");

            playerMigrated.extractFile();

            plugin.getLogger().info("File extracted, initiating save to sql.");

            plugin.getInventoryDataHandler().forceMigratedDataSave(playerMigrated);

            plugin.getLogger().info("User saved to SQL.");

            plugin.getLogger().info("Finished migrating " + user);
            plugin.getLogger().info("");
        }

        plugin.getLogger().info("---------------------------------------------------------");
        plugin.getLogger().info("Finishing migration.");
    }

}
