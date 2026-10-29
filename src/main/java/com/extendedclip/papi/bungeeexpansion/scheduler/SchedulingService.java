package com.extendedclip.papi.bungeeexpansion.scheduler;

import org.bukkit.plugin.Plugin;

import java.util.function.BooleanSupplier;

public interface SchedulingService {
    Cancellation schedule(Plugin plugin, Runnable task, long initialTicks, long periodTicks);

    void send(Plugin plugin, String channel, byte[] packet, BooleanSupplier allowed);

    static SchedulingService create() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, SchedulingService.class.getClassLoader());
            return new FoliaSchedulingService();
        } catch (ClassNotFoundException ignored) {
            return new BukkitSchedulingService();
        }
    }

    interface Cancellation {
        void cancel();
    }
}
