package com.github.Reddishye.tachyonRefreshed.api;

import org.bukkit.Location;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for schematic operations.
 */
public interface SchematicService {
  /**
   * Creates a new schematic by copying blocks between two locations.
   *
   * @param start  The starting location
   * @param end    The ending location
   * @param origin The origin point
   * @return A future that completes with the new schematic
   */
  CompletableFuture<Schematic> createSchematicAsync(Location start, Location end, Location origin);

  /**
   * Loads a schematic from a file.
   *
   * @param file The file to load from
   * @return A future that completes with the loaded schematic
   */
  CompletableFuture<Schematic> loadSchematicAsync(File file);
}
