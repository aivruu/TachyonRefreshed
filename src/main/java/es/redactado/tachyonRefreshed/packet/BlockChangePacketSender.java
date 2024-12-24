package es.redactado.tachyonRefreshed.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.inject.Singleton;
import es.redactado.tachyonRefreshed.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;

@Singleton
public class BlockChangePacketSender {

    public void sendBlockChanges(List<Location> locations, Map<SerializableLocation, Material> blocks) {
        locations.forEach(location -> {
            SerializableLocation serLoc = new SerializableLocation(location);
            Material material = blocks.get(serLoc);
            if (material != null) {
                Vector3i position = new Vector3i(
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                );

                int blockStateId = WrappedBlockState.getByString(material.toString()).getGlobalId();

                WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                        position,
                        blockStateId
                );

                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.getWorld().equals(location.getWorld())) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                    }
                });
            }
        });
    }
}
