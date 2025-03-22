package top.sleepingbed.smartmetro.listeners;

import org.bukkit.scheduler.BukkitRunnable;
import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.TrackSwitch;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class MinecartListener implements Listener {
    
    private final SmartMetro plugin;
    private final Map<UUID, Map<String, Long>> lastStationNotifications = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN = 10000; // 10 seconds in milliseconds
    private final Set<UUID> switchedMinecarts = new HashSet<>();
    
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

    private void handleTrackSwitch(Minecart minecart, Block railBlock, TrackSwitch trackSwitch, String destinationId) {
        // 只有當方塊是鐵軌並且包含形狀數據時才處理
        if (!(railBlock.getBlockData() instanceof Rail)) {
            return;
        }

        UUID cartId = minecart.getUniqueId();
        if (switchedMinecarts.contains(cartId)) {
            return; // 如果這輛礦車已經變軌，則不再處理
        }

        Rail rail = (Rail) railBlock.getBlockData();

        // 獲取礦車進入方向
        Vector velocity = minecart.getVelocity();
        String entryDirection = getDirectionFromVelocity(velocity);

        // 查找應該切換的出口方向
        String exitDirection = findExitDirectionForDestination(trackSwitch, entryDirection, destinationId);

        if (exitDirection != null) {
            // 設置鐵軌形狀
            Rail.Shape newShape = TrackSwitch.getShapeForDirection(entryDirection, exitDirection);

            if (newShape != rail.getShape()) {
                rail.setShape(newShape);
                railBlock.setBlockData(rail);

                // 記錄這輛礦車已經變軌
                switchedMinecarts.add(cartId);

                // 設置一個延遲清除機制，確保過一段時間後可以再次變軌
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        switchedMinecarts.remove(cartId);
                    }
                }.runTaskLater(plugin, 10L); // 20 ticks = 1 秒
            }
        }
    }

    private String findExitDirectionForDestination(TrackSwitch trackSwitch, String entryDirection, String destinationId) {
        // 檢查每個方向的目標站點
//        for (Map.Entry<String, List<String>> entry : trackSwitch.getDirectionDestinations().entrySet()) {
//            String direction = entry.getKey();
//            List<String> stationIds = entry.getValue(); // 修正此處，將 stationId 改為 stationIds (List)
//
//            // 跳過入口方向（不能往回走）
//            if (direction.equals(TrackSwitch.getOppositeDirection(entryDirection))) {
//                continue;
//            }
//
//            // 如果此方向包含目標站點，則選擇這個方向
//            if (stationIds.contains(destinationId)) { // 修正此處，檢查 List 是否包含目標站點
//                return direction;
//            }
//        }

        String directionToFind = trackSwitch.getDirectionForDestination(destinationId);
        if (directionToFind != null) {
            return directionToFind;
        }

        // 如果沒有找到特定的方向，則嘗試直行（如果可能）
        String straightDirection = TrackSwitch.getOppositeDirection(entryDirection);
        if (trackSwitch.hasDestinationStation(straightDirection)) {
            return straightDirection;
        }

        // 如果無法直行，則選擇第一個可用的出口（非入口方向）
        for (String direction : trackSwitch.getDirectionDestinations().keySet()) {
            if (!direction.equals(TrackSwitch.getOppositeDirection(entryDirection))) {
                return direction;
            }
        }

        // 沒有可用的出口
        return null;
    }

    private String getDirectionFromVelocity(Vector velocity) {
        double absX = Math.abs(velocity.getX());
        double absZ = Math.abs(velocity.getZ());
        
        if (absX > absZ) {
            // Moving primarily east-west
            return velocity.getX() > 0 ? "west" : "east"; // Opposite because we want where it's coming FROM
        } else {
            // Moving primarily north-south
            return velocity.getZ() > 0 ? "north" : "south"; // Opposite because we want where it's coming FROM
        }
    }
    
    private boolean isRail(Material material) {
        return material == Material.RAIL || 
               material == Material.POWERED_RAIL || 
               material == Material.DETECTOR_RAIL || 
               material == Material.ACTIVATOR_RAIL;
    }
}

