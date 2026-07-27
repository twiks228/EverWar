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

public class RankingGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        String title = en ? "§0§l🏆 Clan Ranking" : "§0§l🏆 Рейтинг кланов";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        inv.setItem(4, new ItemBuilder(Material.GOLD_INGOT)
                .name("&6&l" + (en ? "Top Clans" : "Топ кланов"))
                .lore("", en ? "&7Ranked by power" : "&7По мощи")
                .glow()
                .build());

        List<Clan> topClans = plugin.getClanManager().getTopClans(9);
        Material[] trophies = {
                Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.LAPIS_BLOCK,
                Material.REDSTONE_BLOCK, Material.COAL_BLOCK, Material.QUARTZ_BLOCK
        };

        for (int i = 0; i < topClans.size(); i++) {
            Clan c = topClans.get(i);
            int slot = 9 + i;
            Material mat = i < trophies.length ? trophies[i] : Material.STONE;
            String prefix = i == 0 ? "&6&l" : i == 1 ? "&f&l" : i == 2 ? "&6" : "&7";

            inv.setItem(slot, new ItemBuilder(mat)
                    .name(prefix + "#" + (i + 1) + " " + c.getName() + " &7[" + c.getTag() + "]")
                    .lore("",
                            (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", c.getTotalPower()),
                            (en ? "&7Members: &f" : "&7Участников: &f") + c.getMemberCount(),
                            (en ? "&7Territories: &f" : "&7Территорий: &f")
                                    + plugin.getTerritoryManager().getClanTerritoryCount(c.getClanId()),
                            (en ? "&7Wars: &a" : "&7Войн: &a") + c.getWarsWon() + "&7/&c" + c.getWarsLost(),
                            (en ? "&7Kills: &f" : "&7Убийств: &f") + c.getTotalKills())
                    .build());
        }

        inv.setItem(26, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back" : "Назад"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot) {
        if (slot == 26) {
            player.closeInventory();
            ClanMenuGUI.open(player);
        }
    }
}