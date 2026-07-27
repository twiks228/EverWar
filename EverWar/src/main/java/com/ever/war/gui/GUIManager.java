package com.ever.war.gui;

import com.ever.war.EverWar;
import org.bukkit.entity.Player;

/**
 * Центральный менеджер GUI.
 * Утилитный класс для управления GUI окнами.
 */
public class GUIManager {

    private final EverWar plugin;

    public GUIManager(EverWar plugin) {
        this.plugin = plugin;
    }

    /**
     * Открыть главное меню
     */
    public void openMainMenu(Player player) {
        ClanMenuGUI.open(player);
    }

    /**
     * Открыть карту территорий
     */
    public void openTerritoryMap(Player player) {
        TerritoryMapGUI.open(player);
    }

    /**
     * Открыть дипломатию
     */
    public void openDiplomacy(Player player) {
        DiplomacyGUI.open(player);
    }

    /**
     * Открыть войну
     */
    public void openWar(Player player) {
        WarGUI.open(player);
    }

    /**
     * Открыть рейтинг
     */
    public void openRanking(Player player) {
        RankingGUI.open(player);
    }

    /**
     * Открыть настройки
     */
    public void openSettings(Player player) {
        SettingsGUI.open(player);
    }

    /**
     * Открыть снабжение
     */
    public void openSupply(Player player) {
        SupplyGUI.open(player);
    }

    /**
     * Открыть страну
     */
    public void openCountry(Player player) {
        CountryGUI.open(player);
    }

    /**
     * Проверить, является ли заголовок инвентаря нашим GUI
     */
    public static boolean isEverWarGUI(String title) {
        return title != null && (
                title.contains("EverWar")
                        || title.contains("⚔")
                        || title.contains("🏆")
                        || title.contains("🤝")
                        || title.contains("📦")
                        || title.contains("🗺")
                        || title.contains("👥")
                        || title.contains("⚙")
                        || title.contains("🏴")
                        || title.contains("👑")
                        || title.contains("🛡")
        );
    }
}