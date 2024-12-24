package es.redactado.tachyonRefreshed.api;

import org.bukkit.Location;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the public API for working with schematics.
 */
public interface Schematic {
    /**
     * Creates a new schematic by copying blocks between two locations.
     *
     * @param start  The starting location of the area to copy
     * @param end    The ending location of the area to copy
     * @param origin The origin location for the schematic
     * @return A CompletableFuture that completes with the new Schematic
     */
    static CompletableFuture<Schematic> createAsync(Location start, Location end, Location origin) {
        return SchematicFactory.getInstance().createAsync(start, end, origin);
    }

    /**
     * Loads a schematic from a file.
     *
     * @param file The file to load from
     * @return A CompletableFuture that completes with the loaded Schematic
     */
    static CompletableFuture<Schematic> createAsync(File file) {
        return SchematicFactory.getInstance().createAsync(file);
    }

    /**
     * Gets the file extension used for schematics.
     *
     * @return The file extension including the dot
     */
    static String getFileExtension() {
        return ".tachyon";
    }

    /**
     * Pastes the schematic at the given location.
     *
     * @param location  Where to paste the schematic
     * @param ignoreAir Whether to ignore air blocks when pasting
     * @return A CompletableFuture that completes when the paste is done
     */
    CompletableFuture<Void> pasteAsync(Location location, boolean ignoreAir);

    /**
     * Saves the schematic to a file.
     *
     * @param file The file to save to
     * @return A CompletableFuture that completes when the save is done
     */
    CompletableFuture<Void> saveAsync(File file);

    /**
     * Rotates the schematic around the Y axis.
     *
     * @param angle The angle in degrees to rotate
     */
    void rotate(double angle);

    /**
     * Flips the schematic in the specified direction.
     *
     * @param direction The direction to flip ("up", "down", "left", "right")
     */
    void flip(String direction);

    /**
     * Gets the number of blocks in this schematic.
     *
     * @return The block count
     */
    int getBlockCount();
}
