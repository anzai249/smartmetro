package top.sleepingbed.smartmetro.listeners;

import top.sleepingbed.smartmetro.SmartMetro;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public class PlayerJoinListener implements Listener {
    
    private final SmartMetro plugin;
    
    public PlayerJoinListener(SmartMetro plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Automatically detect and set the player's locale based on their client settings
        plugin.getLocaleManager().detectAndSetPlayerLocale(player);
    }
    
    @EventHandler
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        // Update the player's locale when they change their client language
        plugin.getLocaleManager().detectAndSetPlayerLocale(player);
    }
}

