package top.sleepingbed.smartmetro.listeners;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Station;
import top.sleepingbed.smartmetro.utils.VendingMachineUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class VendingMachineListener implements Listener {

    private final SmartMetro plugin;

    public VendingMachineListener(SmartMetro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getHand() != EquipmentSlot.HAND ||
                event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }

        // Check if this is a vending machine
        BlockState state = block.getState();
        if (!(state instanceof Skull)) {
            return;
        }

        // Get the bound station from the block's persistent data container
        String stationId = null;

        // Check if this is a placed vending machine with station data
        if (state instanceof PersistentDataHolder) {
            PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
            if (container.has(new NamespacedKey(plugin, "bound_station"), PersistentDataType.STRING)) {
                stationId = container.get(new NamespacedKey(plugin, "bound_station"), PersistentDataType.STRING);
            }
        }

        // Legacy check for old vending machines or manually placed heads
        if (stationId == null) {
            Skull skull = (Skull) state;
            if (!isVendingMachine(skull)) {
                return;
            }

            // Find the nearest station for legacy vending machines
            Station nearestStation = findNearestStation(block);
            if (nearestStation == null) {
                String locale = plugin.getLocaleManager().getPlayerLocale(event.getPlayer());
                String errorMsg = plugin.getLocaleManager().getMessage(locale, "error.no_station_near_vending");
                event.getPlayer().sendMessage("§c" + errorMsg);
                return;
            }

            stationId = nearestStation.getId();
        }

        if (stationId == null) {
            return;
        }

        // Open vending machine GUI
        event.setCancelled(true);
        VendingMachineUtils.openVendingMachineGUI(plugin, event.getPlayer(), stationId);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // Check if the player is placing a vending machine
        String stationId = VendingMachineUtils.getBoundStation(plugin, item);
        if (stationId == null) {
            return;
        }

        // Store the station ID in the block's persistent data container
        Block block = event.getBlockPlaced();
        BlockState state = block.getState();

        if (state instanceof PersistentDataHolder) {
            PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "bound_station"), PersistentDataType.STRING, stationId);
            state.update();
        }

        // Inform the player
        Player player = event.getPlayer();
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        Station station = plugin.getStationManager().getStation(stationId);

        if (station != null) {
            String placedMsg = plugin.getLocaleManager().getMessage(locale, "vending_machine.placed");
            player.sendMessage("§a" + String.format(placedMsg, station.getName()));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        String guiTitle = plugin.getLocaleManager().getMessage(locale, "gui.vending_machine.title");

        if (!event.getView().getTitle().equals("§8" + guiTitle)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.PAPER) {
            return;
        }

        // Find the bound station for this vending machine
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            return;
        }

        String stationId = null;

        // Check if this is a placed vending machine with station data
        BlockState state = block.getState();
        if (state != null && state instanceof PersistentDataHolder) {
            PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
            if (container.has(new NamespacedKey(plugin, "bound_station"), PersistentDataType.STRING)) {
                stationId = container.get(new NamespacedKey(plugin, "bound_station"), PersistentDataType.STRING);
            }
        }

        // Legacy fallback
        if (stationId == null) {
            Station nearestStation = findNearestStation(block);
            if (nearestStation == null) {
                String errorMsg = plugin.getLocaleManager().getMessage(locale, "error.no_station_near_vending");
                player.sendMessage("§c" + errorMsg);
                return;
            }
            stationId = nearestStation.getId();
        }

        // Parse the destination station from the item name
        String itemName = clickedItem.getItemMeta().getDisplayName();
        String ticketName = "§6" + plugin.getLocaleManager().getMessage(locale, "ticket.name") + " - ";

        if (!itemName.startsWith(ticketName)) {
            return;
        }


        String destinationName = itemName.substring(ticketName.length()); // Remove prefix

        // Find the destination station by name
        String destinationId = null;
        for (Station station : plugin.getStationManager().getAllStations().values()) {
            if (station.getName().equals(destinationName)) {
                destinationId = station.getId();
                break;
            }
        }

        if (destinationId == null) {
            String errorMsg = plugin.getLocaleManager().getMessage(locale, "error.destination_not_found");
            player.sendMessage("§c" + errorMsg);
            return;
        }

        // Give the ticket to the player
        VendingMachineUtils.giveTicket(plugin, player, stationId, destinationId);
        player.closeInventory();
    }

    private boolean isVendingMachine(Skull skull) {
        // Check if the skull has the CONSOLE skin
        // This is a simplified check - in a real plugin, you'd need to check the texture
        return skull.getPlayerProfile() != null &&
                skull.getPlayerProfile().getName() != null &&
                skull.getPlayerProfile().getName().equals(plugin.getConfigManager().getVendingMachineSkin());
    }

    private Station findNearestStation(Block block) {
        // Find the nearest station to this block
        // In a real plugin, you might want to store station locations in a spatial index
        Station nearestStation = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Station station : plugin.getStationManager().getAllStations().values()) {
            if (station.getLocation().getWorld().equals(block.getWorld())) {
                double distance = station.getLocation().distanceSquared(block.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestStation = station;
                }
            }
        }

        // Only return if within a reasonable distance (e.g., 25 blocks)
        if (nearestDistance <= 625) { // 25^2
            return nearestStation;
        }

        return null;
    }
}

