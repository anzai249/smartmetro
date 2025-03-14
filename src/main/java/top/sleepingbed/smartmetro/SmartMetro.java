package top.sleepingbed.smartmetro;

import top.sleepingbed.smartmetro.commands.MetroCommand;
import top.sleepingbed.smartmetro.commands.MetroTabCompleter;
import top.sleepingbed.smartmetro.listeners.MinecartListener;
import top.sleepingbed.smartmetro.listeners.PlayerJoinListener;
import top.sleepingbed.smartmetro.listeners.TicketListener;
import top.sleepingbed.smartmetro.listeners.VendingMachineListener;
import top.sleepingbed.smartmetro.managers.ConfigManager;
import top.sleepingbed.smartmetro.managers.StationManager;
import top.sleepingbed.smartmetro.managers.LocaleManager;
import top.sleepingbed.smartmetro.managers.RailManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

public class SmartMetro extends JavaPlugin {
  
  private ConfigManager configManager;
  private StationManager stationManager;
  private LocaleManager localeManager;
  private RailManager railManager;
  
  @Override
  public void onEnable() {
      // Save default config
      saveDefaultConfig();
      
      // Initialize managers
      configManager = new ConfigManager(this);
      localeManager = new LocaleManager(this);
      stationManager = new StationManager(this);
      railManager = new RailManager(this);
      
      // Register commands with tab completer
      PluginCommand metroCommand = getCommand("metro");
      if (metroCommand != null) {
          MetroCommand commandExecutor = new MetroCommand(this);
          metroCommand.setExecutor(commandExecutor);
          metroCommand.setTabCompleter(new MetroTabCompleter(this));
      }
      
      // Register listeners
      getServer().getPluginManager().registerEvents(new MinecartListener(this), this);
      getServer().getPluginManager().registerEvents(new TicketListener(this), this);
      getServer().getPluginManager().registerEvents(new VendingMachineListener(this), this);
      getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
      
      getLogger().info("SmartMetro has been enabled!");
  }
  
  @Override
  public void onDisable() {
      // Save data
      stationManager.saveStations();
      railManager.saveRailsConfig();
      
      getLogger().info("SmartMetro has been disabled!");
  }
  
  public void reloadPlugin() {
      // Reload config
      reloadConfig();
      
      // Reload managers
      configManager = new ConfigManager(this);
      localeManager = new LocaleManager(this);
      stationManager = new StationManager(this);
      railManager = new RailManager(this);
      
      getLogger().info("SmartMetro has been reloaded!");
  }
  
  public ConfigManager getConfigManager() {
      return configManager;
  }
  
  public StationManager getStationManager() {
      return stationManager;
  }
  
  public LocaleManager getLocaleManager() {
      return localeManager;
  }
  
  public RailManager getRailManager() {
      return railManager;
  }
}

