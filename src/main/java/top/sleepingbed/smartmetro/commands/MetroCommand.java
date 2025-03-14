package top.sleepingbed.smartmetro.commands;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Station;
import top.sleepingbed.smartmetro.models.TrackSwitch;
import top.sleepingbed.smartmetro.utils.VendingMachineUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;

public class MetroCommand implements CommandExecutor {
    
    private final SmartMetro plugin;
    
    public MetroCommand(SmartMetro plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c" + plugin.getLocaleManager().getMessage("en_US", "command.error.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        if (!player.hasPermission("metro.admin")) {
            player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_create"));
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("station")) {
                    if (args.length < 3) {
                        player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_create_station"));
                        return true;
                    }
                    
                    String category = "default";
                    if (args.length > 3) {
                        category = args[3];
                    }
                    
                    createStation(player, args[2], category);
                } else if (args[1].equalsIgnoreCase("switch")) {
                    if (args.length < 3) {
                        player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_create_switch"));
                        return true;
                    }
                    
                    createSwitch(player, args[2]);
                } else {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.invalid_type"));
                }
                break;
                
            case "delete":
                if (args.length < 2) {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_delete"));
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("station")) {
                    deleteStation(player);
                } else if (args[1].equalsIgnoreCase("switch")) {
                    deleteSwitch(player);
                } else {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.invalid_type"));
                }
                break;
                
            case "list":
                listStations(player);
                break;
                
            case "language":
            case "lang":
                if (args.length < 2) {
                    // List available languages
                    listLanguages(player);
                    return true;
                }
                
                plugin.getLocaleManager().setPlayerLocale(player, args[1]);
                String langMsg = plugin.getLocaleManager().getMessage(args[1], "command.language_changed");
                player.sendMessage("§a" + langMsg);
                break;
                
            case "givemachine":
                if (args.length < 2) {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_givemachine"));
                    return true;
                }
                
                giveVendingMachine(player, args[1]);
                break;
                
            case "setstation":
                if (args.length < 2) {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_setstation"));
                    return true;
                }
                
                setActivatorRailStation(player, args[1]);
                break;
                
            case "setcategory":
                if (args.length < 2) {
                    player.sendMessage("§c" + plugin.getLocaleManager().getMessage(locale, "command.error.usage_setcategory"));
                    return true;
                }
                
                setStationCategory(player, args[1]);
                break;
                
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        player.sendMessage("§6" + plugin.getLocaleManager().getMessage(locale, "command.help.title"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.create_station"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.create_switch"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.delete_station"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.delete_switch"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.list"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.language"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.givemachine"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.setstation"));
        player.sendMessage("§e" + plugin.getLocaleManager().getMessage(locale, "command.help.setcategory"));
    }
    
    private void createStation(Player player, String name, String category) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Generate a unique ID for the station
        String id = UUID.randomUUID().toString().substring(0, 8);
        
        // Create the station
        Station station = new Station(id, name, player.getLocation(), category);
        plugin.getStationManager().addStation(station);
        
        String createdMsg = plugin.getLocaleManager().getMessage(locale, "station.created_with_category", name, id, category);
        player.sendMessage("§a" + createdMsg);
    }
    
    private void createSwitch(Player player, String destinationId) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Check if the destination exists
        Station destination = plugin.getStationManager().getStation(destinationId);
        if (destination == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "switch.destination_not_found", destinationId);
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Create the track switch
        UUID id = UUID.randomUUID();
        TrackSwitch trackSwitch = new TrackSwitch(id, player.getLocation().getBlock().getLocation(), destinationId);
        plugin.getStationManager().addTrackSwitch(trackSwitch);
        
        String createdMsg = plugin.getLocaleManager().getMessage(locale, "switch.created", destination.getName());
        player.sendMessage("§a" + createdMsg);
    }
    
    private void deleteStation(Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Find the nearest station
        Station nearestStation = plugin.getStationManager().getNearestStation(player.getLocation(), 5);
        
        if (nearestStation == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "station.not_found");
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Delete the station
        plugin.getStationManager().removeStation(nearestStation.getId());
        String deletedMsg = plugin.getLocaleManager().getMessage(locale, "station.deleted", nearestStation.getName());
        player.sendMessage("§a" + deletedMsg);
    }
    
    private void deleteSwitch(Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Find the track switch at the player's location
        TrackSwitch trackSwitch = plugin.getStationManager().getTrackSwitchAtLocation(player.getLocation().getBlock().getLocation());
        
        if (trackSwitch == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "switch.not_found");
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Delete the track switch
        plugin.getStationManager().removeTrackSwitch(trackSwitch.getId());
        String deletedMsg = plugin.getLocaleManager().getMessage(locale, "switch.deleted");
        player.sendMessage("§a" + deletedMsg);
    }
    
    private void listStations(Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        String titleMsg = plugin.getLocaleManager().getMessage(locale, "station.list_title");
        player.sendMessage("§6" + titleMsg);
        
        for (Station station : plugin.getStationManager().getAllStations().values()) {
            player.sendMessage("§e" + station.getId() + " §7- §f" + station.getName() + " §7(Category: " + station.getCategory() + ")");
        }
    }
    
    private void listLanguages(Player player) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        String titleMsg = plugin.getLocaleManager().getMessage(locale, "command.language.available");
        player.sendMessage("§6" + titleMsg);
        
        Map<String, String> languages = plugin.getLocaleManager().getAvailableLocales();
        for (Map.Entry<String, String> entry : languages.entrySet()) {
            player.sendMessage("§e" + entry.getKey() + " §7- §f" + entry.getValue());
        }
        
        String usageMsg = plugin.getLocaleManager().getMessage(locale, "command.language.usage");
        player.sendMessage("§7" + usageMsg);
    }
    
    private void giveVendingMachine(Player player, String stationId) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Check if the station exists
        Station station = plugin.getStationManager().getStation(stationId);
        if (station == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "station.not_found_id", stationId);
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Create and give the vending machine item
        ItemStack vendingMachine = VendingMachineUtils.createVendingMachine(plugin, player, stationId);
        if (vendingMachine == null) {
            String errorMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.creation_failed");
            player.sendMessage("§c" + errorMsg);
            return;
        }
        
        // Add to player's inventory or drop it
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(vendingMachine);
            String givenMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.given", station.getName());
            player.sendMessage("§a" + givenMsg);
        } else {
            player.getWorld().dropItem(player.getLocation(), vendingMachine);
            String droppedMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.dropped", station.getName());
            player.sendMessage("§a" + droppedMsg);
        }
    }

    private void setActivatorRailStation(Player player, String stationId) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Check if the station exists
        Station station = plugin.getStationManager().getStation(stationId);
        if (station == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "station.not_found_id", stationId);
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() != Material.ACTIVATOR_RAIL) {
            String errorMsg = plugin.getLocaleManager().getMessage(locale, "command.error.not_activator_rail");
            player.sendMessage("§c" + errorMsg);
            return;
        }
        
        // Store the station ID for this rail location
        plugin.getRailManager().setStationRail(targetBlock.getLocation(), stationId);
        
        String successMsg = plugin.getLocaleManager().getMessage(locale, "command.setstation.success", station.getName());
        player.sendMessage("§a" + successMsg);
    }
    
    private void setStationCategory(Player player, String category) {
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        
        // Find the nearest station
        Station nearestStation = plugin.getStationManager().getNearestStation(player.getLocation(), 5);
        
        if (nearestStation == null) {
            String notFoundMsg = plugin.getLocaleManager().getMessage(locale, "station.not_found");
            player.sendMessage("§c" + notFoundMsg);
            return;
        }
        
        // Update the station's category
        String oldCategory = nearestStation.getCategory();
        nearestStation.setCategory(category);
        
        String updatedMsg = plugin.getLocaleManager().getMessage(locale, "station.category_updated", 
                nearestStation.getName(), oldCategory, category);
        player.sendMessage("§a" + updatedMsg);
    }
}

