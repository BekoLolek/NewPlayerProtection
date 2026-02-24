package dev.bekololek.newplayerprotection.stats;

import dev.bekololek.newplayerprotection.Main;
import dev.bekololek.newplayerprotection.managers.ProtectionManager;
import dev.bekololek.newplayerprotection.managers.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class NewPlayerProtectionExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final StatsManager statsManager;
    private final ProtectionManager protectionManager;

    public NewPlayerProtectionExpansion(Main plugin, StatsManager statsManager,
                                        ProtectionManager protectionManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.protectionManager = protectionManager;
    }

    @Override public @NotNull String getIdentifier() { return "newplayerprotection"; }
    @Override public @NotNull String getAuthor()     { return "Lolek"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // %newplayerprotection_stat_<name>%
        if (params.startsWith("stat_") && player != null) {
            String statName = params.substring(5);

            // Special computed stat: protection_time_remaining
            if (statName.equalsIgnoreCase("protection_time_remaining")) {
                long remaining = protectionManager.getRemainingTime(player.getUniqueId());
                return remaining > 0 ? protectionManager.formatTime(remaining) : "Expired";
            }

            // Special computed stat: protected (boolean)
            if (statName.equalsIgnoreCase("protected")) {
                return protectionManager.isProtected(player.getUniqueId()) ? "Yes" : "No";
            }

            return String.valueOf(statsManager.getPlayerStat(player.getUniqueId(), statName));
        }

        // %newplayerprotection_global_<name>%
        if (params.startsWith("global_")) {
            String statName = params.substring(7);
            return String.valueOf(statsManager.getGlobalStat(statName));
        }

        // %newplayerprotection_top_<name>_<pos>% — returns player name
        if (params.startsWith("top_") && !params.startsWith("topvalue_")) {
            return parseLeaderboardEntry(params.substring(4), false);
        }

        // %newplayerprotection_topvalue_<name>_<pos>% — returns value
        if (params.startsWith("topvalue_")) {
            return parseLeaderboardEntry(params.substring(9), true);
        }

        return null;
    }

    private String parseLeaderboardEntry(String rest, boolean valueOnly) {
        // rest = "attacks_blocked_1" — last segment after last _ is position
        int lastUnderscore = rest.lastIndexOf('_');
        if (lastUnderscore < 0) return null;
        String statName = rest.substring(0, lastUnderscore);
        int position;
        try {
            position = Integer.parseInt(rest.substring(lastUnderscore + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (position < 1) return null;

        List<Map.Entry<String, Number>> top = statsManager.getTopPlayers(statName, position);
        if (position > top.size()) return valueOnly ? "0" : "-";
        Map.Entry<String, Number> entry = top.get(position - 1);
        if (valueOnly) {
            return String.valueOf(entry.getValue());
        }
        return entry.getKey();
    }
}
