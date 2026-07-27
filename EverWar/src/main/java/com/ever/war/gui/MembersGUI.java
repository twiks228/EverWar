package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MembersGUI {

    private static final int MEMBERS_PER_PAGE = 9;
    private static final Map<UUID, Integer> playerPages = new HashMap<>();

    public static void open(Player player, Clan clan, int page) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        String title = en
                ? "§0§l👥 Members — " + clan.getName()
                : "§0§l👥 Участники — " + clan.getName();
        Inventory inv = Bukkit.createInventory(null, 27, title);

        playerPages.put(player.getUniqueId(), page);

        // Заполняем стеклом
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Заголовок
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&b&l" + (en ? "Members" : "Участники"))
                .lore("",
                        (en ? "&7Total: &f" : "&7Всего: &f") + clan.getMemberCount(),
                        (en ? "&7Online: &a" : "&7Онлайн: &a") + clan.getOnlineMembers().size(),
                        (en ? "&7Page: &f" : "&7Страница: &f") + (page + 1))
                .build());

        // Сортируем участников: онлайн первые, потом по роли
        List<ClanMember> members = new ArrayList<>(clan.getMemberList());
        members.sort((a, b) -> {
            if (a.isOnline() != b.isOnline()) return a.isOnline() ? -1 : 1;
            return Integer.compare(b.getRole().getLevel(), a.getRole().getLevel());
        });

        // Пагинация
        int start = page * MEMBERS_PER_PAGE;
        int end = Math.min(start + MEMBERS_PER_PAGE, members.size());
        int totalPages = (int) Math.ceil((double) members.size() / MEMBERS_PER_PAGE);

        // Слоты для участников: 9-17
        for (int i = start; i < end; i++) {
            ClanMember member = members.get(i);
            int slot = 9 + (i - start);

            var offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUUID());

            ItemStack head = ItemBuilder.playerHead(offlinePlayer)
                    .name(member.getRole().getChatColor() + member.getRole().getIcon()
                            + " " + member.getPlayerName())
                    .lore("",
                            (en ? "&7Role: " : "&7Роль: ") + member.getRoleDisplay(lang),
                            (en ? "&7Status: " : "&7Статус: ") + member.getStatusDisplay(lang),
                            (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", member.getPower()),
                            (en ? "&7Kills: &f" : "&7Убийств: &f") + member.getKills()
                                    + (en ? " | Deaths: &f" : " | Смертей: &f") + member.getDeaths(),
                            (en ? "&7KDR: &f" : "&7К/С: &f") + member.getKillDeathRatio(),
                            (en ? "&7Joined: &f" : "&7Вступил: &f") + TimeUtil.timeAgo(member.getJoinedAt(), lang),
                            "",
                            member.isOnline() ? "&a● Онлайн" : "&7Был: " + TimeUtil.timeAgo(member.getLastSeen(), lang))
                    .build();

            inv.setItem(slot, head);
        }

        // Навигация
        if (page > 0) {
            inv.setItem(18, new ItemBuilder(Material.ARROW)
                    .name("&e◀ " + (en ? "Previous page" : "Предыдущая страница"))
                    .build());
        }

        if (page < totalPages - 1) {
            inv.setItem(25, new ItemBuilder(Material.ARROW)
                    .name("&e▶ " + (en ? "Next page" : "Следующая страница"))
                    .build());
        }

        // Назад
        inv.setItem(26, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back to menu" : "Назад в меню"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot, Inventory inv) {
        EverWar plugin = EverWar.getInstance();
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            player.closeInventory();
            return;
        }

        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        switch (slot) {
            case 18 -> {
                if (page > 0) {
                    open(player, clan, page - 1);
                }
            }
            case 25 -> {
                open(player, clan, page + 1);
            }
            case 26 -> {
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
        }
    }
}