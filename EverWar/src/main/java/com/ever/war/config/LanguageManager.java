package com.ever.war.config;

import com.ever.war.EverWar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class LanguageManager {

    private final EverWar plugin;
    private FileConfiguration messages;
    private String language;

    public LanguageManager(EverWar plugin) {
        this.plugin = plugin;
    }

    public void load() {
        language = plugin.getConfigManager().getLanguage();
        String fileName = "messages_" + language + ".yml";

        File file = new File(plugin.getDataFolder(), fileName);

        // Если файла нет — копируем из ресурсов
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception e) {
                plugin.getLogger().warning("Файл " + fileName + " не найден в ресурсах. Используем ru.");
                fileName = "messages_ru.yml";
                file = new File(plugin.getDataFolder(), fileName);
                if (!file.exists()) {
                    plugin.saveResource("messages_ru.yml", false);
                }
            }
        }

        messages = YamlConfiguration.loadConfiguration(file);

        // Загружаем дефолтные значения из jar
        InputStream defStream = plugin.getResource(fileName);
        if (defStream == null) {
            defStream = plugin.getResource("messages_ru.yml");
        }
        if (defStream != null) {
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8)
            );
            messages.setDefaults(defConfig);
        }

        plugin.getLogger().info("Язык загружен: " + language + " (" + fileName + ")");
    }

    /**
     * Получить сообщение по ключу.
     * Автоматически подставляет префикс если есть {prefix}
     */
    public String get(String key) {
        String prefix = messages.getString("prefix", "&8[&6EverWar&8] ");
        String msg = messages.getString(key, "&cСообщение не найдено: " + key);
        return msg.replace("{prefix}", prefix);
    }

    /**
     * Получить сообщение и заменить плейсхолдеры.
     * Пример: get("clan-created", "{clan}", "MyClан", "{tag}", "MCL")
     */
    public String get(String key, String... placeholders) {
        String msg = get(key);
        if (placeholders.length % 2 != 0) return msg;
        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }

    /**
     * Проверить есть ли ключ
     */
    public boolean has(String key) {
        return messages.contains(key);
    }

    public String getLanguage() {
        return language;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}