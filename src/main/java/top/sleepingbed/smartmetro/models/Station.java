package top.sleepingbed.smartmetro.models;

import org.bukkit.Location;

public class Station {
    
    private final String id;
    private String name;
    private Location location;
    private String category;
    
    public Station(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.category = "default"; // Default category
    }
    
    public Station(String id, String name, Location location, String category) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.category = category;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}

