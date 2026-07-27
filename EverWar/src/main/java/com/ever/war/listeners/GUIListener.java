package com.ever.war.listeners;

import com.ever.war.EverWar;
import com.ever.war.gui.ClanMenuGUI;
import com.ever.war.gui.CountryGUI;
import com.ever.war.gui.DiplomacyGUI;
import com.ever.war.gui.MembersGUI;
import com.ever.war.gui.RankingGUI;
import com.ever.war.gui.SettingsGUI;
import com.ever.war.gui.SupplyGUI;
import com.ever.war.gui.TerritoryMapGUI;
import com.ever.war.gui.WarGUI;
import com.ever.war.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {

    public GUIListener(EverWar plugin) {
        // plugin используется через getInstance() в GUI классах
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!isEverWarGUI(title)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        MessageUtil.soundClick(player);

        // Определяем GUI по заголовку и обрабатываем клик
        if (title.contains("Главное меню") || title.contains("Main Menu")
                || title.contains("👑")) {
            ClanMenuGUI.handleClick(player, slot);
        }
        else if (title.contains("Участники") || title.contains("Members")
                || title.contains("👥")) {
            MembersGUI.handleClick(player, slot, event.getInventory());
        }
        else if (title.contains("Карта") || title.contains("Map")
                || title.contains("Территории") || title.contains("Territory Map")
                || title.contains("🗺")) {
            TerritoryMapGUI.handleClick(player, slot);
        }
        else if (title.contains("Дипломатия") || title.contains("Diplomacy")
                || title.contains("🤝")) {
            DiplomacyGUI.handleClick(player, slot);
        }
        else if (title.contains("Война") || title.contains("War")
                || title.contains("Siege") || title.contains("Осада")
                || title.contains("⚔")) {
            WarGUI.handleClick(player, slot);
        }
        else if (title.contains("Рейтинг") || title.contains("Ranking")
                || title.contains("🏆")) {
            RankingGUI.handleClick(player, slot);
        }
        else if (title.contains("Настройки") || title.contains("Settings")
                || title.contains("⚙")) {
            SettingsGUI.handleClick(player, slot);
        }
        else if (title.contains("Снабжение") || title.contains("Склад")
                || title.contains("Supply") || title.contains("📦")) {
            SupplyGUI.handleClick(player, slot);
        }
        else if (title.contains("Страна") || title.contains("Country")
                || title.contains("🏴")) {
            CountryGUI.handleClick(player, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (isEverWarGUI(title)) {
            event.setCancelled(true);
        }
    }

    /**
     * Проверяет — принадлежит ли инвентарь плагину EverWar
     */
    private boolean isEverWarGUI(String title) {
        if (title == null) return false;

        return title.contains("EverWar")
                || title.contains("👑")
                || title.contains("👥")
                || title.contains("🗺")
                || title.contains("⚔")
                || title.contains("🤝")
                || title.contains("🏆")
                || title.contains("⚙")
                || title.contains("📦")
                || title.contains("🏴")
                || title.contains("🛡")
                || title.contains("Главное меню")
                || title.contains("Main Menu")
                || title.contains("Клан")
                || title.contains("Участники")
                || title.contains("Members")
                || title.contains("Карта")
                || title.contains("Territory")
                || title.contains("Территории")
                || title.contains("Дипломатия")
                || title.contains("Diplomacy")
                || title.contains("Война")
                || title.contains("War")
                || title.contains("Siege")
                || title.contains("Осада")
                || title.contains("Рейтинг")
                || title.contains("Ranking")
                || title.contains("Настройки")
                || title.contains("Settings")
                || title.contains("Снабжение")
                || title.contains("Склад")
                || title.contains("Supply")
                || title.contains("Страна")
                || title.contains("Country");
    }
}