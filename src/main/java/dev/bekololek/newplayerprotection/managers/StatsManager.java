package dev.bekololek.newplayerprotection.managers;

import dev.bekololek.newplayerprotection.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final Main plugin;
    private final File statsFile;
    private final Map<UUID, PlayerStats> players = new HashMap<>();
    private int totalPlayersEverProtected;

    // ── Stat schema ──────────────────────────────────────────────────────────
    // Defines every stat the website can render. Order here = display order.
    // Types: "int", "double", "string" — must match what is stored in the file.

    private static final List<StatDef> PLAYER_SCHEMA = List.of(
            new StatDef("attacks_blocked",    "Attacks Blocked",    "int", null, true),
            new StatDef("attacks_prevented",  "Attacks Prevented",  "int", null, true)
    );

    private static final List<StatDef> GLOBAL_SCHEMA = List.of(
            new StatDef("total_attacks_blocked",       "Total Attacks Blocked",       "int", null, false),
            new StatDef("total_players_protected",     "Currently Protected Players", "int", null, false),
            new StatDef("total_players_ever_protected", "Total Players Ever Protected", "int", null, false)
    );

    record StatDef(String key, String label, String type, String unit, boolean leaderboard) {}

    public StatsManager(Main plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    // ── Data class ───────────────────────────────────────────────────────────

    public static class PlayerStats {
        String name;
        int attacksBlocked;
        int attacksPrevented;
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    public void load() {
        if (!statsFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(statsFile);

        totalPlayersEverProtected = yaml.getInt("global.total_players_ever_protected", 0);

        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection sec = playersSection.getConfigurationSection(uuidStr);
            if (sec == null) continue;

            PlayerStats ps = new PlayerStats();
            ps.name = sec.getString("name", "Unknown");
            ps.attacksBlocked = sec.getInt("attacks_blocked", 0);
            ps.attacksPrevented = sec.getInt("attacks_prevented", 0);
            players.put(uuid, ps);
        }
        plugin.getLogger().info("Loaded stats for " + players.size() + " players.");
    }

    public void save() {
        saveToYaml(true);
    }

    /** Synchronous save — use during onDisable when async tasks cannot be scheduled. */
    public void saveSync() {
        saveToYaml(false);
    }

    private void saveToYaml(boolean async) {
        YamlConfiguration yaml = new YamlConfiguration();

        // ── Plugin identity ──────────────────────────────────────────────────
        yaml.set("plugin", "NewPlayerProtection");
        yaml.set("version", 1);

        // ── Schema (player stats) ────────────────────────────────────────────
        for (StatDef def : PLAYER_SCHEMA) {
            String base = "schema.player." + def.key();
            yaml.set(base + ".label", def.label());
            yaml.set(base + ".type", def.type());
            if (def.unit() != null) yaml.set(base + ".unit", def.unit());
            yaml.set(base + ".leaderboard", def.leaderboard());
        }

        // ── Schema (global stats) ────────────────────────────────────────────
        for (StatDef def : GLOBAL_SCHEMA) {
            String base = "schema.global." + def.key();
            yaml.set(base + ".label", def.label());
            yaml.set(base + ".type", def.type());
            if (def.unit() != null) yaml.set(base + ".unit", def.unit());
        }

        // ── Global values ────────────────────────────────────────────────────
        yaml.set("global.total_attacks_blocked",
                players.values().stream().mapToInt(p -> p.attacksBlocked).sum());
        yaml.set("global.total_players_protected",
                plugin.getProtectionManager().getProtectedCount());
        yaml.set("global.total_players_ever_protected", totalPlayersEverProtected);

        // ── Player data ──────────────────────────────────────────────────────
        for (var entry : players.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats ps = entry.getValue();
            yaml.set(path + ".name", ps.name);
            yaml.set(path + ".attacks_blocked", ps.attacksBlocked);
            yaml.set(path + ".attacks_prevented", ps.attacksPrevented);
        }

        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        yaml.save(statsFile);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
                    }
                }
            }.runTaskAsynchronously(plugin);
        } else {
            try {
                yaml.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
            }
        }
    }

    public void startAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() { save(); }
        }.runTaskTimer(plugin, 6000L, 6000L); // every 5 minutes
    }

    // ── Recording ────────────────────────────────────────────────────────────

    private PlayerStats getOrCreate(UUID uuid, String name) {
        PlayerStats ps = players.computeIfAbsent(uuid, k -> new PlayerStats());
        ps.name = name;
        return ps;
    }

    public void recordAttackBlocked(UUID victimUuid, String victimName) {
        PlayerStats ps = getOrCreate(victimUuid, victimName);
        ps.attacksBlocked++;
    }

    public void recordAttackPrevented(UUID attackerUuid, String attackerName) {
        PlayerStats ps = getOrCreate(attackerUuid, attackerName);
        ps.attacksPrevented++;
    }

    public void recordNewProtection() {
        totalPlayersEverProtected++;
    }

    /** Update cached name on join. */
    public void updateName(Player player) {
        PlayerStats ps = players.get(player.getUniqueId());
        if (ps != null) ps.name = player.getName();
    }

    // ── Per-player stat access ───────────────────────────────────────────────

    public Object getPlayerStat(UUID uuid, String statName) {
        PlayerStats ps = players.get(uuid);
        if (ps == null) return 0;
        return switch (statName.toLowerCase()) {
            case "attacks_blocked" -> ps.attacksBlocked;
            case "attacks_prevented" -> ps.attacksPrevented;
            default -> 0;
        };
    }

    // ── Global stat access ───────────────────────────────────────────────────

    public Object getGlobalStat(String statName) {
        return switch (statName.toLowerCase()) {
            case "total_attacks_blocked" ->
                    players.values().stream().mapToInt(p -> p.attacksBlocked).sum();
            case "total_players_protected" ->
                    plugin.getProtectionManager().getProtectedCount();
            case "total_players_ever_protected" ->
                    totalPlayersEverProtected;
            default -> 0;
        };
    }

    // ── Leaderboard ──────────────────────────────────────────────────────────

    /** Returns top players sorted descending by the given stat. Each entry is name -> value. */
    public List<Map.Entry<String, Number>> getTopPlayers(String statName, int limit) {
        List<Map.Entry<String, Number>> list = new ArrayList<>();
        for (var entry : players.entrySet()) {
            PlayerStats ps = entry.getValue();
            Number value = switch (statName.toLowerCase()) {
                case "attacks_blocked" -> ps.attacksBlocked;
                case "attacks_prevented" -> ps.attacksPrevented;
                default -> 0;
            };
            list.add(new AbstractMap.SimpleEntry<>(ps.name, value));
        }
        list.sort((a, b) -> Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** Valid stat names for leaderboard display (derived from schema). */
    public static List<String> leaderboardStats() {
        return PLAYER_SCHEMA.stream().filter(StatDef::leaderboard).map(StatDef::key).toList();
    }

    /** Human-readable label for a stat name (derived from schema). */
    public static String statLabel(String statName) {
        for (StatDef def : PLAYER_SCHEMA) {
            if (def.key().equals(statName.toLowerCase())) return def.label();
        }
        for (StatDef def : GLOBAL_SCHEMA) {
            if (def.key().equals(statName.toLowerCase())) return def.label();
        }
        return statName;
    }
}
