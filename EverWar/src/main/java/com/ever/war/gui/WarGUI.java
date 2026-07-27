package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.War;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class WarGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        String title = en ? "§0§l⚔ War & Siege" : "§0§l⚔ Война и осада";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        List<War> wars = plugin.getWarManager().getClanWars(clan.getClanId());

        inv.setItem(4, new ItemBuilder(Material.IRON_SWORD)
                .name("&c&l" + (en ? "War Status" : "Статус войн"))
                .lore("",
                        (en ? "&7Active wars: " : "&7Активных войн: ")
                                + (wars.isEmpty() ? "&a0" : "&c" + wars.size()),
                        (en ? "&7Wars won: &a" : "&7Побед: &a") + clan.getWarsWon(),
                        (en ? "&7Wars lost: &c" : "&7Поражений: &c") + clan.getWarsLost())
                .hideFlags()
                .build());

        // Активные войны
        for (int i = 0; i < Math.min(4, wars.size()); i++) {
            War war = wars.get(i);
            Clan opponent = plugin.getClanManager().getClanById(war.getOpponent(clan.getClanId()));
            String opName = opponent != null ? opponent.getName() : "Unknown";
            boolean isAttacker = war.isAttacker(clan.getClanId());

            inv.setItem(10 + i, new ItemBuilder(
                    isAttacker ? Material.DIAMOND_SWORD : Material.SHIELD)
                    .name("&c⚔ " + opName)
                    .lore("",
                            (en ? "&7Role: " : "&7Роль: ") + (isAttacker
                                    ? (en ? "&cAttacker" : "&cАтакующий")
                                    : (en ? "&bDefender" : "&bЗащитник")),
                            (en ? "&7Status: " : "&7Статус: ") + war.getStatusDisplay(lang),
                            (en ? "&7Score: &f" : "&7Счёт: &f") + war.getScoreDisplay(
                                    isAttacker ? clan.getName() : opName,
                                    isAttacker ? opName : clan.getName()),
                            war.getStatus() == War.WarStatus.PREPARATION
                                    ? (en ? "&eStarts in: &f" : "&eНачало через: &f")
                                    + TimeUtil.formatTime(war.getSecondsUntilStart(), lang) : "")
                    .hideFlags()
                    .build());
        }

        if (wars.isEmpty()) {
            inv.setItem(13, new ItemBuilder(Material.GREEN_WOOL)
                    .name("&a" + (en ? "No active wars" : "Нет активных войн"))
                    .lore("",
                            en ? "&7Peace time!" : "&7Мирное время!",
                            "",
                            en ? "&eDeclare war: /war war declare <clan>"
                                    : "&eОбъявить войну: /war war declare <клан>")
                    .build());
        }

        // Осада
        var siege = plugin.getSiegeManager().getActiveSiegeByAttacker(clan.getClanId());
        inv.setItem(16, new ItemBuilder(Material.TNT)
                .name("&4&l" + (en ? "Siege" : "Осада"))
                .lore("",
                        siege != null
                                ? "&c" + (en ? "Active! Progress: " : "Активна! Прогресс: ")
                                + siege.getProgressBar()
                                : "&a" + (en ? "No active siege" : "Нет активных осад"),
                        "",
                        en ? "&eStart: /war siege start" : "&eНачать: /war siege start")
                .build());

        inv.setItem(22, new ItemBuilder(Material.PAPER)
                .name("&e" + (en ? "Details in chat" : "Подробности в чат"))
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
                player.performCommand("war war status");
            }
            case 26 -> {
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
        }
    }
}