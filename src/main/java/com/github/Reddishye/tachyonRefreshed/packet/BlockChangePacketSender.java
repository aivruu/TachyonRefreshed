package com.github.Reddishye.tachyonRefreshed.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@Singleton
public class BlockChangePacketSender {
  public void sendBlockChanges(List<Location> locations, Map<Location, BlockData> blocks) {
    BlockData blockData;
    for (final Location location : locations) {
      blockData = blocks.get(location);
      if (blockData == null) {
        return;
      }
      final WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
        new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
        WrappedBlockState.getByString(blockData.getAsString()).getGlobalId()
      );
      for (final Player player : Bukkit.getOnlinePlayers()) {
        if (player.getWorld().getName().equals(location.getWorld().getName())) {
          PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
      }
      blockData = null;
    }
  }
}
