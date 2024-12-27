package com.github.Reddishye.tachyonRefreshed.core;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.github.Reddishye.tachyonRefreshed.api.Schematic;
import com.github.Reddishye.tachyonRefreshed.packet.BlockChangePacketSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.bukkit.block.structure.StructureRotation.*;

public class TachyonSchematic implements Schematic {
    private static class RelativeBlock implements Serializable {
        private final int x, y, z;
        private final BlockData blockData;

        public RelativeBlock(int x, int y, int z, BlockData blockData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockData = blockData.clone();
        }

        public Location toAbsolute(Location origin) {
            return origin.clone().add(x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelativeBlock that = (RelativeBlock) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private final Set<RelativeBlock> blocks = ConcurrentHashMap.newKeySet();
    private final BlockChangePacketSender packetSender;
    private final int width, height, length;

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
        int minX = Math.min(start.getBlockX(), end.getBlockX());
        int minY = Math.min(start.getBlockY(), end.getBlockY());
        int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
        int maxX = Math.max(start.getBlockX(), end.getBlockX());
        int maxY = Math.max(start.getBlockY(), end.getBlockY());
        int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

        List<Vector> coordinates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    coordinates.add(new Vector(x, y, z));
                }
            }
        }

        coordinates.parallelStream().forEach(vec -> {
            Location loc = vec.toLocation(start.getWorld());
            Block block = loc.getBlock();
            blocks.add(new RelativeBlock(
                    vec.getBlockX() - origin.getBlockX(),
                    vec.getBlockY() - origin.getBlockY(),
                    vec.getBlockZ() - origin.getBlockZ(),
                    block.getBlockData()
            ));
        });
    }

    @Override
    public CompletableFuture<Void> pasteAsync(Location pasteLocation, boolean ignoreAir) {
        return CompletableFuture.runAsync(() -> {
            List<Location> locations = new ArrayList<>();
            Map<Location, BlockData> blockChanges = new HashMap<>();

            blocks.stream()
                    .filter(block -> !ignoreAir || !block.blockData.getMaterial().isAir())
                    .forEach(block -> {
                        Location loc = block.toAbsolute(pasteLocation);
                        locations.add(loc);
                        blockChanges.put(loc, block.blockData);
                    });

            packetSender.sendBlockChanges(locations, blockChanges);
        });
    }

    @Override
    public void pasteSync(Location pasteLocation, boolean ignoreAir) {
        List<Location> locations = new ArrayList<>();
        Map<Location, BlockData> blockChanges = new HashMap<>();

        blocks.stream()
                .filter(block -> !ignoreAir || !block.blockData.getMaterial().isAir())
                .forEach(block -> {
                    Location loc = block.toAbsolute(pasteLocation);
                    locations.add(loc);
                    blockChanges.put(loc, block.blockData);
                });

        packetSender.sendBlockChanges(locations, blockChanges);
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
            } catch (IOException e) {
                throw new RuntimeException("Failed to save schematic", e);
            }
        });
    }

    public static CompletableFuture<Schematic> loadAsync(
            File file,
            Location origin,
            BlockChangePacketSender packetSender) {
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
            } catch (IOException e) {
                throw new RuntimeException("Failed to load schematic", e);
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

        StructureRotation rotation;
        if (angle == 90) rotation = StructureRotation.CLOCKWISE_90;
        else if (angle == 180) rotation = StructureRotation.CLOCKWISE_180;
        else if (angle == 270) rotation = StructureRotation.COUNTERCLOCKWISE_90;
        else return;

        Set<RelativeBlock> rotatedBlocks = ConcurrentHashMap.newKeySet();

        blocks.forEach(block -> {
            double x = block.x;
            double z = block.z;

            int newX, newZ;
            switch (rotation) {
                case CLOCKWISE_90:
                    newX = (int) -z;
                    newZ = (int) x;
                    break;
                case CLOCKWISE_180:
                    newX = (int) -x;
                    newZ = (int) -z;
                    break;
                case COUNTERCLOCKWISE_90:
                    newX = (int) z;
                    newZ = (int) -x;
                    break;
                default:
                    newX = (int) x;
                    newZ = (int) z;
            }

            BlockData rotatedData = block.blockData.clone();
            rotatedData.rotate(rotation);

            rotatedBlocks.add(new RelativeBlock(
                    newX,
                    block.y,
                    newZ,
                    rotatedData
            ));
        });

        blocks.clear();
        blocks.addAll(rotatedBlocks);
    }

    @Override
    public void flip(String direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction cannot be null");
        }

        Set<RelativeBlock> flippedBlocks = ConcurrentHashMap.newKeySet();
        Mirror mirror;

        switch (direction.toLowerCase()) {
            case "up":
            case "down":
            case "left":
            case "right":
                mirror = Mirror.LEFT_RIGHT;
                break;
            case "north":
            case "south":
                mirror = Mirror.FRONT_BACK;
                break;
            default:
                throw new IllegalArgumentException("Invalid flip direction: " + direction);
        }

        blocks.forEach(block -> {
            int newX = block.x;
            int newY = block.y;
            int newZ = block.z;

            switch (direction.toLowerCase()) {
                case "up":
                case "down":
                    newY = -newY;
                    break;
                case "left":
                case "right":
                    newX = -newX;
                    break;
                case "north":
                case "south":
                    newZ = -newZ;
                    break;
            }

            BlockData flippedData = block.blockData.clone();
            flippedData.mirror(mirror);

            flippedBlocks.add(new RelativeBlock(
                    newX,
                    newY,
                    newZ,
                    flippedData
            ));
        });

        blocks.clear();
        blocks.addAll(flippedBlocks);
    }

    @Override
    public int getBlockCount() {
        return blocks.size();
    }
}
