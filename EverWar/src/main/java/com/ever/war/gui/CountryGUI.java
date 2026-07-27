package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Country;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CountryGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        Country country = plugin.getCountryManager().getCountryByClan(clan.getClanId());

        String title = en ? "§0§l🏴 Country" : "§0§l🏴 Страна";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        if (country == null) {
            // Не в стране
            inv.setItem(4, new ItemBuilder(Material.BLUE_BANNER)
                    .name("&b&l" + (en ? "Country" : "Страна"))
                    .lore("",
                            en ? "&7You are not part of any country"
                                    : "&7Ваш клан не состоит в стране",
                            "",
                            en ? "&7Create or join one!" : "&7Создайте или вступите!")
                    .hideFlags()
                    .build());

            inv.setItem(11, new ItemBuilder(Material.EMERALD)
                    .name("&a" + (en ? "Create Country" : "Создать страну"))
                    .lore("",
                            en ? "&7/war country create <name> <tag>"
                                    : "&7/war country create <имя> <тег>",
                            "",
                            en ? "&aClick to create" : "&aНажмите для создания")
                    .build());

            inv.setItem(15, new ItemBuilder(Material.BOOK)
                    .name("&e" + (en ? "Country List" : "Список стран"))
                    .lore("", en ? "&7Browse countries" : "&7Просмотр стран")
                    .build());

        } else {
            // В стране
            boolean isLeader = country.isLeaderClan(clan.getClanId());

            inv.setItem(4, new ItemBuilder(Material.BLUE_BANNER)
                    .name("&b&l" + country.getFormattedName() + " &7[" + country.getTag() + "]")
                    .lore("",
                            (en ? "&7Clans: &f" : "&7Кланов: &f") + country.getClanCount(),
                            (en ? "&7Leader clan: &f" : "&7Лидер-клан: &f")
                                    + (isLeader ? "&a" + clan.getName() : "Unknown"),
                            "",
                            isLeader
                                    ? (en ? "&6★ You are the leader!" : "&6★ Вы лидер-клан!")
                                    : "")
                    .hideFlags()
                    .glow()
                    .build());

            // Кланы в стране
            var clanIds = country.getClanIds();
            for (int i = 0; i < Math.min(7, clanIds.size()); i++) {
                Clan memberClan = plugin.getClanManager().getClanById(clanIds.get(i));
                if (memberClan == null) continue;

                boolean isClanLeader = country.isLeaderClan(memberClan.getClanId());

                inv.setItem(10 + i, new ItemBuilder(
                        isClanLeader ? Material.GOLD_BLOCK : Material.IRON_BLOCK)
                        .name((isClanLeader ? "&6★ " : "&f") + memberClan.getName()
                                + " &7[" + memberClan.getTag() + "]")
                        .lore("",
                                (en ? "&7Members: &f" : "&7Участников: &f") + memberClan.getMemberCount(),
                                (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", memberClan.getTotalPower()))
                        .build());
            }

            // Пригласить клан (только лидер-клан)
            if (isLeader) {
                inv.setItem(19, new ItemBuilder(Material.WRITABLE_BOOK)
                        .name("&a" + (en ? "Invite Clan" : "Пригласить клан"))
                        .lore("",
                                en ? "&7/war country invite <clan>"
                                        : "&7/war country invite <клан>")
                        .build());
            }

            // Покинуть страну
            if (!isLeader) {
                inv.setItem(21, new ItemBuilder(Material.IRON_DOOR)
                        .name("&c" + (en ? "Leave Country" : "Покинуть страну"))
                        .build());
            }

            // Удалить страну (только лидер)
            if (isLeader) {
                inv.setItem(23, new ItemBuilder(Material.TNT)
                        .name("&c&l" + (en ? "DELETE COUNTRY" : "УДАЛИТЬ СТРАНУ"))
                        .build());
            }
        }

        inv.setItem(22, new ItemBuilder(Material.PAPER)
                .name("&e" + (en ? "Country List" : "Список стран"))
                .build());

        inv.setItem(26, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back" : "Назад"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot) {
        switch (slot) {
            case 11 -> {
                player.closeInventory();
                MessageUtil.sendSuggest(player,
                        "&a[Создать страну]",
                        "&aНажмите и введите название и тег",
                        "/war country create ");
            }
            case 15, 22 -> {
                player.closeInventory();
                player.performCommand("war country list");
            }
            case 19 -> {
                player.closeInventory();
                MessageUtil.sendSuggest(player,
                        "&a[Пригласить клан]",
                        "&aНажмите и введите название клана",
                        "/war country invite ");
            }
            case 21 -> {
                player.closeInventory();
                player.performCommand("war country leave");
            }
            case 23 -> {
                player.closeInventory();
                MessageUtil.sendClickable(player,
                        "&c[УДАЛИТЬ СТРАНУ]",
                        "&c⚠ Нажмите для удаления",
                        "/war country delete");
            }
            case 26 -> {
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
        }
    }
}