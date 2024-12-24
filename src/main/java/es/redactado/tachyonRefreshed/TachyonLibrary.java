package es.redactado.tachyonRefreshed;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Guice;
import com.google.inject.Injector;
import es.redactado.tachyonRefreshed.inject.TachyonModule;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public final class TachyonLibrary extends JavaPlugin {
    private static TachyonLibrary instance;
    private Injector injector;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize PacketEvents
        PacketEvents.getAPI().init();
        
        // Setup Guice
        injector = Guice.createInjector(new TachyonModule());
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public static TachyonLibrary getInstance() {
        return instance;
    }

    public Injector getInjector() {
        return injector;
    }
}
