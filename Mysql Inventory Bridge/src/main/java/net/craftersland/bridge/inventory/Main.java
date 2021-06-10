package net.craftersland.bridge.inventory;

import io.mymetaverse.livewallet.api.MetaWalletAPI;
import lombok.Getter;
import net.craftersland.bridge.inventory.database.ConnectionHandler;
import net.craftersland.bridge.inventory.database.InvMysqlInterface;
import net.craftersland.bridge.inventory.database.MysqlSetup;
import net.craftersland.bridge.inventory.events.InventoryClick;
import net.craftersland.bridge.inventory.events.PlayerJoin;
import net.craftersland.bridge.inventory.events.PlayerQuit;
import net.craftersland.bridge.inventory.hooks.AdvancedMobArenaListener;
import net.craftersland.bridge.inventory.hooks.WalletHandler;
import net.craftersland.bridge.inventory.jedisbridge.Bridge;
import net.craftersland.bridge.inventory.jedisbridge.InventoryPusher;
import net.craftersland.bridge.inventory.migrator.DataMigrator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class Main extends JavaPlugin {

    public static Logger log;
    public boolean useProtocolLib = false;
    public static String pluginName = "MysqlInventoryBridge";
    //public Set<String> playersSync = new HashSet<String>();
    public static boolean is19Server = true;
    public static boolean is13Server = false;
    public static boolean isDisabling = false;

    private static ConfigHandler configHandler;
    private static SoundHandler sH;
    private static MysqlSetup databaseManager;
    private static InvMysqlInterface invMysqlInterface;
    private static InventoryDataHandler idH;
    private static BackgroundTask bt;

    @Getter
    private ConnectionHandler connectionHandler;

    @Getter
    private Bridge bridge;

    @Getter
    private InventoryPusher inventoryPusher;

    @Getter
    private WalletHandler walletHandler;

    @Override
    public void onEnable() {
        inventoryPusher = new InventoryPusher(this);

        log = getLogger();
        getMcVersion();
        configHandler = new ConfigHandler(this);
        sH = new SoundHandler(this);
        checkDependency();
        try {
            connectionHandler = new ConnectionHandler(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().severe("DATABASE NOT AVAILABLE FOR INVENTORIES. ");
            //Bukkit.getServer().shutdown();
        }

        bt = new BackgroundTask(this);

        try {
            databaseManager = new MysqlSetup(this);
            invMysqlInterface = new InvMysqlInterface(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            getLogger().severe("DATABASE NOT NOT GENERATED. ");
            //Bukkit.getServer().shutdown();
        }

        idH = new InventoryDataHandler(this);
        //Register Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoin(this), this);
        pm.registerEvents(new PlayerQuit(this), this);
        pm.registerEvents(new InventoryClick(this), this);

        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AdvancedMobArena");
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getServer().getPluginManager().registerEvents(new AdvancedMobArenaListener(this), this);
        }

        // Loading Redis inventory bridge.
        this.bridge = new Bridge(this);

        // Register WalletHandler
        this.walletHandler = new WalletHandler(MetaWalletAPI.getInstance().getPlugin());

        log.info(pluginName + " loaded successfully!");

    }

    @Override
    public void onDisable() {
        isDisabling = true;
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        bt.onShutDownDataSave();
        databaseManager.closeConnection();
        bridge.closeRedis();
        log.info(pluginName + " is disabled!");
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public MysqlSetup getDatabaseManager() {
        return databaseManager;
    }

    public InvMysqlInterface getInvMysqlInterface() {
        return invMysqlInterface;
    }

    public SoundHandler getSoundHandler() {
        return sH;
    }

    public BackgroundTask getBackgroundTask() {
        return bt;
    }

    public InventoryDataHandler getInventoryDataHandler() {
        return idH;
    }

    private boolean getMcVersion() {
        String[] serverVersion = Bukkit.getBukkitVersion().split("-");
        String version = serverVersion[0];

        if (version.matches("1.7.10") || version.matches("1.7.9") || version.matches("1.7.5") || version.matches("1.7.2") || version.matches("1.8.8") || version.matches("1.8.3") || version.matches("1.8.4") || version.matches("1.8")) {
            is19Server = false;
            return true;
        } else if (version.matches("1.13") || version.matches("1.13.1") || version.matches("1.13.2")) {
            is13Server = true;
            return true;
        } else if (version.matches("1.14") || version.matches("1.14.1") || version.matches("1.14.2") || version.matches("1.14.3") || version.matches("1.14.4")) {
            is13Server = true;
            return true;
        } else if (version.matches("1.15") || version.matches("1.15.1") || version.matches("1.15.2")) {
            is13Server = true;
            return true;
        } else if (version.matches("1.16") || version.matches("1.16.1") || version.matches("1.16.2") || version.matches("1.16.3")) {
            is13Server = true;
            return true;
        }
        return false;
    }

    private void checkDependency() {
        //Check dependency
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            useProtocolLib = true;
            log.info("ProtocolLib dependency found.");
        } else {
            useProtocolLib = false;
            log.warning("ProtocolLib dependency not found. No support for modded items NBT data!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equals("mib")) {

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Nothing to show.");
            } else if (args[0].equalsIgnoreCase("migrate")) {
                if (sender instanceof ConsoleCommandSender && sender.isOp()) {
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Please append a valid unix time.");
                        return true;
                    }

                    if (!args[1].matches("[0-9]+")) {
                        sender.sendMessage(ChatColor.RED + "Invalid unix time.");
                        return false;
                    }

                    sender.sendMessage(ChatColor.GREEN + "Starting migration.");
                    long now = System.currentTimeMillis();
                    long unixTime = Long.parseUnsignedLong(args[1]);

                    try {
                        DataMigrator.migrateServer(this, unixTime);
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.RED + "Migration failed with message: " + ex.getLocalizedMessage());
                        ex.printStackTrace();
                    } finally {
                        sender.sendMessage(ChatColor.YELLOW + "Migration try in: " + ChatColor.AQUA
                                + (System.currentTimeMillis() - now) + " milliseconds.");
                    }

                    sender.sendMessage(ChatColor.GREEN + "Migration finished.");
                } else {
                    sender.sendMessage(ChatColor.RED + "haha no.");
                }
            }

            return true;
        }

        return true;
    }


}
