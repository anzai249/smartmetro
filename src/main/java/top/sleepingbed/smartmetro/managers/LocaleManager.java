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
  private final Map<String, String> clientLocaleMap;
  
  public LocaleManager(SmartMetro plugin) {
      this.plugin = plugin;
      this.locales = new HashMap<>();
      this.playerLocales = new HashMap<>();
      this.defaultLocale = plugin.getConfigManager().getDefaultLocale();
      this.clientLocaleMap = new HashMap<>();
      
      // Initialize client locale mapping
      initClientLocaleMap();
      
      // Load all locale files
      loadLocales();
  }
  
  private void initClientLocaleMap() {
      // Map Minecraft client locales to our supported locales
      clientLocaleMap.put("en_us", "en_US");
      clientLocaleMap.put("en_gb", "en_US");
      clientLocaleMap.put("en_au", "en_US");
      clientLocaleMap.put("en_ca", "en_US");
      clientLocaleMap.put("en_nz", "en_US");
      
      clientLocaleMap.put("zh_cn", "zh_CN");
      clientLocaleMap.put("zh_tw", "zh_TW");
      clientLocaleMap.put("ja_jp", "ja_JP");
      clientLocaleMap.put("ko_kr", "ko_KR");
      clientLocaleMap.put("ru_ru", "ru_RU");
      clientLocaleMap.put("es_es", "es_ES");
      clientLocaleMap.put("es_mx", "es_ES");
      clientLocaleMap.put("es_ar", "es_ES");
      clientLocaleMap.put("es_cl", "es_ES");
      clientLocaleMap.put("es_uy", "es_ES");
      clientLocaleMap.put("es_ve", "es_ES");
      
      clientLocaleMap.put("fr_fr", "fr_FR");
      clientLocaleMap.put("fr_ca", "fr_FR");
      
      clientLocaleMap.put("de_de", "de_DE");
      clientLocaleMap.put("de_at", "de_DE");
      clientLocaleMap.put("de_ch", "de_DE");
      
      clientLocaleMap.put("lzh", "lzh");
      clientLocaleMap.put("zh_classical", "lzh");
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
          plugin.saveResource("locales/lzh.yml", false);
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
  
  /**
   * Automatically detect and set the player's locale based on their client settings
   * @param player The player to set the locale for
   */
  public void detectAndSetPlayerLocale(Player player) {
      String clientLocale = player.getLocale().toLowerCase();
      String mappedLocale = clientLocaleMap.getOrDefault(clientLocale, defaultLocale);
      
      // Only set if we have this locale
      if (locales.containsKey(mappedLocale)) {
          playerLocales.put(player.getUniqueId(), mappedLocale);
          plugin.getLogger().info("Set player " + player.getName() + " locale to " + mappedLocale + 
                  " (from client locale " + clientLocale + ")");
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

