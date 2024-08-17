package me.athish.tachyon;

import me.athish.tachyon.block.BlockChanger;
import me.athish.tachyon.serialization.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Schematic implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Map<UUID, Schematic> playerSchematics = new HashMap<>();
    private static final String FILE_EXTENSION = ".tachyon";

    private Map<SerializableLocation, Material> blocks = new HashMap<>();
    private SerializableLocation origin;


    public Schematic(Map<SerializableLocation, Material> blocks, SerializableLocation origin) {
        this.blocks = blocks;
        this.origin = origin;
    }

    public Schematic(Location start, Location end) {
        copyBlocks(start, end, start);
    }

    public Schematic(Location start, Location end, Location origin) {
        copyBlocks(start, end, origin);
    }

    private void copyBlocks(Location start, Location end, Location origin) {
        World world = start.getWorld();
        int minX = Math.min(start.getBlockX(), end.getBlockX());
        int minY = Math.min(start.getBlockY(), end.getBlockY());
        int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
        int maxX = Math.max(start.getBlockX(), end.getBlockX());
        int maxY = Math.max(start.getBlockY(), end.getBlockY());
        int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

        this.origin = new SerializableLocation(origin);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    blocks.put(new SerializableLocation(loc), world.getBlockAt(loc).getType());
                }
            }
        }
    }

    public void setOrigin(Location origin) {
        this.origin = new SerializableLocation(origin);
    }

    public void paste(Location pasteLocation) {
        Location originLoc = origin.toLocation();
        for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);
            loc.add(pasteLocation);
            BlockChanger.setSectionBlock(loc, entry.getValue());
        }
    }

    public void pasteAsync(Location pasteLocation) {
        Location originLoc = origin.toLocation();
        for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);
            loc.add(pasteLocation);
            BlockChanger.setSectionBlockAsynchronously(loc, new ItemStack(entry.getValue()), false);
        }
    }

    public static void setPlayerSchematic(UUID playerUUID, Schematic schematic) {
        playerSchematics.put(playerUUID, schematic);
    }

    public static Schematic getPlayerSchematic(UUID playerUUID) {
        return playerSchematics.get(playerUUID);
    }

    public void serialize(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
    }

    public static Schematic deserialize(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        return (Schematic) ois.readObject();
    }

    public static String getFileExtension() {
        return FILE_EXTENSION;
    }
}