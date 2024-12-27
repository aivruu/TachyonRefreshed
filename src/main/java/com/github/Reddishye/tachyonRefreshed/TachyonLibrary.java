package com.github.Reddishye.tachyonRefreshed;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.github.Reddishye.tachyonRefreshed.inject.TachyonModule;
import com.github.Reddishye.tachyonRefreshed.packet.BlockChangePacketSender;
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

        // Initialize PacketEvents
        initializePacketEvents(plugin);

        // Create module with dependencies
        TachyonModule module = new TachyonModule();

        // Setup Guice with all required modules
        injector = Guice.createInjector(binder -> {
            // Bind the plugin instance
            binder.bind(Plugin.class).toInstance(plugin);
            // Bind PacketEvents
            binder.bind(PacketEventsAPI.class).toInstance(PacketEvents.getAPI());
            // Install our main module
            binder.install(module);
        });
    }

    private void initializePacketEvents(Plugin plugin) {
        if (PacketEvents.getAPI() == null || !PacketEvents.getAPI().isInitialized()) {
            Plugin packetEvents = plugin.getServer().getPluginManager().getPlugin("PacketEvents");
            if (packetEvents == null) {
                var builder = SpigotPacketEventsBuilder.build(plugin);
                var settings = builder.getSettings();
                settings.debug(false);
                settings.bStats(true);
                settings.checkForUpdates(false);

                PacketEvents.setAPI(builder);
                PacketEvents.getAPI().load();
                PacketEvents.getAPI().init();
                ownedPacketEvents = true;
            } else {
                plugin.getLogger().info("Using PacketEvents from plugin: " + packetEvents.getName() + " v" + packetEvents.getDescription().getVersion());
            }
        } else {
            plugin.getLogger().info("Using existing PacketEvents instance");
        }
    }

    public void disable() {
        if (ownedPacketEvents) {
            PacketEvents.getAPI().terminate();
        }
    }

    public static TachyonLibrary getInstance() {
        return instance;
    }

    public <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
