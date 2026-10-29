package com.extendedclip.papi.bungeeexpansion.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BooleanSupplier;

final class BukkitSchedulingService implements SchedulingService {
    @Override
    public Cancellation schedule(Plugin plugin, Runnable task, long initialTicks, long periodTicks) {
        BukkitTask scheduled = Bukkit.getScheduler().runTaskTimer(plugin, task, initialTicks, periodTicks);
        return scheduled::cancel;
    }

    @Override
    public void send(Plugin plugin, String channel, byte[] packet, BooleanSupplier allowed) {
        if (!allowed.getAsBoolean()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (allowed.getAsBoolean()) {
                player.sendPluginMessage(plugin, channel, packet);
            }
            return;
        }
    }
}
