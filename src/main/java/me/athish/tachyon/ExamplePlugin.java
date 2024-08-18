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

public final class ExamplePlugin extends JavaPlugin {
  private final Map<UUID, Location> firstPoints = new HashMap<>();
  private final Map<UUID, Location> secondPoints = new HashMap<>();
  private final Map<UUID, Schematic> schematics = new HashMap<>();

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
      player.sendMessage("Usage: /schematic <pos1|pos2|copy|save|load|paste|rotate|flip> [filename]");
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
      case "rotate":
        if (args.length < 2) {
          player.sendMessage("Usage: /schematic rotate <angle>");
          return true;
        }
        rotateSchematic(player, Double.parseDouble(args[1]));
        break;
      case "flip":
        if (args.length < 2) {
          player.sendMessage("Usage: /schematic flip <up|down|left|right>");
          return true;
        }
        flipSchematic(player, String.valueOf(args[1]));
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
    long start = System.currentTimeMillis();
    Schematic.createAsync(first, second, player.getLocation()).thenAccept(schematic -> {
      schematics.put(player.getUniqueId(), schematic);
      long end = (System.currentTimeMillis() - start);
      player.sendMessage("Blocks copied successfully. (" + schematic.getBlockCount() + " blocks) ("  + end+ " ms)");
    }).exceptionally(e -> {
      player.sendMessage("Error creating schematic: " + e.getMessage());
      return null;
    });
  }

  private void saveSchematic(Player player, String filename) {
    Schematic schematic = schematics.get(player.getUniqueId());
    if (schematic == null) {
      player.sendMessage("Please copy a schematic first.");
      return;
    }
    File dir = new File(getDataFolder(), "schematics");
    if (!dir.exists()) {
      dir.mkdirs();
    }

    File file = new File(dir, filename + Schematic.getFileExtension());
    if (file.exists()) {
      player.sendMessage("A schematic with that name already exists.");
      return;
    }
    long start = System.currentTimeMillis();
    schematic.saveAsync(file).thenRun(() -> player.sendMessage("Schematic saved successfully." + (System.currentTimeMillis() - start) + " ms"))
            .exceptionally(e -> {
              player.sendMessage("Error saving schematic: " + e.getMessage());
              return null;
            });
  }

  private void loadSchematic(Player player, String filename)  {
    try {
      File file = new File(getDataFolder(), "schematics/" + filename + Schematic.getFileExtension());
      long start = System.currentTimeMillis();
      Schematic.createAsync(file).thenAccept(schematic -> {
        schematics.put(player.getUniqueId(), schematic);
        long end = (System.currentTimeMillis() - start);
        player.sendMessage("Schematic loaded successfully. (" + schematic.getBlockCount() + " blocks) ("  + end+ " ms)");
      }).exceptionally(e -> {
        player.sendMessage("Error loading schematic: " + e.getMessage());
        return null;
      });
    } catch (Exception e) {
      player.sendMessage("Error loading schematic: " + e.getMessage());
    }
  }

  private void pasteSchematic(Player player) {
    Schematic schematic = schematics.get(player.getUniqueId());
    if (schematic == null) {
      player.sendMessage("Please load/copy a schematic first.");
      return;
    }
    Location pasteLocation = player.getLocation();
    long start = System.currentTimeMillis();
    schematic.pasteAsync(pasteLocation, true).thenRun(() ->
    player.sendMessage("Schematic pasted successfully. (" + schematic.getBlockCount() + " blocks) ("  + (System.currentTimeMillis() - start)+ " ms)"))
            .exceptionally(e -> {
              player.sendMessage("Error pasting schematic: " + e.getMessage());
              return null;
            });
  }

  private void rotateSchematic(Player player, double angle) {
    Schematic schematic = schematics.get(player.getUniqueId());
    if (schematic == null) {
      player.sendMessage("Please load/copy a schematic first.");
      return;
    }
    long start = System.currentTimeMillis();
    schematic.rotate(angle);
    long end = (System.currentTimeMillis() - start);
    player.sendMessage("Schematic rotated successfully. (" + schematic.getBlockCount() + " blocks) ("  + end+ " ms)");
  }

  private void flipSchematic(Player player, String direction) {
    Schematic schematic = schematics.get(player.getUniqueId());
    if (schematic == null) {
      player.sendMessage("Please load/copy a schematic first.");
      return;
    }
    long start = System.currentTimeMillis();
    schematic.flip(direction);
    long end = (System.currentTimeMillis() - start);
    player.sendMessage("Schematic flipped successfully. (" + schematic.getBlockCount() + " blocks) ("  + end+ " ms)");
  }
}