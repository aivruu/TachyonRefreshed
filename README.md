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
- **Smart PacketEvents Integration**: Automatically uses existing PacketEvents instance if available

## 📦 Installation

Add this to your `build.gradle`:

```gradle
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '9.0.0-beta4'  // Latest version of GradleUp Shadow
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Reddishye:TachyonRefreshed:VERSION'
    // Optional: If you want to use your own PacketEvents instance
    implementation 'com.github.retrooper.packetevents:spigot:2.2.0'
}

shadowJar {
    // Relocate Tachyon and all its dependencies to your package
    relocate 'es.redactado.tachyonRefreshed', 'your.package.lib.tachyon'
    // If using your own PacketEvents, relocate it too
    relocate 'io.github.retrooper.packetevents', 'your.package.lib.packetevents'
}
```

That's it! All dependencies (Guice, etc.) are included and relocated automatically. If you have PacketEvents installed as a plugin or already included in your project, Tachyon will use that instance instead of creating its own.

## 🚀 Quick Start Guide

### Initialize the Library

First, initialize TachyonLibrary in your plugin's onEnable:

```java
public class YourPlugin extends JavaPlugin {
    private TachyonLibrary tachyon;
    
    @Override
    public void onEnable() {
        // Initialize Tachyon with your plugin instance
        // It will automatically detect and use any existing PacketEvents instance
        tachyon = new TachyonLibrary(this);
    }
    
    @Override
    public void onDisable() {
        if (tachyon != null) {
            tachyon.disable();
        }
    }
}
```

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

We are still working on a complete example plugin, but it will be available soon. Stay tuned!

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
