package com.github.Reddishye.tachyonRefreshed.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Singleton
public class BlockChangePacketSender {

    public void sendBlockChanges(List<Location> locations, Map<Location, BlockData> blocks) {
        locations.forEach(location -> {
            BlockData blockData = blocks.get(location);

            if (blockData != null) {
                Vector3i position = new Vector3i(
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                );

                try {
                    int blockStateId = WrappedBlockState.getByString(blockData.getAsString()).getGlobalId();

                    WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                            position,
                            blockStateId
                    );

                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.getWorld().equals(location.getWorld())) {
                            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                        }
                    });
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.WARNING,
                            String.format("Failed to send block change packet for blockdata %s at %d,%d,%d: %s",
                                    blockData.getAsString(), position.getX(), position.getY(), position.getZ(), e.getMessage()
                            ), e);
                }
            }
        });
    }
}
