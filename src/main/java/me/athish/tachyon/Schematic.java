package me.athish.tachyon;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a schematic that can be copied, saved, loaded, and pasted.
 * The schematic stores blocks and their locations relative to an origin.
 */
@SuppressWarnings("all")
public class Schematic implements Serializable {
    private static final long serialVersionUID = 1L;
    // Set your custom schematic file extension here.
    private static final String FILE_EXTENSION = ".tachyon";
    private static final Kryo kryo = new Kryo();

    private Map<SerializableLocation, Material> blocks = new ConcurrentHashMap<>();
    private SerializableLocation origin;


    static {
        kryo.register(Schematic.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(SerializableLocation.class);
        kryo.register(Material.class);
    }


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

        // Create a list of all coordinates within the specified range
        List<int[]> coordinates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    coordinates.add(new int[]{x, y, z});
                }
            }
        }

        // Use parallelStream to process the coordinates in parallel
        coordinates.parallelStream().forEach(coord -> {
            Location loc = new Location(world, coord[0], coord[1], coord[2]);
            blocks.put(new SerializableLocation(loc), world.getBlockAt(loc).getType());
        });
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
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, this);
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
    public void load(File file) throws IOException {
        try (Input input = new Input(new FileInputStream(file))) {
            Schematic loadedSchematic = kryo.readObject(input, Schematic.class);
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
     * Rotates the schematic by a specified angle (in degrees) clockwise around the origin using matrix transformation
     * on the block locations for faster and more efficient rotation. Only angles that are multiples of 90 are allowed.
     * If the angle is not a multiple of 90, it will be rounded to the nearest multiple of 90.
     *
     * @param angle The angle of rotation in degrees.
     */
    public void rotate(double angle) {
        Map<SerializableLocation, Material> rotatedBlocks = new ConcurrentHashMap<>();
        Location originLoc = origin.toLocation();

        // normalize angle to be within the range [0, 360)
        angle = ((angle % 360) + 360) % 360;

        // round angle to nearest multiple of 90.
        int roundedAngle = (int) (Math.round(angle / 90.0) * 90);

        // get number of effective rotations. ie when we have to prevent rotating by 540 degrees when we can just rotate by 180 once.
        int effectiveRotations = roundedAngle / 90;

        blocks.entrySet().parallelStream().forEach(entry -> {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);

            double newX = loc.getX();
            double newZ = loc.getZ();

            // apply the appropriate matrix transformation based on the effective rotations
            switch (effectiveRotations) {
                case 1: // 90 degrees or 1 effective rotation
                    newX = -loc.getZ();
                    newZ = loc.getX();
                    break;
                case 2: // 180 degrees or 2 effective rotations
                    newX = -loc.getX();
                    newZ = -loc.getZ();
                    break;
                case 3: // 270 degrees or 3 effective rotations
                    newX = loc.getZ();
                    newZ = -loc.getX();
                    break;
                default: // no effective rotation
                    break;
            }

            loc.setX(newX);
            loc.setZ(newZ);
            loc.add(originLoc);

            rotatedBlocks.put(new SerializableLocation(loc), entry.getValue());
        });

        blocks = rotatedBlocks;
    }

    /**
     * Flips the schematic in the specified direction around the origin.
     *
     * @param direction The direction to flip. Valid values are "up", "down", "left", "right".
     */
    public void flip(String direction) {
        switch (direction.toLowerCase()) {
            case "up":
                flipUp();
                break;
            case "down":
                flipDown();
                break;
            case "left":
                flipLeft();
                break;
            case "right":
                flipRight();
                break;
            default:
                throw new IllegalArgumentException("Invalid flip direction: " + direction);
        }
    }

    /**
     * Flips the schematic upwards around the origin.
     */
    private void flipUp() {
        Map<SerializableLocation, Material> flippedBlocks = new ConcurrentHashMap<>();
        Location originLoc = origin.toLocation();

        blocks.entrySet().parallelStream().forEach(entry -> {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);

            double newY = -loc.getY();

            loc.setY(newY);
            loc.add(originLoc);

            flippedBlocks.put(new SerializableLocation(loc), entry.getValue());
        });

        blocks = flippedBlocks;
    }

    /**
     * Flips the schematic downwards around the origin.
     */
    private void flipDown() {
        Map<SerializableLocation, Material> flippedBlocks = new ConcurrentHashMap<>();
        Location originLoc = origin.toLocation();

        blocks.entrySet().parallelStream().forEach(entry -> {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);

            double newY = -loc.getY();

            loc.setY(newY);
            loc.add(originLoc);

            flippedBlocks.put(new SerializableLocation(loc), entry.getValue());
        });

        blocks = flippedBlocks;
    }

    /**
     * Flips the schematic to the left around the origin.
     */
    private void flipLeft() {
        Map<SerializableLocation, Material> flippedBlocks = new ConcurrentHashMap<>();
        Location originLoc = origin.toLocation();

        blocks.entrySet().parallelStream().forEach(entry -> {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);

            double newX = -loc.getX();

            loc.setX(newX);
            loc.add(originLoc);

            flippedBlocks.put(new SerializableLocation(loc), entry.getValue());
        });

        blocks = flippedBlocks;
    }

    /**
     * Flips the schematic to the right around the origin.
     */
    private void flipRight() {
        Map<SerializableLocation, Material> flippedBlocks = new ConcurrentHashMap<>();
        Location originLoc = origin.toLocation();

        blocks.entrySet().parallelStream().forEach(entry -> {
            Location loc = entry.getKey().toLocation();
            loc.subtract(originLoc);

            double newX = -loc.getX();

            loc.setX(newX);
            loc.add(originLoc);

            flippedBlocks.put(new SerializableLocation(loc), entry.getValue());
        });

        blocks = flippedBlocks;
    }

    /**
     * Replaces all blocks of a certain type inside the schematic.
     *
     * @param from The material to be replaced.
     * @param to   The material to replace with.
     */
    public void replaceBlocks(Material from, Material to) {
        blocks.entrySet().parallelStream().forEach(entry -> {
            if (entry.getValue() == from) {
                entry.setValue(to);
            }
        });
    }

    /**
     * Gets the number of blocks in the schematic.
     *
     * @return The number of blocks in the schematic.
     */
    public int getBlockCount() {
        return blocks.size();
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