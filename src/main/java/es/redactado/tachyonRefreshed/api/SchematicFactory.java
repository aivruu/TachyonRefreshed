package es.redactado.tachyonRefreshed.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating Schematic instances.
 */
@Singleton
public class SchematicFactory {
    private static SchematicFactory instance;
    private final SchematicService schematicService;

    @Inject
    private SchematicFactory(SchematicService schematicService) {
        this.schematicService = schematicService;
        instance = this;
    }

    public static SchematicFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SchematicFactory has not been initialized by Guice");
        }
        return instance;
    }

    public CompletableFuture<Schematic> createAsync(Location start, Location end, Location origin) {
        return schematicService.createSchematicAsync(start, end, origin);
    }

    public CompletableFuture<Schematic> createAsync(File file) {
        return schematicService.loadSchematicAsync(file);
    }
}
