package dev.bekololek.newplayerprotection.listeners;

import dev.bekololek.newplayerprotection.Main;
import dev.bekololek.newplayerprotection.managers.ProtectionManager;
import dev.bekololek.newplayerprotection.managers.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {

    private final Main plugin;
    private final ProtectionManager protectionManager;
    private final StatsManager statsManager;

    public PvPListener(Main plugin, ProtectionManager protectionManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.statsManager = statsManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        // Bypass permission allows attacking anyone and being attacked
        if (attacker.hasPermission("newplayerprotection.bypass")) {
            return;
        }

        // Protected attacker cannot deal PvP damage
        if (protectionManager.isProtected(attacker)) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getMessage("you-are-protected"));
            statsManager.recordAttackPrevented(attacker.getUniqueId(), attacker.getName());
            return;
        }

        // Protected victim cannot receive PvP damage
        if (protectionManager.isProtected(victim)) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getMessage("cannot-hit-protected"));
            statsManager.recordAttackBlocked(victim.getUniqueId(), victim.getName());
        }
    }

    /**
     * Resolve the attacking player from direct melee or projectile damage.
     */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }

        return null;
    }
}
