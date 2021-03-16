package net.craftersland.bridge.inventory;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Logger;

import net.craftersland.bridge.inventory.database.InvMysqlInterface;
import net.craftersland.bridge.inventory.database.MysqlSetup;
import net.craftersland.bridge.inventory.events.DropItem;
import net.craftersland.bridge.inventory.events.InventoryClick;
import net.craftersland.bridge.inventory.events.PlayerJoin;
import net.craftersland.bridge.inventory.events.PlayerQuit;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
	
	@Override
    public void onEnable() {
		log = getLogger();
		getMcVersion();
    	configHandler = new ConfigHandler(this);
    	sH = new SoundHandler(this);
    	checkDependency();
    	bt = new BackgroundTask(this);
    	databaseManager = new MysqlSetup(this);
    	invMysqlInterface = new InvMysqlInterface(this);
    	idH = new InventoryDataHandler(this);
    	//Register Listeners
    	PluginManager pm = getServer().getPluginManager();
    	pm.registerEvents(new PlayerJoin(this), this);
    	pm.registerEvents(new PlayerQuit(this), this);
    	pm.registerEvents(new DropItem(this), this);
    	pm.registerEvents(new InventoryClick(this), this);
    	log.info(pluginName + " loaded successfully!");
	}
	
	@Override
    public void onDisable() {
		isDisabling = true;
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(this);
		if (databaseManager.getConnection() != null) {
			bt.onShutDownDataSave();
			databaseManager.closeConnection();
		}
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
			} else if (args[0].equalsIgnoreCase("test")) {
				sender.sendMessage(ChatColor.GREEN + "Running test command.");

				try {
					File file = new File(this.getDataFolder(), "testdata.dat");

					if (!file.exists()) {
						sender.sendMessage(ChatColor.RED + "Test file doesn't exists.");
						return true;
					}

					net.minecraft.server.v1_16_R3.NBTTagCompound nbtTagCompound = NBTCompressedStreamTools.a(file);

					if (nbtTagCompound.hasKey("Inventory")) {
						// NBTTagList inventoryNBTBase = nbtTagCompound.getList("Inventory", 10);
						Field f = nbtTagCompound.getClass().getField("map");
						f.setAccessible(true);
						Map<String, NBTBase> map = (Map<String, NBTBase>) f.get(nbtTagCompound);
						NBTTagList inventoryNBTBase = (NBTTagList) map.get("Inventory");
						System.out.println(inventoryNBTBase.getTypeId());
						for (NBTBase nbtBase : inventoryNBTBase) {
							System.out.println("type: " + nbtBase.getTypeId());
							ItemStack itemStack = ItemStack.a((NBTTagCompound) nbtBase);
							System.out.println(itemStack.toString());
						}
					}

				} catch (IOException | NoSuchFieldException | IllegalAccessException e) {
					e.printStackTrace();
					sender.sendMessage(ChatColor.RED + "Something went wrong :s");
				}

			}

			return true;
		}

		return true;
	}

}
