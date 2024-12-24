# Tachyon Refreshed
[![](https://jitpack.io/v/Reddishye/TachyonRefreshed.svg)](https://jitpack.io/#Reddishye/TachyonRefreshed)

A high-performance, cross-version schematic library for Minecraft servers that provides an efficient alternative to WorldEdit for handling schematics.

## 🌟 Key Features

### Performance & Efficiency
- **Zero TPS Impact**: Utilizes PacketEvents for block placement, bypassing Bukkit methods
- **Fully Asynchronous Operations**: All operations (create/save/load/paste) run asynchronously
- **Lightning-Fast Performance**: 
  - Up to 10x faster than traditional Java serialization for loading/saving
  - Faster block placement compared to WorldEdit
  - Optimized for large schematics

### Compatibility & Integration
- **Cross-Version Support**: Compatible with Minecraft versions 1.8 through 1.20
- **Lightweight**: Minimal dependencies, significantly smaller than WorldEdit
- **Custom File Format**: Uses `.tachyon` extension (customizable)

### Schematic Manipulation
- **Creation**: Create schematics from selected regions
- **Storage**: Save and load schematics efficiently
- **Editing Capabilities**:
  - **Rotation**: Rotate schematics in 90-degree increments
  - **Flipping**: Flip in any direction (up/down/left/right)
  - **Block Replacement**: Replace specific block types within schematics
- **Paste Options**: Control air block handling during paste operations

## 📦 Installation

### Manual Installation
1. Download the latest release
2. Add the JAR to your project dependencies
3. Include the library in your plugin.yml

### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Reddishye:TachyonRefreshed:VERSION'
}
```

## 🚀 Quick Start Guide

### Basic Usage

1. **Creating a Schematic**
```java
// From locations
Location pos1 = /* first corner */;
Location pos2 = /* second corner */;
Location origin = /* origin point */;
Schematic.createAsync(pos1, pos2, origin).thenAccept(schematic -> {
    // Schematic created successfully
});

// From file
File file = new File("plugins/YourPlugin/schematics/house.tachyon");
Schematic.createAsync(file).thenAccept(schematic -> {
    // Schematic loaded successfully
});
```

2. **Saving a Schematic**
```java
File saveFile = new File("plugins/YourPlugin/schematics/build.tachyon");
schematic.saveAsync(saveFile).thenRun(() -> {
    // Schematic saved successfully
});
```

3. **Pasting a Schematic**
```java
Location pasteLocation = /* where to paste */;
schematic.pasteAsync(pasteLocation, true) // true = ignore air blocks
    .thenRun(() -> {
        // Paste completed successfully
    });
```

### Advanced Operations

1. **Schematic Manipulation**
```java
// Rotate the schematic
schematic.rotate(90); // Rotates 90 degrees clockwise

// Flip the schematic
schematic.flip("up");    // Flips upward
schematic.flip("down");  // Flips downward
schematic.flip("left");  // Flips left
schematic.flip("right"); // Flips right

// Get block count
int totalBlocks = schematic.getBlockCount();
```

## 🔧 Complete Example Plugin

```java
package me.athish.tachyon;

import es.redactado.tachyonRefreshed.Schematic;
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

    private void loadSchematic(Player player, String filename) {
        try {
            File file = new File(getDataFolder(), "schematics/" + filename + Schematic.getFileExtension());
            long start = System.currentTimeMillis();
            Schematic.createAsync(file).thenAccept(schematic -> {
                schematics.put(player.getUniqueId(), schematic);
                player.sendMessage("Schematic created and stored successfully." + (System.currentTimeMillis() - start) + " ms");
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

## ⚙️ Configuration

The default file extension is `.tachyon`, but you can customize it in your implementation:

```java
// Custom implementation of SchematicFactory
public class CustomSchematicFactory implements SchematicFactory {
    @Override
    public String getFileExtension() {
        return ".myext";
    }
    // ... other implementations
}
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits
- [BlockChanger](https://github.com/TheGaming999/BlockChanger) by TheGaming999 - For block manipulation utilities
- [Resonos](https://github.com/Resonos) - For the original Tachyon library

## 🔄 Version History

See [CHANGELOG.md](CHANGELOG.md) for a list of changes.

## 🎯 Roadmap
- [ ] Add version info for schematics
- [ ] Fix block rotations
