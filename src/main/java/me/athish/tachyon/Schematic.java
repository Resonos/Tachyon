package me.athish.tachyon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Represents a schematic that can be copied, saved, loaded, and pasted.
 * The schematic stores blocks and their locations relative to an origin.
 */
@SuppressWarnings("all")
public class Schematic {
    // Set your custom schematic file extension here.
    private static final String FILE_EXTENSION = ".tachyon";

    private Map<SerializableLocation, Material> blocks = new ConcurrentHashMap<>();
    private SerializableLocation origin;
    private SerializableLocation min;
    private SerializableLocation max;

    /**
     * Creates a new Schematic by copying blocks between two locations.
     *
     * @param start  The starting location of the area to copy.
     * @param end    The ending location of the area to copy.
     * @param origin The origin location for the schematic.
     */
    private Schematic(Location start, Location end, Location origin) {
        this.min = new SerializableLocation(start);
        this.max = new SerializableLocation(end);
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
            loc.setWorld(pasteLocation.getWorld());
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
                loc.setWorld(pasteLocation.getWorld());
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
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))))) {
            // Write world name, pitch, and yaw once
            writer.write(origin.getWorldName() + ",");
            writer.write((int) origin.getYaw() + ",");
            writer.write((int) origin.getPitch() + ",");

            // Write origin coordinates
            writer.write((int) origin.getX() + ",");
            writer.write((int) origin.getY() + ",");
            writer.write((int) origin.getZ() + ",");

            // Write cuboid bounds
            int cuboidMinX = (int) Math.min(min.getX(), max.getX());
            int cuboidMinY = (int) Math.min(min.getY(), max.getY());
            int cuboidMinZ = (int) Math.min(min.getZ(), max.getZ());
            int cuboidMaxX = (int) Math.max(min.getX(), max.getX());
            int cuboidMaxY = (int) Math.max(min.getY(), max.getY());
            int cuboidMaxZ = (int) Math.max(min.getZ(), max.getZ());

            writer.write(cuboidMinX + ",");
            writer.write(cuboidMinY + ",");
            writer.write(cuboidMinZ + ",");
            writer.write(cuboidMaxX + ",");
            writer.write(cuboidMaxY + ",");
            writer.write(cuboidMaxZ + ",");

            // Write blocks grouped by material
            Map<Material, List<Location>> materialBlocks = new HashMap<>();
            Location originLoc = origin.toLocation();
            for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
                if (entry.getValue() != Material.AIR) {
                    materialBlocks.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                            .add(entry.getKey().toLocation().subtract(originLoc));
                }
            }

            writer.write(materialBlocks.size() + ",");
            for (Map.Entry<Material, List<Location>> entry : materialBlocks.entrySet()) {
                writer.write(entry.getKey().name() + ",");
                writer.write(entry.getValue().size() + ",");
                for (Location loc : entry.getValue()) {
                    writer.write((int) loc.getX() + "," + (int) loc.getY() + "," + (int) loc.getZ() + ",");
                    writer.write((int) loc.getYaw() + "," + (int) loc.getPitch() + ",");
                }
            }
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))))) {
            String[] data = reader.readLine().split(",");
            int index = 0;

            // Read world name, pitch, and yaw once
            String worldName = data[index++];
            if (worldName == null) worldName = Bukkit.getWorlds().get(0).getName();
            float yaw = Integer.parseInt(data[index++]);
            float pitch = Integer.parseInt(data[index++]);

            // Read origin coordinates
            double originX = Integer.parseInt(data[index++]);
            double originY = Integer.parseInt(data[index++]);
            double originZ = Integer.parseInt(data[index++]);
            this.origin = new SerializableLocation(worldName, originX, originY, originZ, yaw, pitch);

            // Read cuboid bounds
            double cuboidMinX = Integer.parseInt(data[index++]);
            double cuboidMinY = Integer.parseInt(data[index++]);
            double cuboidMinZ = Integer.parseInt(data[index++]);
            double cuboidMaxX = Integer.parseInt(data[index++]);
            double cuboidMaxY = Integer.parseInt(data[index++]);
            double cuboidMaxZ = Integer.parseInt(data[index++]);

            // Read blocks grouped by material
            int materialCount = Integer.parseInt(data[index++]);
            Location originLoc = origin.toLocation();
            World world = originLoc.getWorld();
            for (int i = 0; i < materialCount; i++) {
                Material material = Material.valueOf(data[index++]);
                int blockCount = Integer.parseInt(data[index++]);
                for (int j = 0; j < blockCount; j++) {
                    double x = Integer.parseInt(data[index++]);
                    double y = Integer.parseInt(data[index++]);
                    double z = Integer.parseInt(data[index++]);
                    float blockYaw = Integer.parseInt(data[index++]);
                    float blockPitch = Integer.parseInt(data[index++]);
                    Location relativeLoc = new Location(world, x, y, z, blockYaw, blockPitch).add(originLoc);
                    this.blocks.put(new SerializableLocation(relativeLoc), material);
                }
            }

            // Fill the rest with air blocks if needed
            for (double x = cuboidMinX; x <= cuboidMaxX; x++) {
                for (double y = cuboidMinY; y <= cuboidMaxY; y++) {
                    for (double z = cuboidMinZ; z <= cuboidMaxZ; z++) {
                        Location loc = new Location(world, x, y, z).add(originLoc);
                        SerializableLocation serializableLoc = new SerializableLocation(loc);
                        if (!blocks.containsKey(serializableLoc)) {
                            blocks.put(serializableLoc, Material.AIR);
                        }
                    }
                }
            }
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