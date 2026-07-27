package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class DiplomacyGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        String title = en ? "§0§l🤝 Diplomacy" : "§0§l🤝 Дипломатия";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        List<Clan> allies = plugin.getDiplomacyManager().getAllies(clan.getClanId());
        List<Clan> enemies = plugin.getDiplomacyManager().getEnemies(clan.getClanId());

        // Заголовок
        inv.setItem(4, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&d&l" + (en ? "Diplomacy" : "Дипломатия"))
                .lore("",
                        "&a🤝 " + (en ? "Allies: " : "Союзников: ") + "&f" + allies.size(),
                        "&c⚔ " + (en ? "Enemies: " : "Врагов: ") + "&f" + enemies.size())
                .build());

        // Союзники (слоты 9-12)
        inv.setItem(9, new ItemBuilder(Material.EMERALD)
                .name("&a&l" + (en ? "Allies" : "Союзники"))
                .lore("", en ? "&7Your allied clans" : "&7Ваши союзные кланы")
                .glow()
                .build());

        for (int i = 0; i < Math.min(3, allies.size()); i++) {
            Clan ally = allies.get(i);
            inv.setItem(10 + i, new ItemBuilder(Material.GREEN_WOOL)
                    .name("&a" + ally.getName() + " &7[" + ally.getTag() + "]")
                    .lore("",
                            (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", ally.getTotalPower()),
                            (en ? "&7Members: &f" : "&7Участников: &f") + ally.getMemberCount(),
                            "",
                            (en ? "&cClick to break alliance" : "&cНажмите для разрыва союза"))
                    .build());
        }

        // Враги (слоты 14-17)
        inv.setItem(14, new ItemBuilder(Material.REDSTONE)
                .name("&c&l" + (en ? "Enemies" : "Враги"))
                .lore("", en ? "&7Your enemy clans" : "&7Ваши вражеские кланы")
                .glow()
                .build());

        for (int i = 0; i < Math.min(3, enemies.size()); i++) {
            Clan enemy = enemies.get(i);
            inv.setItem(15 + i, new ItemBuilder(Material.RED_WOOL)
                    .name("&c" + enemy.getName() + " &7[" + enemy.getTag() + "]")
                    .lore("",
                            (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", enemy.getTotalPower()),
                            (en ? "&7Members: &f" : "&7Участников: &f") + enemy.getMemberCount(),
                            "",
                            (en ? "&eClick to set neutral" : "&eНажмите для нейтралитета"))
                    .build());
        }

        // Список в чате
        inv.setItem(22, new ItemBuilder(Material.PAPER)
                .name("&e" + (en ? "Full list in chat" : "Полный список в чат"))
                .build());

        inv.setItem(26, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back" : "Назад"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot) {
        switch (slot) {
            case 22 -> {
                player.closeInventory();
                player.performCommand("war diplomacy list");
            }
            case 26 -> {
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
        }
    }
}