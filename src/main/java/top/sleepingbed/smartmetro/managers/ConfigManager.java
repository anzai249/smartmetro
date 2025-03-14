package top.sleepingbed.smartmetro.managers;

import top.sleepingbed.smartmetro.SmartMetro;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final SmartMetro plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(SmartMetro plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        
        // Set default values if they don't exist
        config.addDefault("ticket.material", "PAPER");
        config.addDefault("vending-machine.skin", "CONSOLE");
        config.addDefault("locale.default", "en_US");
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
        
        // Create stations.yml if it doesn't exist
        loadStationsConfig();
    }
    
    public void loadStationsConfig() {
        configFile = new File(plugin.getDataFolder(), "stations.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("stations.yml", false);
        }
    }
    
    public FileConfiguration getStationsConfig() {
        return YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void saveStationsConfig(FileConfiguration config) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stations config: " + e.getMessage());
        }
    }
    
    public String getTicketMaterial() {
        return config.getString("ticket.material");
    }
    
    public String getVendingMachineSkin() {
        return config.getString("vending-machine.skin");
    }
    
    public String getDefaultLocale() {
        return config.getString("locale.default", "en_US");
    }
}

