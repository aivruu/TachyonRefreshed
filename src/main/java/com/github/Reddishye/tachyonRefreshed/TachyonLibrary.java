package com.github.Reddishye.tachyonRefreshed;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.github.Reddishye.tachyonRefreshed.inject.TachyonModule;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;

public final class TachyonLibrary {
    private static TachyonLibrary instance;
    private final Injector injector;
    private final Plugin plugin;
    private boolean ownedPacketEvents = false;

    public TachyonLibrary(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        
        // Check if PacketEvents is already initialized
        if (!PacketEvents.getAPI().isInitialized()) {
            // If not initialized, check if it's loaded as a plugin
            Plugin packetEvents = plugin.getServer().getPluginManager().getPlugin("PacketEvents");
            if (packetEvents == null) {
                // No PacketEvents found, initialize our own
                PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
                PacketEvents.getAPI().load();
                PacketEvents.getAPI().init();
                ownedPacketEvents = true;
            } else {
                // Use the existing PacketEvents instance
                plugin.getLogger().info("Using PacketEvents from plugin: " + packetEvents.getName() + " v" + packetEvents.getDescription().getVersion());
            }
        } else {
            plugin.getLogger().info("Using existing PacketEvents instance");
        }
        
        // Setup Guice
        injector = Guice.createInjector(new TachyonModule());
    }

    public void disable() {
        // Only terminate PacketEvents if we created it
        if (ownedPacketEvents) {
            PacketEvents.getAPI().terminate();
        }
    }

    public static TachyonLibrary getInstance() {
        return instance;
    }

    public Injector getInjector() {
        return injector;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
