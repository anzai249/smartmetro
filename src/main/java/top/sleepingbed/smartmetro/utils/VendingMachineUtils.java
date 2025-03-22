package top.sleepingbed.smartmetro.utils;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Station;
import top.sleepingbed.smartmetro.models.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VendingMachineUtils {
    
    private static final NamespacedKey STATION_KEY = new NamespacedKey("smartmetro", "bound_station");
    
    public static void openVendingMachineGUI(SmartMetro plugin, Player player, String currentStationId) {
        Station currentStation = plugin.getStationManager().getStation(currentStationId);
        if (currentStation == null) {
            return;
        }
        
        String category = currentStation.getCategory();
        List<Station> categoryStations = plugin.getStationManager().getStationsByCategory(category);
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Calculate inventory size (multiple of 9)
        int size = (int) Math.ceil(categoryStations.size() / 9.0) * 9;
        size = Math.max(27, size); // Minimum size of 27 (3 rows)
        
        String title = plugin.getLocaleManager().getMessage(locale, "gui.vending_machine.title");
        Inventory gui = Bukkit.createInventory(null, size, "§8" + title);
        
        // Add station tickets
        for (Station station : categoryStations) {
            // Skip current station
            if (station.getId().equals(currentStationId)) {
                continue;
            }
            
            ItemStack stationItem = new ItemStack(Material.PAPER);
            ItemMeta meta = stationItem.getItemMeta();
            
            if (meta != null) {
                String ticketName = plugin.getLocaleManager().getMessage(locale, "ticket.name");
                meta.setDisplayName("§6" + ticketName + " - " + station.getName());
                
                List<String> lore = new ArrayList<>();
                String categoryMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.category");
                lore.add("§7" + plugin.getLocaleManager().getMessage(locale, "gui.vending_machine.click_to_get"));
                lore.add("§7" + String.format(categoryMsg, currentStation.getCategory()));
                meta.setLore(lore);
                
                stationItem.setItemMeta(meta);
            }
            
            gui.addItem(stationItem);
        }
        
        // Add info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            String infoTitle = plugin.getLocaleManager().getMessage(locale, "gui.vending_machine.info");
            infoMeta.setDisplayName("§e" + infoTitle);
            
            List<String> lore = new ArrayList<>();
            String currentStationMsg = plugin.getLocaleManager().getMessage(locale, "gui.vending_machine.current_station");
            String categoryMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.category");
            lore.add("§7" + String.format(currentStationMsg, currentStation.getName()));
            lore.add("§7" + String.format(categoryMsg, currentStation.getCategory()));
            infoMeta.setLore(lore);
            
            infoItem.setItemMeta(infoMeta);
        }
        
        gui.setItem(size - 5, infoItem);
        
        player.openInventory(gui);
    }
    
    public static void giveTicket(SmartMetro plugin, Player player, String fromStationId, String toStationId) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        Station fromStation = plugin.getStationManager().getStation(fromStationId);
        Station toStation = plugin.getStationManager().getStation(toStationId);
        if (fromStation == null || toStation == null) {
            String errorMsg = plugin.getLocaleManager().getMessage(locale, "error.invalid_station");
            player.sendMessage("§c" + errorMsg);
            return;
        }
        
        // Check if stations are in the same category
        if (!fromStation.getCategory().equals(toStation.getCategory())) {
            String errorMsg = plugin.getLocaleManager().getMessage(locale, "error.different_category");
            player.sendMessage("§c" + errorMsg);
            return;
        }
        
        ItemStack ticket = Ticket.createTicket(plugin, player, fromStationId, toStationId, fromStation.getName(), toStation.getName());
        
        // Add ticket to player's inventory or drop it if inventory is full
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(ticket);
            String purchaseMsg = plugin.getLocaleManager().getMessage(locale, "ticket.received");
            player.sendMessage("§a" + String.format(purchaseMsg, toStation.getName()));
        } else {
            player.getWorld().dropItem(player.getLocation(), ticket);
            String inventoryFullMsg = plugin.getLocaleManager().getMessage(locale, "ticket.inventory_full");
            player.sendMessage("§a" + String.format(inventoryFullMsg, toStation.getName()));
        }
    }
    
    public static ItemStack createVendingMachine(SmartMetro plugin, Player player, String stationId) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        Station station = plugin.getStationManager().getStation(stationId);
        
        if (station == null) {
            return null;
        }
        
        // Create a player head item
        ItemStack vendingMachine = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) vendingMachine.getItemMeta();
        
        if (meta != null) {
            // Set the display name
            String machineName = plugin.getLocaleManager().getMessage(locale, "vending_machine.name");
            meta.setDisplayName("§6" + machineName);
            
            // Set lore
            List<String> lore = new ArrayList<>();
            String boundToMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.bound_to");
            String categoryMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.category");
            lore.add("§7" + String.format(boundToMsg, station.getName()));
            lore.add("§7" + String.format(categoryMsg, station.getCategory()));
            lore.add("§7" + plugin.getLocaleManager().getMessage(locale, "vending_machine.place_instruction"));
            meta.setLore(lore);
            
            // Store the station ID in the item's persistent data container
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(STATION_KEY, PersistentDataType.STRING, stationId);
            
            // Set the skin to CONSOLE or a custom texture
            try {
                // Try to set a custom skin if available
                String skinOwner = plugin.getConfigManager().getVendingMachineSkin();
                if (skinOwner != null && !skinOwner.isEmpty()) {
                    meta.setPlayerProfile(Bukkit.createProfile(skinOwner));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set vending machine skin: " + e.getMessage());
            }
            
            vendingMachine.setItemMeta(meta);
        }
        
        return vendingMachine;
    }
    
    public static String getBoundStation(SmartMetro plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        if (container.has(STATION_KEY, PersistentDataType.STRING)) {
            return container.get(STATION_KEY, PersistentDataType.STRING);
        }
        
        return null;
    }
}

