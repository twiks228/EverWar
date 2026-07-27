package com.ever.war.config;

import com.ever.war.EverWar;

public class SettingsConfig {

    private final EverWar plugin;

    public SettingsConfig(EverWar plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // SettingsConfig делегирует всё в ConfigManager
        // Этот класс можно использовать для отдельного settings.yml в будущем
    }

    // Удобные методы-обёртки

    public String getLanguage() {
        return plugin.getConfigManager().getLanguage();
    }

    public int getMaxMembers() {
        return plugin.getConfigManager().getMaxMembers();
    }

    public int getChunksPerPlayer() {
        return plugin.getConfigManager().getChunksPerPlayer();
    }

    public int getPreparationTime() {
        return plugin.getConfigManager().getPreparationTime();
    }

    public boolean isFriendlyFire() {
        return plugin.getConfigManager().isFriendlyFire();
    }

    public boolean isExplosionProtection() {
        return plugin.getConfigManager().isExplosionProtection();
    }
}