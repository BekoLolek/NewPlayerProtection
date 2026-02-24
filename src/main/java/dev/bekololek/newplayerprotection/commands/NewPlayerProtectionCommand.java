package dev.bekololek.newplayerprotection.commands;

import dev.bekololek.newplayerprotection.Main;
import dev.bekololek.newplayerprotection.managers.ProtectionManager;
import dev.bekololek.newplayerprotection.managers.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NewPlayerProtectionCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ProtectionManager protectionManager;
    private final StatsManager statsManager;

    public NewPlayerProtectionCommand(Main plugin, ProtectionManager protectionManager,
                                       StatsManager statsManager) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "me"     -> handleMe(sender);
            case "reload" -> handleReload(sender);
            case "add"    -> handleAdd(sender, label, args);
            case "remove" -> handleRemove(sender, label, args);
            case "check"  -> handleCheck(sender, label, args);
            case "stats"  -> handleStats(sender, label, args);
            default       -> sendHelp(sender, label);
        }

        return true;
    }

    // ── me ───────────────────────────────────────────────────────────────────

    private void handleMe(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.translateColors("&cThis command can only be used by players."));
            return;
        }

        if (protectionManager.isProtected(player)) {
            String time = protectionManager.formatTime(protectionManager.getRemainingTime(player));
            sender.sendMessage(plugin.getMessage("self-protected").replace("%time%", time));
        } else {
            sender.sendMessage(plugin.getMessage("self-not-protected"));
        }
    }

    // ── reload ───────────────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("newplayerprotection.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getMessage("config-reloaded"));
    }

    // ── add ──────────────────────────────────────────────────────────────────

    private void handleAdd(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("newplayerprotection.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.translateColors("&cUsage: /" + label + " add <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            return;
        }

        if (protectionManager.isProtected(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessage("player-already-protected"));
            return;
        }

        protectionManager.addProtection(target.getUniqueId());
        statsManager.recordNewProtection();
        String name = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(plugin.getMessage("player-added").replace("%player%", name));

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                long hours = plugin.getConfig().getLong("protection-duration-hours", 72);
                onlineTarget.sendMessage(plugin.getMessage("protection-granted")
                        .replace("%hours%", String.valueOf(hours)));
            }
        }
    }

    // ── remove ───────────────────────────────────────────────────────────────

    private void handleRemove(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("newplayerprotection.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.translateColors("&cUsage: /" + label + " remove <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            return;
        }

        if (!protectionManager.isProtected(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessage("player-not-protected"));
            return;
        }

        protectionManager.removeProtection(target.getUniqueId());
        String name = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(plugin.getMessage("player-removed").replace("%player%", name));

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.sendMessage(plugin.getMessage("protection-expired"));
            }
        }
    }

    // ── check ────────────────────────────────────────────────────────────────

    private void handleCheck(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("newplayerprotection.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.translateColors("&cUsage: /" + label + " check <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            return;
        }

        String name = target.getName() != null ? target.getName() : args[1];

        if (protectionManager.isProtected(target.getUniqueId())) {
            String time = protectionManager.formatTime(
                    protectionManager.getRemainingTime(target.getUniqueId()));
            sender.sendMessage(plugin.getMessage("check-protected")
                    .replace("%player%", name)
                    .replace("%time%", time));
        } else {
            sender.sendMessage(plugin.getMessage("check-not-protected")
                    .replace("%player%", name));
        }
    }

    // ── stats ────────────────────────────────────────────────────────────────

    private void handleStats(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("newplayerprotection.stats")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        // /npp stats top [stat]
        if (args.length >= 2 && args[1].equalsIgnoreCase("top")) {
            String statName = args.length >= 3 ? args[2].toLowerCase() : "attacks_blocked";
            if (!StatsManager.leaderboardStats().contains(statName)) {
                sender.sendMessage(Component.text("Unknown stat. Available: "
                        + String.join(", ", StatsManager.leaderboardStats()), NamedTextColor.RED));
                return;
            }
            List<Map.Entry<String, Number>> top = statsManager.getTopPlayers(statName, 10);
            sender.sendMessage(Component.text("--- Top " + StatsManager.statLabel(statName) + " ---",
                    NamedTextColor.GOLD));
            if (top.isEmpty()) {
                sender.sendMessage(Component.text("  No data yet.", NamedTextColor.GRAY));
                return;
            }
            for (int i = 0; i < top.size(); i++) {
                Map.Entry<String, Number> entry = top.get(i);
                sender.sendMessage(Component.text("  " + (i + 1) + ". " + entry.getKey()
                        + " - " + entry.getValue(), NamedTextColor.AQUA));
            }
            return;
        }

        // /npp stats <player>
        if (args.length >= 2) {
            if (!sender.hasPermission("newplayerprotection.stats.others")) {
                sender.sendMessage(Component.text("No permission to view other players' stats.",
                        NamedTextColor.RED));
                return;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(Component.text("Player '" + args[1] + "' not found.",
                        NamedTextColor.RED));
                return;
            }
            String name = target.getName() != null ? target.getName() : args[1];
            showPlayerStats(sender, target.getUniqueId(), name);
            return;
        }

        // /npp stats (own)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Usage: /" + label + " stats <player>",
                    NamedTextColor.YELLOW));
            return;
        }
        showPlayerStats(sender, player.getUniqueId(), player.getName());
    }

    private void showPlayerStats(CommandSender sender, java.util.UUID uuid, String name) {
        sender.sendMessage(Component.text("--- " + name + "'s Protection Stats ---",
                NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Attacks Blocked: "
                + statsManager.getPlayerStat(uuid, "attacks_blocked"), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Attacks Prevented: "
                + statsManager.getPlayerStat(uuid, "attacks_prevented"), NamedTextColor.AQUA));

        // Protection time remaining (computed, if protected)
        long remaining = protectionManager.getRemainingTime(uuid);
        if (remaining > 0) {
            sender.sendMessage(Component.text("  Protection Remaining: "
                    + protectionManager.formatTime(remaining), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("  Protection: Expired",
                    NamedTextColor.GRAY));
        }

        // Global highlights
        sender.sendMessage(Component.text("--- Global ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Total Attacks Blocked: "
                + statsManager.getGlobalStat("total_attacks_blocked"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Currently Protected: "
                + statsManager.getGlobalStat("total_players_protected"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Total Ever Protected: "
                + statsManager.getGlobalStat("total_players_ever_protected"), NamedTextColor.YELLOW));
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("NewPlayerProtection commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /" + label + " me              - Check your protection status",
                NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /" + label + " stats           - View protection statistics",
                NamedTextColor.YELLOW));
        if (sender.hasPermission("newplayerprotection.admin")) {
            sender.sendMessage(Component.text("  /" + label + " reload          - Reload config",
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /" + label + " add <player>    - Grant protection",
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /" + label + " remove <player> - Remove protection",
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /" + label + " check <player>  - Check a player's status",
                    NamedTextColor.YELLOW));
        }
    }

    // ── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("me");
            options.add("stats");
            if (sender.hasPermission("newplayerprotection.admin")) {
                options.addAll(Arrays.asList("reload", "add", "remove", "check"));
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            // Admin subcommands: tab-complete online player names
            if (sender.hasPermission("newplayerprotection.admin")
                    && (sub.equals("add") || sub.equals("remove") || sub.equals("check"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Stats: tab-complete "top" and online player names
            if (sub.equals("stats")) {
                List<String> options = new ArrayList<>();
                options.add("top");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    options.add(p.getName());
                }
                return options.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("stats")
                && args[1].equalsIgnoreCase("top")) {
            return StatsManager.leaderboardStats().stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
