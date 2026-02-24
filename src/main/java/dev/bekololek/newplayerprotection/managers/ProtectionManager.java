package dev.bekololek.newplayerprotection.managers;

import dev.bekololek.newplayerprotection.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ProtectionManager {

    private final Main plugin;
    private final File dataFile;
    private final Map<UUID, Long> protectedPlayers = new HashMap<>();

    public ProtectionManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    public void loadData() {
        protectedPlayers.clear();

        if (!dataFile.exists()) {
            plugin.getLogger().info("No data.yml found, starting fresh.");
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = yaml.getConfigurationSection("protected-players");
        if (section == null) return;

        long now = System.currentTimeMillis();
        for (String uuidStr : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in data.yml: " + uuidStr);
                continue;
            }

            long expiry = section.getLong(uuidStr);
            if (expiry > now) {
                protectedPlayers.put(uuid, expiry);
            }
        }

        plugin.getLogger().info("Loaded " + protectedPlayers.size() + " protected players.");
    }

    public void saveData() {
        saveToYaml(true);
    }

    /** Synchronous save for use during onDisable when async tasks cannot be scheduled. */
    public void saveDataSync() {
        saveToYaml(false);
    }

    private void saveToYaml(boolean async) {
        // Clean up expired entries before saving
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = protectedPlayers.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : protectedPlayers.entrySet()) {
            yaml.set("protected-players." + entry.getKey().toString(), entry.getValue());
        }

        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        yaml.save(dataFile);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save data.yml", e);
                    }
                }
            }.runTaskAsynchronously(plugin);
        } else {
            try {
                yaml.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save data.yml", e);
            }
        }
    }

    // ── Protection checks ────────────────────────────────────────────────────

    public boolean isProtected(UUID uuid) {
        Long expiry = protectedPlayers.get(uuid);
        if (expiry == null) return false;

        if (System.currentTimeMillis() >= expiry) {
            protectedPlayers.remove(uuid);
            saveData();
            return false;
        }
        return true;
    }

    public boolean isProtected(Player player) {
        return isProtected(player.getUniqueId());
    }

    // ── Add / Remove ─────────────────────────────────────────────────────────

    public void addProtection(UUID uuid) {
        long expiry = System.currentTimeMillis() + plugin.getProtectionDurationMillis();
        protectedPlayers.put(uuid, expiry);
        saveData();
    }

    public void addProtection(Player player) {
        addProtection(player.getUniqueId());
    }

    public boolean removeProtection(UUID uuid) {
        if (protectedPlayers.remove(uuid) != null) {
            saveData();
            return true;
        }
        return false;
    }

    public boolean removeProtection(Player player) {
        return removeProtection(player.getUniqueId());
    }

    // ── Time helpers ─────────────────────────────────────────────────────────

    public long getRemainingTime(UUID uuid) {
        Long expiry = protectedPlayers.get(uuid);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    public long getRemainingTime(Player player) {
        return getRemainingTime(player.getUniqueId());
    }

    /**
     * Format milliseconds into a human-readable string with proper pluralization.
     * Shows days and hours for large durations, omitting smaller units.
     * Shows minutes when under a day, and seconds when under an hour.
     */
    public String formatTime(long millis) {
        if (millis <= 0) return "0 seconds";

        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 && days == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.isEmpty() ? "0 seconds" : sb.toString();
    }

    /**
     * Get the number of currently protected players (non-expired).
     */
    public int getProtectedCount() {
        long now = System.currentTimeMillis();
        return (int) protectedPlayers.values().stream().filter(expiry -> expiry > now).count();
    }
}
