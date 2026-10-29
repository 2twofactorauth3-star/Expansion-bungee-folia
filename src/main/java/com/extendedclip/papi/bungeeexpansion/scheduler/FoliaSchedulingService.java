package com.extendedclip.papi.bungeeexpansion.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.BooleanSupplier;

final class FoliaSchedulingService implements SchedulingService {
    @Override
    public Cancellation schedule(Plugin plugin, Runnable task, long initialTicks, long periodTicks) {
        ScheduledTask scheduled = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin, ignored -> task.run(), initialTicks, periodTicks);
        return scheduled::cancel;
    }

    @Override
    public void send(Plugin plugin, String channel, byte[] packet, BooleanSupplier allowed) {
        if (!allowed.getAsBoolean()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            ScheduledTask scheduled = player.getScheduler().run(plugin, ignored -> {
                if (allowed.getAsBoolean() && player.isOnline()) {
                    player.sendPluginMessage(plugin, channel, packet);
                }
            }, null);
            if (scheduled != null) {
                return;
            }
        }
    }
}
