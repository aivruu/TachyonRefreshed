package com.github.Reddishye.tachyonRefreshed.api;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import com.github.Reddishye.tachyonRefreshed.TachyonLibrary;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating Schematic instances.
 */
@Singleton
public class SchematicFactory {
  private final SchematicService schematicService;

  @Inject
  public SchematicFactory(SchematicService schematicService) {
    this.schematicService = schematicService;
  }

  public static SchematicFactory getInstance() {
    return TachyonLibrary.getInstance().getInstance(SchematicFactory.class);
  }

  public interface Create {
    Schematic create(
      @Assisted("start") Location start,
      @Assisted("end") Location end,
      @Assisted("origin") Location origin
    );
  }

  public Schematic createSchematic(Location start, Location end, Location origin) {
    return schematicService.createSchematicAsync(start, end, origin).join();
  }

  public CompletableFuture<Schematic> loadSchematic(File file) {
    return schematicService.loadSchematicAsync(file);
  }

  public CompletableFuture<Schematic> createAsync(Location start, Location end, Location origin) {
    return CompletableFuture.supplyAsync(() -> createSchematic(start, end, origin));
  }

  public CompletableFuture<Schematic> createAsync(File file) {
    return loadSchematic(file);
  }
}