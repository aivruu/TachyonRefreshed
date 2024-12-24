package es.redactado.tachyonRefreshed.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import es.redactado.tachyonRefreshed.api.Schematic;
import es.redactado.tachyonRefreshed.api.SchematicService;
import es.redactado.tachyonRefreshed.packet.BlockChangePacketSender;
import org.bukkit.Location;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TachyonSchematicService implements SchematicService {
    private final BlockChangePacketSender packetSender;

    @Inject
    public TachyonSchematicService(BlockChangePacketSender packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public CompletableFuture<Schematic> createSchematicAsync(Location start, Location end, Location origin) {
        return CompletableFuture.supplyAsync(() -> 
            new TachyonSchematic(start, end, origin, packetSender)
        );
    }

    @Override
    public CompletableFuture<Schematic> loadSchematicAsync(File file) {
        // TODO: Implement file loading
        throw new UnsupportedOperationException("Loading from file not yet implemented");
    }
}
