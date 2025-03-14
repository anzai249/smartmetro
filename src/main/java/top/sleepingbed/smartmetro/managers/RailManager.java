package top.sleepingbed.smartmetro.managers;

import top.sleepingbed.smartmetro.SmartMetro;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RailManager {
    
    private final SmartMetro plugin;
    private final Map<String, String> stationRails; // Format: "world,x,y,z" -> stationId
    private File railsFile;
    
    public RailManager(SmartMetro plugin) {
        this.plugin = plugin;
        this.stationRails = new HashMap<>();
        this.loadRailsConfig();
    }
    
    private void loadRailsConfig() {
        railsFile = new File(plugin.getDataFolder(), "rails.yml");
        if (!railsFile.exists()) {
            railsFile.getParentFile().mkdirs();
            try {
                railsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create rails.yml: " + e.getMessage());
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(railsFile);
        ConfigurationSection railsSection = config.getConfigurationSection("station-rails");
        
        if (railsSection != null) {
            for (String locationKey : railsSection.getKeys(false)) {
                String stationId = railsSection.getString(locationKey);
                stationRails.put(locationKey, stationId);
            }
        }
    }
    
    public void saveRailsConfig() {
        FileConfiguration config = new YamlConfiguration();
        
        // Save station rails
        for (Map.Entry<String, String> entry : stationRails.entrySet()) {
            config.set("station-rails." + entry.getKey(), entry.getValue());
        }
        
        try {
            config.save(railsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save rails.yml: " + e.getMessage());
        }
    }
    
    public void setStationRail(Location location, String stationId) {
        String locationKey = locationToString(location);
        stationRails.put(locationKey, stationId);
        saveRailsConfig();
    }
    
    public String getStationAtRail(Location location) {
        String locationKey = locationToString(location);
        return stationRails.get(locationKey);
    }
    
    public void removeStationRail(Location location) {
        String locationKey = locationToString(location);
        stationRails.remove(locationKey);
        saveRailsConfig();
    }
    
    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
}

