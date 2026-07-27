package com.ever.war.config;

import com.ever.war.EverWar;

public class MessagesConfig {

    private final EverWar plugin;

    public MessagesConfig(EverWar plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // MessagesConfig делегирует всё в LanguageManager
        // Здесь можно добавить кэширование сообщений если нужно
    }

    /**
     * Получить сообщение через LanguageManager
     */
    public String get(String key) {
        return plugin.getLanguageManager().get(key);
    }

    /**
     * Получить сообщение с плейсхолдерами
     */
    public String get(String key, String... placeholders) {
        return plugin.getLanguageManager().get(key, placeholders);
    }
}