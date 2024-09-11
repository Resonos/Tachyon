# Tachyon
A Easy to Use, Fast, Cross-Version schematic library.

Stop having to depend on large plugins like WorldEdit or Limiting Libraries like NeoSchematic. Tachyon allows you to create, save, load & paste schematics easily & Efficiently.

## Features
- [x] 🟩 **Easy to Use** 🟩
    - Extremely easy to use, just implement tachyon and look at our simple usage examples!
  
- [x] 🌎 **1.8 - 1.20 Version Support** 🌎
    - Create Schematics on any version you please. It is just as fast if not faster on some versions 😉
  
- [x] 💎 **Much Smaller than WorldEdit** 💎
    - Stop having to depend on a whole large plugin like worldedit or having to use other schematic libraries that are inefficient or sacrifice version support
  
- [x] 🌟 **Faster than WorldEdit** 🌟
    - Tachyon's Pasting system is simply much faster than worldedit :)
  
- [x] 🚀 **Fully Asynchronous & Zero TPS Impact** 🚀
    - There won't be any TPS loss no matter the amount of blocks being set. This is because Tachyon places blocks using NMS, thereby bypassing inefficient bukkit methods.
  
- [x] 📜 **Custom file extension for schematics 📜**
    - Tachyon uses a custom file extension for schematics which is ".tachyon" by default but you may set it to whatever you'd like by rewriting 1 string.
  
- [x] ⚡ **Hyperfast loading & saving of schematics** ⚡
    - Significantly greater speeds for saving and loading schematics, sometimes upto 10x faster than regular java serialization.
- [x] 🛠️ **Schematic Editing** 🛠️
    - We support direct editing of schematics without having to paste them. This includes:
      - [x] **Rotations** - Rotate the schematic by any angle multiple of 90 degrees.
      - [x] **Flipping** - Flip the schematic up , down, left or right.
      - [x] **Block Replacements** - Replace any block type in the schematic.

## TODO
- [ ] Add Version info for schematics
- [ ] Fix block rotations


## Setup

**Manual**\
Just Copy the files into your project.

## Usage

### Creating a Schematic from locations
```java
Schematic.createAsync(pos1, pos2, origin);
```

### Saving a Schematic
```java
Schematic schematic = /* get your schematic */
File file = new File(dir, filename + Schematic.getFileExtension());
schematic.saveAsync(file);
```

### Loading/Creating a Schematic from File
```java
File file = new File(getDataFolder(), "schematics/" + filename + Schematic.getFileExtension());
Schematic.createAsync(file);
```


### Pasting a Schematic
```java
Schematic schematic = /* get your schematic */
schematic.pasteAsync(pasteLocation, true);  // boolean ignoreAir blocks
```

### Example plugin
```java
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
    long start = System.currentTimeMillis();
    Schematic.createAsync(first, second, player.getLocation()).thenAccept(schematic -> {
      schematics.put(player.getUniqueId(), schematic);
      player.sendMessage("Blocks copied successfully. " + (System.currentTimeMillis() - start) + " ms");
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
        player.sendMessage("Schematic created and stored successfully."  + (System.currentTimeMillis() - start) + " ms");
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
                    player.sendMessage("Schematic pasted successfully. " + (System.currentTimeMillis() - start) + " ms"))
            .exceptionally(e -> {
              player.sendMessage("Error pasting schematic: " + e.getMessage());
              return null;
            });
  }
}
```

## Credits
- [BlockChanger](https://github.com/TheGaming999/BlockChanger) by TheGaming999



