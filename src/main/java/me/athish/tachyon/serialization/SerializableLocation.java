package me.athish.tachyon.serialization;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;

public class SerializableLocation {
    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    // no-arg constructor for Kryo
    public SerializableLocation() {
    }

    public SerializableLocation(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + worldName);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}