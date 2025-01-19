package com.github.Reddishye.tachyonRefreshed.core;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.github.Reddishye.tachyonRefreshed.api.Schematic;
import com.github.Reddishye.tachyonRefreshed.packet.BlockChangePacketSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TachyonSchematic implements Schematic {
    private static final Logger LOGGER = Logger.getLogger("TachyonRefreshed");

    private record RelativeBlock(int x, int y, int z, BlockData blockData) implements Serializable {
        public Location toAbsolute(Location origin) {
            return new Location(
                    origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                    origin.getYaw(), origin.getPitch()
            ).add(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RelativeBlock that)) return false;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private final Set<RelativeBlock> blocks = ConcurrentHashMap.newKeySet();
    private final BlockChangePacketSender packetSender;
    private final int width;
    private final int height;
    private final int length;

    @AssistedInject
    public TachyonSchematic(
            @Assisted("start") Location start,
            @Assisted("end") Location end,
            @Assisted("origin") Location origin,
            BlockChangePacketSender packetSender) {
        this.packetSender = packetSender;
        this.width = Math.abs(end.getBlockX() - start.getBlockX()) + 1;
        this.height = Math.abs(end.getBlockY() - start.getBlockY()) + 1;
        this.length = Math.abs(end.getBlockZ() - start.getBlockZ()) + 1;
        copyBlocks(start, end, origin);
    }

    private void copyBlocks(Location start, Location end, Location origin) {
        int maxX = Math.max(start.getBlockX(), end.getBlockX());
        int maxY = Math.max(start.getBlockY(), end.getBlockY());
        int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

        List<Vector> coordinates = new ArrayList<>();
        for (int x = Math.min(start.getBlockX(), end.getBlockX()); x <= maxX; x++) {
            for (int y = Math.min(start.getBlockY(), end.getBlockY()); y <= maxY; y++) {
                for (int z = Math.min(start.getBlockZ(), end.getBlockZ()); z <= maxZ; z++) {
                    coordinates.add(new Vector(x, y, z));
                }
            }
        }
        int vecX;
        int vecY;
        int vecZ;
        for (final Vector vec : coordinates) {
            vecX = vec.getBlockX();
            vecY = vec.getBlockY();
            vecZ = vec.getBlockZ();
            blocks.add(new RelativeBlock(
                    vecX - origin.getBlockX(),
                    vecY - origin.getBlockY(),
                    vecZ - origin.getBlockZ(),
                    start.getWorld().getBlockData(vecX, vecY, vecZ)
            ));
        }
    }

    @Override
    public CompletableFuture<Void> pasteAsync(Location pasteLocation, boolean ignoreAir) {
        return CompletableFuture.runAsync(() -> this.notifyBlockChanges(pasteLocation, ignoreAir));
    }

    private void notifyBlockChanges(final Location pasteLocation, final boolean ignoreAir) {
        final List<Location> locations = new ArrayList<>();
        final Map<Location, BlockData> blockChanges = new HashMap<>();
        for (final RelativeBlock block : blocks) {
            if (ignoreAir && block.blockData.getMaterial() == Material.AIR) {
                continue;
            }
            final Location loc = block.toAbsolute(pasteLocation);
            locations.add(loc);
            blockChanges.put(loc, block.blockData);
        }
        packetSender.sendBlockChanges(locations, blockChanges);
    }

    @Override
    public void pasteSync(Location pasteLocation, boolean ignoreAir) {
        this.notifyBlockChanges(pasteLocation, ignoreAir);
    }

    @Override
    public CompletableFuture<Void> saveAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
                out.writeInt(width);
                out.writeInt(height);
                out.writeInt(length);
                out.writeInt(blocks.size());

                for (RelativeBlock block : blocks) {
                    out.writeInt(block.x);
                    out.writeInt(block.y);
                    out.writeInt(block.z);
                    out.writeUTF(block.blockData.getAsString());
                }
            } catch (final IOException exception) {
                LOGGER.severe("Failed to load schematic, check by corrupt-data or file syntax-invalid.");
            }
        });
    }

    public static CompletableFuture<@Nullable Schematic> loadAsync(File file, BlockChangePacketSender packetSender) {
        return CompletableFuture.supplyAsync(() -> {
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {

                int width = in.readInt();
                int height = in.readInt();
                int length = in.readInt();
                int blockCount = in.readInt();

                Set<RelativeBlock> blocks = ConcurrentHashMap.newKeySet(blockCount);
                for (int i = 0; i < blockCount; i++) {
                    blocks.add(new RelativeBlock(
                            in.readInt(),
                            in.readInt(),
                            in.readInt(),
                            Bukkit.createBlockData(in.readUTF())
                    ));
                }

                return new TachyonSchematic(blocks, width, height, length, packetSender);
            } catch (final IOException exception) {
                LOGGER.severe("Failed to load schematic, check by corrupt-data or file syntax-invalid.");
                return null;
            }
        });
    }

    private TachyonSchematic(
            Set<RelativeBlock> blocks,
            int width,
            int height,
            int length,
            BlockChangePacketSender packetSender) {
        this.blocks.addAll(blocks);
        this.width = width;
        this.height = height;
        this.length = length;
        this.packetSender = packetSender;
    }

    @Override
    public void rotate(double angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;

        final StructureRotation rotation = switch ((int) angle) {
            case 90 -> StructureRotation.CLOCKWISE_90;
            case 180 -> StructureRotation.CLOCKWISE_180;
            case 270 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> null;
        };
        if (rotation == null) {
            return;
        }
        Set<RelativeBlock> rotatedBlocks = ConcurrentHashMap.newKeySet();
        int newX;
        int newZ;
        for (final RelativeBlock block : this.blocks) {
            switch (rotation) {
                case CLOCKWISE_90 -> {
                    newX = -block.z;
                    newZ = block.x;
                }
                case CLOCKWISE_180 -> {
                    newX = -block.z;
                    newZ = -block.x;
                }
                case COUNTERCLOCKWISE_90 -> {
                    newX = block.z;
                    newZ = -block.x;
                }
                default -> {
                    newX = block.z;
                    newZ = block.x;
                }
            }
            final BlockData rotatedData = block.blockData.clone();
            rotatedData.rotate(rotation);
            rotatedBlocks.add(new RelativeBlock(newX, block.y, newZ, rotatedData));
        }

        blocks.clear();
        blocks.addAll(rotatedBlocks);
    }

    @Override
    public void flip(@NotNull String direction) {
        // Avoid repeated to lower-case conversion.
        direction = direction.toLowerCase(Locale.ROOT);

        final Mirror mirror = switch (direction) {
            case "up", "down", "left", "right" -> Mirror.LEFT_RIGHT;
            case "north", "south" -> Mirror.FRONT_BACK;
            default -> Mirror.NONE;
        };
        if (mirror == Mirror.NONE) {
            return;
        }
        Set<RelativeBlock> flippedBlocks = ConcurrentHashMap.newKeySet();
        int originalX;
        int originalY;
        int originalZ;
        for (final RelativeBlock block : blocks) {
            originalX = block.x;
            originalY = block.y;
            originalZ = block.z;
            switch (direction) {
                case "up", "down" -> originalY = -originalY;
                case "left", "right" -> originalX = -originalX;
                case "north", "south" -> originalZ = -originalZ;
            }
            BlockData flippedData = block.blockData.clone();
            flippedData.mirror(mirror);
            flippedBlocks.add(new RelativeBlock(originalX, originalY, originalZ, flippedData));
        }

        blocks.clear();
        blocks.addAll(flippedBlocks);
    }

    @Override
    public int getBlockCount() {
        return blocks.size();
    }
}
