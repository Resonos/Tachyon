package me.athish.tachyon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a schematic that can be copied, saved, loaded, and pasted.
 * The schematic stores blocks and their locations relative to an origin.
 */
public class Schematic implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILE_EXTENSION = ".tachyon";

    private Map<SerializableLocation, Material> blocks = new HashMap<>();
    private SerializableLocation origin;

    /**
     * Creates a new Schematic by copying blocks between two locations.
     *
     * @param start  The starting location of the area to copy.
     * @param end    The ending location of the area to copy.
     * @param origin The origin location for the schematic.
     */
    private Schematic(Location start, Location end, Location origin) {
        copyBlocks(start, end, origin);
    }

    /**
     * Loads a Schematic from a file.
     *
     * @param file The file to load the schematic from.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    private Schematic(File file) throws IOException, ClassNotFoundException {
        load(file);
    }

    /**
     * Copies blocks between two locations and stores them in the schematic.
     *
     * @param start  The starting location of the area to copy.
     * @param end    The ending location of the area to copy.
     * @param origin The origin location for the schematic.
     */
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

    /**
     * Pastes the schematic at a given location synchronously.
     *
     * @param pasteLocation The location to paste the schematic.
     * @param ignoreAir     Whether to ignore air blocks when pasting.
     */
    public void pasteSync(Location pasteLocation, boolean ignoreAir) {
        Location originLoc = origin.toLocation();
        for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
            if (ignoreAir && entry.getValue() == Material.AIR) continue;

            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);
            loc.add(pasteLocation);
            BlockChanger.setSectionBlock(loc, entry.getValue());
        }
    }

    /**
     * Pastes the schematic at a given location asynchronously.
     *
     * @param pasteLocation The location to paste the schematic.
     * @param ignoreAir     Whether to ignore air blocks when pasting.
     * @return A CompletableFuture that completes when the paste operation is done.
     */
    public CompletableFuture<Void> pasteAsync(Location pasteLocation, boolean ignoreAir) {
        return CompletableFuture.runAsync(() -> {
            Location originLoc = origin.toLocation();
            for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
                if (ignoreAir && entry.getValue() == Material.AIR) continue;

                Location loc = entry.getKey().toLocation();
                loc.subtract(originLoc);
                loc.add(pasteLocation);
                BlockChanger.setSectionBlockAsynchronously(loc, new ItemStack(entry.getValue()), false);
            }
        });
    }

    /**
     * Saves the schematic to a file.
     *
     * @param file The file to save the schematic to.
     * @throws IOException If an I/O error occurs.
     */
    public void save(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    /**
     * Saves the schematic to a file asynchronously.
     *
     * @param file The file to save the schematic to.
     * @return A CompletableFuture that completes when the save operation is done.
     */
    public CompletableFuture<Void> saveAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            try {
                save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Loads the schematic from a file.
     *
     * @param file The file to load the schematic from.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public void load(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Schematic loadedSchematic = (Schematic) ois.readObject();
            this.blocks = loadedSchematic.blocks;
            this.origin = loadedSchematic.origin;
        }
    }

    /**
     * Creates a new Schematic by copying blocks between two locations.
     *
     * @param start  The starting location of the area to copy.
     * @param end    The ending location of the area to copy.
     * @param origin The origin location for the schematic.
     * @return The created Schematic.
     */
    public static Schematic create(Location start, Location end, Location origin) {
        return new Schematic(start, end, origin);
    }

    /**
     * Creates a new Schematic by copying blocks between two locations asynchronously.
     *
     * @param start  The starting location of the area to copy.
     * @param end    The ending location of the area to copy.
     * @param origin The origin location for the schematic.
     * @return A CompletableFuture that completes with the created Schematic.
     */
    public static CompletableFuture<Schematic> createAsync(Location start, Location end, Location origin) {
        return CompletableFuture.supplyAsync(() -> new Schematic(start, end, origin));
    }

    /**
     * Loads a Schematic from a file.
     *
     * @param file The file to load the schematic from.
     * @return The loaded Schematic.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static Schematic create(File file) throws IOException, ClassNotFoundException {
        return new Schematic(file);
    }

    /**
     * Loads a Schematic from a file asynchronously.
     *
     * @param file The file to load the schematic from.
     * @return A CompletableFuture that completes with the loaded Schematic.
     */
    public static CompletableFuture<Schematic> createAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new Schematic(file);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Gets the file extension for schematic files.
     *
     * @return The file extension for schematic files.
     */
    public static String getFileExtension() {
        return FILE_EXTENSION;
    }
}