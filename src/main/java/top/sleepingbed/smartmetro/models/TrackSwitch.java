package top.sleepingbed.smartmetro.models;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackSwitch {
    
    private final UUID id;
    private Location location;
    private Map<String, List<String>> directionDestinations; // Direction -> List of StationIds
    
    public TrackSwitch(UUID id, Location location) {
        this.id = id;
        this.location = location;
        this.directionDestinations = new HashMap<>();
    }
    
    public UUID getId() {
        return id;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public Map<String, List<String>> getDirectionDestinations() {
        return directionDestinations;
    }
    
    public void setDirectionDestinations(Map<String, List<String>> directionDestinations) {
        this.directionDestinations = directionDestinations;
    }
    
    public void addDestination(String direction, String stationId) {
        directionDestinations.computeIfAbsent(direction, k -> new ArrayList<>()).add(stationId);
    }
    
    public void removeDestination(String direction, String stationId) {
        if (directionDestinations.containsKey(direction)) {
            directionDestinations.get(direction).remove(stationId);
            // Remove the direction entry if there are no more destinations
            if (directionDestinations.get(direction).isEmpty()) {
                directionDestinations.remove(direction);
            }
        }
    }
    
    public List<String> getDestinations(String direction) {
        return directionDestinations.getOrDefault(direction, new ArrayList<>());
    }
    
    public boolean hasDirection(String direction) {
        return directionDestinations.containsKey(direction);
    }
    
    public boolean hasDestinationInDirection(String direction, String stationId) {
        return directionDestinations.containsKey(direction) && 
               directionDestinations.get(direction).contains(stationId);
    }
    
    public boolean hasDestinationStation(String stationId) {
        for (List<String> destinations : directionDestinations.values()) {
            if (destinations.contains(stationId)) {
                return true;
            }
        }
        return false;
    }
    
    public String getDirectionForDestination(String stationId) {
        for (Map.Entry<String, List<String>> entry : directionDestinations.entrySet()) {
            if (entry.getValue().contains(stationId)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // Convert Rail.Shape to a direction string
    public static String shapeToDirection(Rail.Shape shape) {
        switch (shape) {
            case NORTH_SOUTH:
                return "north_south";
            case EAST_WEST:
                return "east_west";
            case ASCENDING_EAST:
                return "ascending_east";
            case ASCENDING_WEST:
                return "ascending_west";
            case ASCENDING_NORTH:
                return "ascending_north";
            case ASCENDING_SOUTH:
                return "ascending_south";
            case SOUTH_EAST:
                return "south_east";
            case SOUTH_WEST:
                return "south_west";
            case NORTH_WEST:
                return "north_west";
            case NORTH_EAST:
                return "north_east";
            default:
                return "unknown";
        }
    }
    
    // Convert a direction string to Rail.Shape
    public static Rail.Shape directionToShape(String direction) {
        switch (direction) {
            case "north_south":
                return Rail.Shape.NORTH_SOUTH;
            case "east_west":
                return Rail.Shape.EAST_WEST;
            case "ascending_east":
                return Rail.Shape.ASCENDING_EAST;
            case "ascending_west":
                return Rail.Shape.ASCENDING_WEST;
            case "ascending_north":
                return Rail.Shape.ASCENDING_NORTH;
            case "ascending_south":
                return Rail.Shape.ASCENDING_SOUTH;
            case "south_east":
                return Rail.Shape.SOUTH_EAST;
            case "south_west":
                return Rail.Shape.SOUTH_WEST;
            case "north_west":
                return Rail.Shape.NORTH_WEST;
            case "north_east":
                return Rail.Shape.NORTH_EAST;
            default:
                return Rail.Shape.NORTH_SOUTH; // Default
        }
    }
    
    // Get possible exit directions from a rail shape
    public static String[] getPossibleExitDirections(Rail.Shape currentShape) {
        switch (currentShape) {
            case NORTH_SOUTH:
                return new String[]{"north", "south"};
            case EAST_WEST:
                return new String[]{"east", "west"};
            case ASCENDING_EAST:
                return new String[]{"east", "west"};
            case ASCENDING_WEST:
                return new String[]{"east", "west"};
            case ASCENDING_NORTH:
                return new String[]{"north", "south"};
            case ASCENDING_SOUTH:
                return new String[]{"north", "south"};
            case SOUTH_EAST:
                return new String[]{"south", "east"};
            case SOUTH_WEST:
                return new String[]{"south", "west"};
            case NORTH_WEST:
                return new String[]{"north", "west"};
            case NORTH_EAST:
                return new String[]{"north", "east"};
            default:
                return new String[]{};
        }
    }
    
    // Get the appropriate rail shape for a given direction
    public static Rail.Shape getShapeForDirection(String entryDirection, String exitDirection) {
        if (entryDirection.equals("north")) {
            if (exitDirection.equals("south")) return Rail.Shape.NORTH_SOUTH;
            if (exitDirection.equals("east")) return Rail.Shape.NORTH_EAST;
            if (exitDirection.equals("west")) return Rail.Shape.NORTH_WEST;
        } else if (entryDirection.equals("south")) {
            if (exitDirection.equals("north")) return Rail.Shape.NORTH_SOUTH;
            if (exitDirection.equals("east")) return Rail.Shape.SOUTH_EAST;
            if (exitDirection.equals("west")) return Rail.Shape.SOUTH_WEST;
        } else if (entryDirection.equals("east")) {
            if (exitDirection.equals("west")) return Rail.Shape.EAST_WEST;
            if (exitDirection.equals("north")) return Rail.Shape.NORTH_EAST;
            if (exitDirection.equals("south")) return Rail.Shape.SOUTH_EAST;
        } else if (entryDirection.equals("west")) {
            if (exitDirection.equals("east")) return Rail.Shape.EAST_WEST;
            if (exitDirection.equals("north")) return Rail.Shape.NORTH_WEST;
            if (exitDirection.equals("south")) return Rail.Shape.SOUTH_WEST;
        }
        
        // Default fallback
        return Rail.Shape.NORTH_SOUTH;
    }
    
    // Get the opposite direction
    public static String getOppositeDirection(String direction) {
        switch (direction) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            default: return direction;
        }
    }
}

