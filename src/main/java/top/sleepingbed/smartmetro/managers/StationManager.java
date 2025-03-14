package top.sleepingbed.smartmetro.managers;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Station;
import top.sleepingbed.smartmetro.models.TrackSwitch;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class StationManager {
    
    private final SmartMetro plugin;
    private final Map<String, Station> stations;
    private final Map<UUID, TrackSwitch> trackSwitches;
    
    public StationManager(SmartMetro plugin) {
        this.plugin = plugin;
        this.stations = new HashMap<>();
        this.trackSwitches = new HashMap<>();
        
        loadStations();
    }
    
    public void loadStations() {
        FileConfiguration config = plugin.getConfigManager().getStationsConfig();
        ConfigurationSection stationsSection = config.getConfigurationSection("stations");
        
        if (stationsSection != null) {
            for (String stationId : stationsSection.getKeys(false)) {
                ConfigurationSection stationSection = stationsSection.getConfigurationSection(stationId);
                if (stationSection != null) {
                    String name = stationSection.getString("name");
                    Location location = (Location) stationSection.get("location");
                    String category = stationSection.getString("category", "default");
                    
                    Station station = new Station(stationId, name, location, category);
                    stations.put(stationId, station);
                }
            }
        }
        
        ConfigurationSection switchesSection = config.getConfigurationSection("track-switches");
        if (switchesSection != null) {
            for (String switchId : switchesSection.getKeys(false)) {
                ConfigurationSection switchSection = switchesSection.getConfigurationSection(switchId);
                if (switchSection != null) {
                    UUID uuid = UUID.fromString(switchId);
                    Location location = (Location) switchSection.get("location");
                    String destinationStation = switchSection.getString("destination");
                    
                    TrackSwitch trackSwitch = new TrackSwitch(uuid, location, destinationStation);
                    trackSwitches.put(uuid, trackSwitch);
                }
            }
        }
    }
    
    public void saveStations() {
        FileConfiguration config = plugin.getConfigManager().getStationsConfig();
        
        // Clear existing data
        config.set("stations", null);
        config.set("track-switches", null);
        
        // Save stations
        for (Station station : stations.values()) {
            String path = "stations." + station.getId();
            config.set(path + ".name", station.getName());
            config.set(path + ".location", station.getLocation());
            config.set(path + ".category", station.getCategory());
        }
        
        // Save track switches
        for (TrackSwitch trackSwitch : trackSwitches.values()) {
            String path = "track-switches." + trackSwitch.getId().toString();
            config.set(path + ".location", trackSwitch.getLocation());
            config.set(path + ".destination", trackSwitch.getDestinationStation());
        }
        
        plugin.getConfigManager().saveStationsConfig(config);
    }
    
    public void addStation(Station station) {
        stations.put(station.getId(), station);
    }
    
    public void removeStation(String stationId) {
        stations.remove(stationId);
    }
    
    public Station getStation(String stationId) {
        return stations.get(stationId);
    }
    
    public Map<String, Station> getAllStations() {
        return stations;
    }
    
    public List<Station> getStationsByCategory(String category) {
        List<Station> result = new ArrayList<>();
        for (Station station : stations.values()) {
            if (station.getCategory().equals(category)) {
                result.add(station);
            }
        }
        return result;
    }
    
    public Set<String> getAllCategories() {
        Set<String> categories = new HashSet<>();
        for (Station station : stations.values()) {
            categories.add(station.getCategory());
        }
        if (categories.isEmpty()) {
            categories.add("default");
        }
        return categories;
    }
    
    public Station getNearestStation(Location location, double maxDistance) {
        Station nearestStation = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Station station : stations.values()) {
            if (station.getLocation().getWorld().equals(location.getWorld())) {
                double distance = station.getLocation().distanceSquared(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestStation = station;
                }
            }
        }
        
        if (nearestStation != null && nearestDistance <= maxDistance * maxDistance) {
            return nearestStation;
        }
        
        return null;
    }
    
    public void addTrackSwitch(TrackSwitch trackSwitch) {
        trackSwitches.put(trackSwitch.getId(), trackSwitch);
    }
    
    public void removeTrackSwitch(UUID switchId) {
        trackSwitches.remove(switchId);
    }
    
    public TrackSwitch getTrackSwitch(UUID switchId) {
        return trackSwitches.get(switchId);
    }
    
    public TrackSwitch getTrackSwitchAtLocation(Location location) {
        for (TrackSwitch trackSwitch : trackSwitches.values()) {
            if (trackSwitch.getLocation().getWorld().equals(location.getWorld()) &&
                    trackSwitch.getLocation().getBlockX() == location.getBlockX() &&
                    trackSwitch.getLocation().getBlockY() == location.getBlockY() &&
                    trackSwitch.getLocation().getBlockZ() == location.getBlockZ()) {
                return trackSwitch;
            }
        }
        return null;
    }
}

