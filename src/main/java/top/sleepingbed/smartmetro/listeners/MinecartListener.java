package top.sleepingbed.smartmetro.listeners;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.TrackSwitch;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;

import org.bukkit.entity.Player;
import top.sleepingbed.smartmetro.models.Station;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinecartListener implements Listener {

    private final SmartMetro plugin;
    private final Map<UUID, Map<String, Long>> lastStationNotifications = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN = 10000; // 10 seconds in milliseconds

    public MinecartListener(SmartMetro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();

        // Only handle minecarts
        if (!(vehicle instanceof Minecart)) {
            return;
        }

        Minecart minecart = (Minecart) vehicle;

        // Check if the minecart has a destination
        if (!minecart.hasMetadata("metro_destination")) {
            return;
        }

        String destination = minecart.getMetadata("metro_destination").get(0).asString();

        // Get the block the minecart is on
        Location location = minecart.getLocation();
        Block block = location.getBlock();

        // Check if the block is a rail
        if (!isRail(block.getType())) {
            return;
        }

        // Check if there's a track switch at this location
        TrackSwitch trackSwitch = plugin.getStationManager().getTrackSwitchAtLocation(block.getLocation());

        if (trackSwitch != null) {
            // This is a track switch point
            handleTrackSwitch(minecart, block, trackSwitch, destination);
        }

        // Check if this is an activator rail with a station
        if (block.getType() == Material.ACTIVATOR_RAIL) {
            handleActivatorRail(minecart, block);
        }

        // Check if we're near a station to notify the player
        if (!minecart.getPassengers().isEmpty() && minecart.getPassengers().get(0) instanceof Player) {
            Player passenger = (Player) minecart.getPassengers().get(0);
            Station nearbyStation = plugin.getStationManager().getNearestStation(location, 10);

            if (nearbyStation != null) {
                // Don't notify if this is the destination station (that's handled by the activator rail)
                if (!nearbyStation.getId().equals(destination)) {
                    notifyPassingStation(passenger, nearbyStation);
                }
            }
        }
    }

    @EventHandler
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        // Check if this is a metro minecart
        if (!(event.getVehicle() instanceof Minecart)) {
            return;
        }

        Minecart minecart = (Minecart) event.getVehicle();

        // If this is a metro minecart, cancel the collision
        if (minecart.hasMetadata("metro_minecart")) {
            event.setCancelled(true);
        }
    }

    private void notifyPassingStation(Player player, Station station) {
        UUID playerId = player.getUniqueId();
        String stationId = station.getId();
        String locale = plugin.getLocaleManager().getPlayerLocale(player);

        // Initialize the player's notification map if it doesn't exist
        lastStationNotifications.putIfAbsent(playerId, new HashMap<>());
        Map<String, Long> playerNotifications = lastStationNotifications.get(playerId);

        // Check if we've recently notified about this station
        long currentTime = System.currentTimeMillis();
        if (!playerNotifications.containsKey(stationId) ||
                currentTime - playerNotifications.get(stationId) > NOTIFICATION_COOLDOWN) {

            // Send notification
            String passingMsg = plugin.getLocaleManager().getMessage(locale, "minecart.passing_station");
            player.sendMessage("§7" + String.format(passingMsg, station.getName()));

            // Update the last notification time
            playerNotifications.put(stationId, currentTime);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Vehicle vehicle = event.getVehicle();

        // Only handle minecarts
        if (!(vehicle instanceof Minecart)) {
            return;
        }

        Minecart minecart = (Minecart) vehicle;

        // Check if this is a metro minecart (has the destination metadata)
        if (minecart.hasMetadata("metro_destination")) {
            // Schedule the minecart to be removed on the next tick
            // This prevents issues with removing the entity during the event
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                minecart.remove();
            });
        }
    }

    private void handleActivatorRail(Minecart minecart, Block block) {
        // Check if the minecart has a destination and a passenger
        if (!minecart.hasMetadata("metro_destination") || minecart.getPassengers().isEmpty() ||
                !(minecart.getPassengers().get(0) instanceof Player)) {
            return;
        }

        Player passenger = (Player) minecart.getPassengers().get(0);
        String locale = plugin.getLocaleManager().getPlayerLocale(passenger);

        // Get the destination station ID
        String destinationId = minecart.getMetadata("metro_destination").get(0).asString();

        // Check if this activator rail is associated with a station
        String stationId = plugin.getRailManager().getStationAtRail(block.getLocation());
        if (stationId == null) {
            return;
        }

        // If this is the destination station, eject the passenger and destroy the minecart
        if (stationId.equals(destinationId)) {
            Station station = plugin.getStationManager().getStation(stationId);
            if (station != null) {
                String arrivalMsg = plugin.getLocaleManager().getMessage(locale, "minecart.arrived");
                passenger.sendMessage("§a" + String.format(arrivalMsg, station.getName()));
            }

            // Eject the passenger
            minecart.eject();

            // Remove the minecart
            minecart.remove();
        }
        // If not the destination, the minecart continues on its way
    }

    private void handleTrackSwitch(Minecart minecart, Block railBlock, TrackSwitch trackSwitch, String destination) {
        // If this track switch is for our destination, take the switch path
        if (trackSwitch.getDestinationStation().equals(destination)) {
            // Set the rail shape to direct the minecart to the correct path
            if (railBlock.getBlockData() instanceof Rail) {
                Rail rail = (Rail) railBlock.getBlockData();

                // Determine the best shape based on the current direction and desired destination
                Rail.Shape bestShape = determineBestShape(minecart, rail, trackSwitch);

                if (bestShape != null && bestShape != rail.getShape()) {
                    rail.setShape(bestShape);
                    railBlock.setBlockData(rail);
                }
            }
        }
    }

    private Rail.Shape determineBestShape(Minecart minecart, Rail rail, TrackSwitch trackSwitch) {
        // This is a simplified implementation

        // Get the current velocity direction
        Vector velocity = minecart.getVelocity();
        double absX = Math.abs(velocity.getX());
        double absZ = Math.abs(velocity.getZ());

        // Determine primary direction (north-south or east-west)
        boolean isNorthSouth = absZ > absX;

        // Get the location of the destination station
        Location destination = plugin.getStationManager()
                .getStation(trackSwitch.getDestinationStation())
                .getLocation();

        // Calculate direction to destination
        double dx = destination.getX() - minecart.getLocation().getX();
        double dz = destination.getZ() - minecart.getLocation().getZ();

        // Determine which way to turn based on current direction and destination
        if (isNorthSouth) {
            // Currently going north-south
            if (Math.abs(dx) > 5) { // If destination is significantly east or west
                if (velocity.getZ() > 0) { // Going south
                    return dx > 0 ? Rail.Shape.SOUTH_EAST : Rail.Shape.SOUTH_WEST;
                } else { // Going north
                    return dx > 0 ? Rail.Shape.NORTH_EAST : Rail.Shape.NORTH_WEST;
                }
            }
        } else {
            // Currently going east-west
            if (Math.abs(dz) > 5) { // If destination is significantly north or south
                if (velocity.getX() > 0) { // Going east
                    return dz > 0 ? Rail.Shape.SOUTH_EAST : Rail.Shape.NORTH_EAST;
                } else { // Going west
                    return dz > 0 ? Rail.Shape.SOUTH_WEST : Rail.Shape.NORTH_WEST;
                }
            }
        }

        // Default: keep current shape
        return rail.getShape();
    }

    private boolean isRail(Material material) {
        return material == Material.RAIL ||
                material == Material.POWERED_RAIL ||
                material == Material.DETECTOR_RAIL ||
                material == Material.ACTIVATOR_RAIL;
    }
}

