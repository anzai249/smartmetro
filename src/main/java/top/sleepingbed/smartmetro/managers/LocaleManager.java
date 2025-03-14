package top.sleepingbed.smartmetro.managers;

import top.sleepingbed.smartmetro.SmartMetro;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocaleManager {
    
    private final SmartMetro plugin;
    private final Map<String, YamlConfiguration> locales;
    private final Map<UUID, String> playerLocales;
    private String defaultLocale;
    
    public LocaleManager(SmartMetro plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
        this.playerLocales = new HashMap<>();
        this.defaultLocale = plugin.getConfigManager().getDefaultLocale();
        
        // Load all locale files
        loadLocales();
    }
    
    private void loadLocales() {
        // Ensure locale directory exists
        File localeDir = new File(plugin.getDataFolder(), "locales");
        if (!localeDir.exists()) {
            localeDir.mkdirs();
            
            // Save default locales
            plugin.saveResource("locales/en_US.yml", false);
            plugin.saveResource("locales/zh_CN.yml", false);
            plugin.saveResource("locales/zh_TW.yml", false);
            plugin.saveResource("locales/ja_JP.yml", false);
            plugin.saveResource("locales/ko_KR.yml", false);
            plugin.saveResource("locales/ru_RU.yml", false);
            plugin.saveResource("locales/es_ES.yml", false);
            plugin.saveResource("locales/fr_FR.yml", false);
            plugin.saveResource("locales/de_DE.yml", false);
        }
        
        // Load all locale files from the directory
        File[] localeFiles = localeDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (localeFiles != null) {
            for (File file : localeFiles) {
                String localeName = file.getName().replace(".yml", "");
                locales.put(localeName, YamlConfiguration.loadConfiguration(file));
                plugin.getLogger().info("Loaded locale: " + localeName);
            }
        }
        
        // If default locale is not loaded, use en_US
        if (!locales.containsKey(defaultLocale)) {
            defaultLocale = "en_US";
            plugin.getLogger().warning("Default locale not found, using en_US instead");
        }
    }
    
    public String getMessage(Player player, String path) {
        String locale = playerLocales.getOrDefault(player.getUniqueId(), defaultLocale);
        return getMessage(locale, path);
    }
    
    public String getMessage(String locale, String path) {
        YamlConfiguration localeConfig = locales.getOrDefault(locale, locales.get(defaultLocale));
        return localeConfig.getString(path, "Missing translation: " + path);
    }
    
    public String getMessage(Player player, String path, Object... args) {
        String locale = playerLocales.getOrDefault(player.getUniqueId(), defaultLocale);
        return getMessage(locale, path, args);
    }
    
    public String getMessage(String locale, String path, Object... args) {
        String message = getMessage(locale, path);
        if (message != null && args != null && args.length > 0) {
            return String.format(message, args);
        }
        return message;
    }
    
    public void setPlayerLocale(Player player, String locale) {
        if (locales.containsKey(locale)) {
            playerLocales.put(player.getUniqueId(), locale);
        }
    }
    
    public String getPlayerLocale(Player player) {
        return playerLocales.getOrDefault(player.getUniqueId(), defaultLocale);
    }
    
    public Map<String, String> getAvailableLocales() {
        Map<String, String> availableLocales = new HashMap<>();
        for (String locale : locales.keySet()) {
            String localeName = locales.get(locale).getString("locale.name", locale);
            availableLocales.put(locale, localeName);
        }
        return availableLocales;
    }
}

