package es.redactado.tachyonRefreshed.core;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import es.redactado.tachyonRefreshed.api.Schematic;
import es.redactado.tachyonRefreshed.packet.BlockChangePacketSender;
import es.redactado.tachyonRefreshed.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TachyonSchematic implements Schematic {
    private final Map<SerializableLocation, Material> blocks = new ConcurrentHashMap<>();
    private final SerializableLocation origin;
    private final SerializableLocation min;
    private final SerializableLocation max;
    private final BlockChangePacketSender packetSender;

    @Inject
    public TachyonSchematic(
            @Assisted("start") Location start,
            @Assisted("end") Location end,
            @Assisted("origin") Location origin,
            BlockChangePacketSender packetSender) {
        this.min = new SerializableLocation(start);
        this.max = new SerializableLocation(end);
        this.origin = new SerializableLocation(origin);
        this.packetSender = packetSender;
        copyBlocks(start, end, origin);
    }

    private void copyBlocks(Location start, Location end, Location origin) {
        World world = start.getWorld();
        int minX = Math.min(start.getBlockX(), end.getBlockX());
        int minY = Math.min(start.getBlockY(), end.getBlockY());
        int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
        int maxX = Math.max(start.getBlockX(), end.getBlockX());
        int maxY = Math.max(start.getBlockY(), end.getBlockY());
        int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

        // Create a list of all coordinates within the specified range
        List<int[]> coordinates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    coordinates.add(new int[]{x, y, z});
                }
            }
        }

        // Use parallelStream to process the coordinates in parallel
        coordinates.parallelStream().forEach(coord -> {
            Location loc = new Location(world, coord[0], coord[1], coord[2]);
            blocks.put(new SerializableLocation(loc), world.getBlockAt(loc).getType());
        });
    }

    @Override
    public CompletableFuture<Void> pasteAsync(Location pasteLocation, boolean ignoreAir) {
        return CompletableFuture.runAsync(() -> {
            Location originLoc = origin.toLocation();
            List<Location> locations = blocks.entrySet().stream()
                .filter(entry -> !ignoreAir || entry.getValue() != Material.AIR)
                .map(entry -> {
                    Location loc = entry.getKey().toLocation();
                    loc.setWorld(pasteLocation.getWorld());
                    loc.subtract(originLoc);
                    loc.add(pasteLocation);
                    return loc;
                })
                .collect(Collectors.toList());

            packetSender.sendBlockChanges(locations, blocks);
        });
    }

    @Override
    public CompletableFuture<Void> saveAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))))) {
                // Write metadata
                writer.write(String.format("%s,%d,%d,%d,%d,%d,",
                    origin.getWorldName(),
                    (int)origin.getX(),
                    (int)origin.getY(),
                    (int)origin.getZ(),
                    (int)origin.getYaw(),
                    (int)origin.getPitch()
                ));

                // Write blocks
                writer.write(blocks.size() + "\n");
                for (Map.Entry<SerializableLocation, Material> entry : blocks.entrySet()) {
                    SerializableLocation loc = entry.getKey();
                    writer.write(String.format("%s,%d,%d,%d,%s\n",
                        loc.getWorldName(),
                        (int)loc.getX(),
                        (int)loc.getY(),
                        (int)loc.getZ(),
                        entry.getValue().name()
                    ));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save schematic", e);
            }
        });
    }

    @Override
    public void rotate(double angle) {
        // TODO: Implement rotation logic using PacketEvents
    }

    @Override
    public void flip(String direction) {
        // TODO: Implement flip logic using PacketEvents
    }

    @Override
    public int getBlockCount() {
        return blocks.size();
    }
}
