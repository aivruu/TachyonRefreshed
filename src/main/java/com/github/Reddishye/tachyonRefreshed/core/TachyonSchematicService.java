package com.github.Reddishye.tachyonRefreshed.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.github.Reddishye.tachyonRefreshed.api.Schematic;
import com.github.Reddishye.tachyonRefreshed.api.SchematicService;
import com.github.Reddishye.tachyonRefreshed.packet.BlockChangePacketSender;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.io.*;
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
  public CompletableFuture<@Nullable Schematic> loadSchematicAsync(File file) {
    return TachyonSchematic.loadAsync(file, packetSender);
  }

}
