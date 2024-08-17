package me.athish.tachyon;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Tachyon extends JavaPlugin {
    private final Map<UUID, Location> firstPoints = new HashMap<>();
    private final Map<UUID, Location> secondPoints = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("schematic").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("Usage: /schematic <pos1|pos2|copy|save|load|paste> [filename]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1":
                firstPoints.put(player.getUniqueId(), player.getLocation());
                player.sendMessage("First position set.");
                break;
            case "pos2":
                secondPoints.put(player.getUniqueId(), player.getLocation());
                player.sendMessage("Second position set.");
                break;
            case "copy":
                copyBlocks(player);
                break;
            case "save":
                if (args.length < 2) {
                    player.sendMessage("Usage: /schematic save <filename>");
                    return true;
                }
                saveSchematic(player, args[1]);
                break;
            case "load":
                if (args.length < 2) {
                    player.sendMessage("Usage: /schematic load <filename>");
                    return true;
                }
                loadSchematic(player, args[1]);
                break;
            case "paste":
                pasteSchematic(player);
                break;
            default:
                player.sendMessage("Unknown subcommand. Use pos1, pos2, copy, save, load, or paste.");
        }

        return true;
    }

    private void copyBlocks(Player player) {
        Location first = firstPoints.get(player.getUniqueId());
        Location second = secondPoints.get(player.getUniqueId());

        if (first == null || second == null) {
            player.sendMessage("Please set both positions first.");
            return;
        }

        Schematic schematic = new Schematic(first, second);
        schematic.setOrigin(player.getLocation());
        Schematic.setPlayerSchematic(player.getUniqueId(), schematic);
        player.sendMessage("Blocks copied successfully.");
    }

    private void saveSchematic(Player player, String filename) {
        Schematic schematic = Schematic.getPlayerSchematic(player.getUniqueId());
        if (schematic == null) {
            player.sendMessage("No schematic copied. Use /schematic copy first.");
            return;
        }

        File dir = new File(getDataFolder(), "schematics");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, filename + Schematic.getFileExtension());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            schematic.serialize(fos);
            player.sendMessage("Schematic saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("Error saving schematic: " + e.getMessage());
        }
    }

    private void loadSchematic(Player player, String filename) {
        File dir = new File(getDataFolder(), "schematics");
        File file = new File(dir, filename + Schematic.getFileExtension());
        try (FileInputStream fis = new FileInputStream(file)) {
            Schematic schematic = Schematic.deserialize(fis);
            Schematic.setPlayerSchematic(player.getUniqueId(), schematic);
            player.sendMessage("Schematic loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("Error loading schematic: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void pasteSchematic(Player player) {
        Schematic schematic = Schematic.getPlayerSchematic(player.getUniqueId());
        if (schematic == null) {
            player.sendMessage("No schematic loaded. Use /schematic load first.");
            return;
        }

        Location pasteLocation = player.getLocation();
        long start = System.currentTimeMillis();
        schematic.pasteAsync(pasteLocation);
        long end = System.currentTimeMillis();
        player.sendMessage("Schematic pasted successfully in " + (end - start) + "ms.");
    }
}