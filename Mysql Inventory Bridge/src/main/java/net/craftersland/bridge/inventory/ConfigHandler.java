package net.craftersland.bridge.inventory;

import java.io.File;

public class ConfigHandler {
	
	private final Main main;
	
	public ConfigHandler(Main main) {
		this.main = main;
		loadConfig();
	}
	
	public void loadConfig() {
		File pluginFolder = new File(main.getDataFolder().getAbsolutePath());
		if (!pluginFolder.exists()) {
    		pluginFolder.mkdir();
    	}
		File configFile = new File(main.getDataFolder() + System.getProperty("file.separator") + "config.yml");
		if (!configFile.exists()) {
			Main.log.info("No config file found! Creating new one...");
			main.saveDefaultConfig();
		}
    	try {
    		Main.log.info("Loading the config file...");
    		main.getConfig().load(configFile);
    	} catch (Exception e) {
    		Main.log.severe("Could not load the config file! You need to regenerate the config! Error: " + e.getMessage());
			e.printStackTrace();
    	}
	}
	
	public String getString(String key) {
		if (!main.getConfig().contains(key)) {
			main.getLogger().severe("Could not locate " + key + " in the config.yml inside of the " + Main.pluginName + " folder! (Try generating a new one by deleting the current)");
			return "errorCouldNotLocateInConfigYml:" + key;
		} else {
			return main.getConfig().getString(key);
		}
	}
	
	public String getStringWithColor(String key) {
		if (!main.getConfig().contains(key)) {
			main.getLogger().severe("Could not locate " + key + " in the config.yml inside of the " + Main.pluginName + " folder! (Try generating a new one by deleting the current)");
			return "errorCouldNotLocateInConfigYml:" + key;
		} else {
			return main.getConfig().getString(key).replaceAll("&", "�");
		}
	}
	
	public Integer getInteger(String key) {
		if (!main.getConfig().contains(key)) {
			main.getLogger().severe("Could not locate " + key + " in the config.yml inside of the " + Main.pluginName + " folder! (Try generating a new one by deleting the current)");
			return null;
		} else {
			return main.getConfig().getInt(key);
		}
	}

	public Integer getInteger(String key, int def) {
		if (!main.getConfig().contains(key)) {
			return def;
		} else {
			return main.getConfig().getInt(key);
		}
	}

	public String getString(String key, String def) {
		if (!main.getConfig().contains(key)) {
			return def;
		} else {
			return main.getConfig().getString(key);
		}
	}

	public Boolean getBoolean(String key) {
		if (!main.getConfig().contains(key)) {
			main.getLogger().severe("Could not locate " + key + " in the config.yml inside of the " + Main.pluginName + " folder! (Try generating a new one by deleting the current)");
			return null;
		} else {
			return main.getConfig().getBoolean(key);
		}
	}

}
