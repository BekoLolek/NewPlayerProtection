package dev.bekololek.newplayerprotection.listeners;

import dev.bekololek.newplayerprotection.Main;
import dev.bekololek.newplayerprotection.managers.ProtectionManager;
import dev.bekololek.newplayerprotection.managers.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final Main plugin;
    private final ProtectionManager protectionManager;
    private final StatsManager statsManager;

    public JoinListener(Main plugin, ProtectionManager protectionManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.statsManager = statsManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update cached name in stats
        statsManager.updateName(player);

        if (!player.hasPlayedBefore()) {
            // Brand new player — grant protection
            protectionManager.addProtection(player);
            statsManager.recordNewProtection();

            long hours = plugin.getConfig().getLong("protection-duration-hours", 72);
            String message = plugin.getMessage("protection-granted")
                    .replace("%hours%", String.valueOf(hours));
            player.sendMessage(message);
        } else if (protectionManager.isProtected(player)) {
            // Returning player with active protection — show remaining time
            String timeRemaining = protectionManager.formatTime(
                    protectionManager.getRemainingTime(player));
            String message = plugin.getMessage("protection-active")
                    .replace("%time%", timeRemaining);
            player.sendMessage(message);
        }
    }
}
