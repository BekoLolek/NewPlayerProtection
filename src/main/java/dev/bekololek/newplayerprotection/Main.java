package dev.bekololek.newplayerprotection;

import dev.bekololek.newplayerprotection.commands.NewPlayerProtectionCommand;
import dev.bekololek.newplayerprotection.listeners.JoinListener;
import dev.bekololek.newplayerprotection.listeners.PvPListener;
import dev.bekololek.newplayerprotection.managers.ProtectionManager;
import dev.bekololek.newplayerprotection.managers.StatsManager;
import dev.bekololek.newplayerprotection.stats.NewPlayerProtectionExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ProtectionManager protectionManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        protectionManager = new ProtectionManager(this);
        protectionManager.loadData();

        statsManager = new StatsManager(this);
        statsManager.load();
        statsManager.startAutoSave();

        // Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PvPListener(this, protectionManager, statsManager), this);
        pm.registerEvents(new JoinListener(this, protectionManager, statsManager), this);

        // Commands
        var cmd = getCommand("newplayerprotection");
        var handler = new NewPlayerProtectionCommand(this, protectionManager, statsManager);
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NewPlayerProtectionExpansion(this, statsManager, protectionManager).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("NewPlayerProtection.v1 - BL enabled.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) statsManager.saveSync();
        if (protectionManager != null) protectionManager.saveData();
        getLogger().info("NewPlayerProtection.v1 - BL disabled.");
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    /**
     * Reload config and protection data from disk.
     */
    public void reloadPlugin() {
        reloadConfig();
        protectionManager.loadData();
    }

    /**
     * Get a formatted message from config with color codes translated.
     */
    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        return translateColors(message);
    }

    /**
     * Translate ampersand color codes to section symbols.
     */
    public String translateColors(String message) {
        return message.replace("&", "\u00A7");
    }

    /**
     * Get the configured protection duration in milliseconds.
     */
    public long getProtectionDurationMillis() {
        long hours = getConfig().getLong("protection-duration-hours", 72);
        long minutes = getConfig().getLong("protection-duration-minutes", 0);
        return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);
    }
}
