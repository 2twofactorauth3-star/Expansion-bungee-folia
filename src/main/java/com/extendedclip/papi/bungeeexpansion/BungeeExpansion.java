package com.extendedclip.papi.bungeeexpansion;

import com.extendedclip.papi.bungeeexpansion.scheduler.SchedulingService;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

public final class BungeeExpansion extends PlaceholderExpansion implements PluginMessageListener, Taskable, Configurable {
    private static final String MESSAGE_CHANNEL = "BungeeCord";
    private static final String SERVERS_CHANNEL = "GetServers";
    private static final String PLAYERS_CHANNEL = "PlayerCount";
    private static final String CONFIG_INTERVAL = "check_interval";
    private static final long DEFAULT_INTERVAL = 30L;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();
    private volatile boolean active;
    private volatile long generation;
    private SchedulingService.Cancellation polling;
    private SchedulingService scheduling;
    private boolean channelsRegistered;

    @Override
    public String getIdentifier() {
        return "bungee";
    }

    @Override
    public String getAuthor() {
        return "clip";
    }

    @Override
    public String getVersion() {
        return "2.3";
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Collections.singletonMap(CONFIG_INTERVAL, 30);
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        String key = identifier.toLowerCase(Locale.ROOT);
        if ("all".equals(key) || "total".equals(key)) {
            int total = 0;
            for (int count : counts.values()) {
                total += count;
            }
            return Integer.toString(total);
        }
        return Integer.toString(counts.getOrDefault(key, 0));
    }

    @Override
    public synchronized void start() {
        stop();
        Plugin plugin = getPlaceholderAPI();
        scheduling = SchedulingService.create();
        long interval = Math.max(1L, getLong(CONFIG_INTERVAL, DEFAULT_INTERVAL));
        long periodTicks = interval > Long.MAX_VALUE / 20L ? Long.MAX_VALUE : interval * 20L;
        long currentGeneration = ++generation;
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, MESSAGE_CHANNEL);
            channelsRegistered = true;
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, MESSAGE_CHANNEL, this);
            active = true;
            polling = scheduling.schedule(plugin, () -> poll(currentGeneration), 40L, periodTicks);
        } catch (RuntimeException ex) {
            stop();
            throw ex;
        }
    }

    @Override
    public synchronized void stop() {
        active = false;
        ++generation;
        if (polling != null) {
            polling.cancel();
            polling = null;
        }
        if (channelsRegistered) {
            Plugin plugin = getPlaceholderAPI();
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, MESSAGE_CHANNEL, this);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, MESSAGE_CHANNEL);
            channelsRegistered = false;
        }
        scheduling = null;
        counts.clear();
    }

    private void poll(long expectedGeneration) {
        if (!isCurrent(expectedGeneration)) {
            return;
        }
        if (counts.isEmpty()) {
            sendMessage(SERVERS_CHANNEL, null, expectedGeneration);
            return;
        }
        for (String server : counts.keySet()) {
            if (!isCurrent(expectedGeneration)) {
                return;
            }
            sendMessage(PLAYERS_CHANNEL, server, expectedGeneration);
        }
    }

    private boolean isCurrent(long expectedGeneration) {
        return active && generation == expectedGeneration;
    }

    private void sendMessage(String subchannel, String server, long expectedGeneration) {
        byte[] packet = createPacket(subchannel, server);
        if (packet == null || !isCurrent(expectedGeneration)) {
            return;
        }
        SchedulingService service = scheduling;
        if (service != null) {
            BooleanSupplier allowed = () -> isCurrent(expectedGeneration);
            service.send(getPlaceholderAPI(), MESSAGE_CHANNEL, packet, allowed);
        }
    }

    private byte[] createPacket(String subchannel, String server) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(subchannel);
            if (server != null) {
                out.writeUTF(server);
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            getPlaceholderAPI().getLogger().log(Level.WARNING, "[Bungee] Failed to create proxy request", ex);
            return null;
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!active || !MESSAGE_CHANNEL.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();
            if (SERVERS_CHANNEL.equals(subchannel)) {
                for (String name : in.readUTF().split(",", -1)) {
                    String server = name.trim().toLowerCase(Locale.ROOT);
                    if (!server.isEmpty()) {
                        counts.putIfAbsent(server, 0);
                    }
                }
            } else if (PLAYERS_CHANNEL.equals(subchannel)) {
                if (in.available() == 0) {
                    return;
                }
                String server = in.readUTF().toLowerCase(Locale.ROOT);
                if (in.available() < Integer.BYTES) {
                    getPlaceholderAPI().getLogger().log(Level.SEVERE,
                        "[" + getName() + "] Could not get the player count from server " + server + ".");
                    counts.put(server, 0);
                } else {
                    counts.put(server, in.readInt());
                }
            }
        } catch (IOException ex) {
            getPlaceholderAPI().getLogger().log(Level.WARNING, "[Bungee] Invalid proxy response", ex);
        }
    }
}
