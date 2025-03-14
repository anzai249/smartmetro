package top.sleepingbed.smartmetro.models;

import org.bukkit.Location;

import java.util.UUID;

public class TrackSwitch {
    
    private final UUID id;
    private Location location;
    private String destinationStation;
    
    public TrackSwitch(UUID id, Location location, String destinationStation) {
        this.id = id;
        this.location = location;
        this.destinationStation = destinationStation;
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
    
    public String getDestinationStation() {
        return destinationStation;
    }
    
    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }
}

