package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.utils.ColorUtil;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ClanMenuGUI {

    private static final String TITLE_RU = "§0§l👑 EverWar — Главное меню";
    private static final String TITLE_EN = "§0§l👑 EverWar — Main Menu";

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        String title = en ? TITLE_EN : TITLE_RU;
        Inventory inv = Bukkit.createInventory(null, 27, title);

        Clan clan = plugin.getClanManager().getClanByPlayer(player);

        // Заполняем декоративным стеклом
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        if (clan == null) {
            // Игрок не в клане — показываем меню создания
            inv.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                    .name("&6&l" + (en ? "You're not in a clan" : "Вы не в клане"))
                    .lore("",
                            en ? "&7Create or join a clan!" : "&7Создайте или вступите в клан!",
                            "",
                            en ? "&eClick to create" : "&eНажмите для создания")
                    .glow()
                    .build());

            inv.setItem(11, new ItemBuilder(Material.EMERALD)
                    .name("&a" + (en ? "Create Clan" : "Создать клан"))
                    .lore("",
                            en ? "&7/war clan create <name> <tag>" : "&7/war clan create <имя> <тег>",
                            "",
                            en ? "&aClick to create" : "&aНажмите для создания")
                    .build());

            inv.setItem(15, new ItemBuilder(Material.BOOK)
                    .name("&e" + (en ? "Clan List" : "Список кланов"))
                    .lore("",
                            en ? "&7Browse existing clans" : "&7Просмотр существующих кланов",
                            "",
                            en ? "&eClick to view" : "&eНажмите для просмотра")
                    .build());

            inv.setItem(22, new ItemBuilder(Material.GOLD_INGOT)
                    .name("&6" + (en ? "Clan Ranking" : "Рейтинг кланов"))
                    .lore("",
                            en ? "&7Top clans by power" : "&7Топ кланов по мощи",
                            "",
                            en ? "&eClick to view" : "&eНажмите для просмотра")
                    .build());

            inv.setItem(26, new ItemBuilder(Material.BARRIER)
                    .name("&c" + (en ? "Close" : "Закрыть"))
                    .build());

        } else {
            // Игрок в клане — полное меню
            ClanMember member = clan.getMember(player.getUniqueId());
            String roleName = member != null ? member.getRole().getName(lang) : "?";
            String roleIcon = member != null ? member.getRole().getIcon() : "";

            // [4] Профиль клана
            inv.setItem(4, new ItemBuilder(Material.GOLDEN_HELMET)
                    .name("&6&l" + clan.getFormattedName() + " &7[" + clan.getTag() + "]")
                    .lore("",
                            (en ? "&7Leader: &f" : "&7Лидер: &f") + getLeaderName(clan),
                            (en ? "&7Your role: " : "&7Ваша роль: ") + member.getRole().getChatColor() + roleIcon + " " + roleName,
                            (en ? "&7Members: &f" : "&7Участников: &f") + clan.getMemberCount() + "/" + plugin.getConfigManager().getMaxMembers(),
                            (en ? "&7Online: &a" : "&7Онлайн: &a") + clan.getOnlineMembers().size(),
                            (en ? "&7Power: &f" : "&7Мощь: &f") + String.format("%.0f", clan.getTotalPower()),
                            (en ? "&7Territories: &f" : "&7Территорий: &f") + plugin.getTerritoryManager().getClanTerritoryCount(clan.getClanId()),
                            "",
                            (en ? "&eClick for details" : "&eНажмите для подробностей"))
                    .hideFlags()
                    .glow()
                    .build());

            // [10] Участники
            inv.setItem(10, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&b&l" + (en ? "👥 Members" : "👥 Участники"))
                    .lore("",
                            (en ? "&7View clan members" : "&7Просмотр участников клана"),
                            (en ? "&7Members: &f" : "&7Участников: &f") + clan.getMemberCount(),
                            "",
                            (en ? "&eClick to view" : "&eНажмите для просмотра"))
                    .build());

            // [11] Карта территорий
            inv.setItem(11, new ItemBuilder(Material.FILLED_MAP)
                    .name("&a&l" + (en ? "🗺 Territory Map" : "🗺 Карта территорий"))
                    .lore("",
                            (en ? "&7View territory map" : "&7Просмотр карты территорий"),
                            (en ? "&7Territories: &f" : "&7Территорий: &f") + plugin.getTerritoryManager().getClanTerritoryCount(clan.getClanId()),
                            "",
                            (en ? "&eClick to view" : "&eНажмите для просмотра"))
                    .build());

            // [12] Война и осада
            int warCount = plugin.getWarManager().getClanWars(clan.getClanId()).size();
            inv.setItem(12, new ItemBuilder(Material.IRON_SWORD)
                    .name("&c&l" + (en ? "⚔ War & Siege" : "⚔ Война и осада"))
                    .lore("",
                            (en ? "&7Manage wars" : "&7Управление войнами"),
                            (en ? "&7Active wars: " : "&7Активных войн: ") + (warCount > 0 ? "&c" + warCount : "&a0"),
                            "",
                            (en ? "&eClick to manage" : "&eНажмите для управления"))
                    .hideFlags()
                    .build());

            // [13] Щит территории
            boolean shieldActive = plugin.getTerritoryManager().isShieldActive(clan.getClanId());
            inv.setItem(13, new ItemBuilder(Material.SHIELD)
                    .name("&a&l" + (en ? "🛡 Territory Shield" : "🛡 Щит территории"))
                    .lore("",
                            (en ? "&7Status: " : "&7Статус: ") + (shieldActive ? "&a" + (en ? "ON" : "ВКЛ") : "&c" + (en ? "OFF" : "ВЫКЛ")),
                            "",
                            shieldActive
                                    ? (en ? "&7Shield is protecting your territory" : "&7Щит защищает вашу территорию")
                                    : (en ? "&cTerritory is vulnerable!" : "&cТерритория уязвима!"),
                            "",
                            (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                    .hideFlags()
                    .build());

            // [14] Дипломатия
            int alliesCount = plugin.getDiplomacyManager().getAllies(clan.getClanId()).size();
            int enemiesCount = plugin.getDiplomacyManager().getEnemies(clan.getClanId()).size();
            inv.setItem(14, new ItemBuilder(Material.WRITABLE_BOOK)
                    .name("&d&l" + (en ? "🤝 Diplomacy" : "🤝 Дипломатия"))
                    .lore("",
                            (en ? "&7Manage alliances" : "&7Управление союзами"),
                            (en ? "&aAllies: &f" : "&aСоюзников: &f") + alliesCount,
                            (en ? "&cEnemies: &f" : "&cВрагов: &f") + enemiesCount,
                            "",
                            (en ? "&eClick to manage" : "&eНажмите для управления"))
                    .build());

            // [15] Снабжение
            var supply = plugin.getSupplyManager().getSupply(clan.getClanId());
            inv.setItem(15, new ItemBuilder(Material.CHEST)
                    .name("&e&l" + (en ? "📦 Supply" : "📦 Снабжение"))
                    .lore("",
                            (en ? "&7Clan storage & supplies" : "&7Клановый склад и снабжение"),
                            (en ? "&7Food: &f" : "&7Еда: &f") + supply.getFood(),
                            (en ? "&7Materials: &f" : "&7Материалы: &f") + supply.getMaterials(),
                            (en ? "&7Fuel: &f" : "&7Топливо: &f") + supply.getFuel(),
                            "",
                            (en ? "&eClick to manage" : "&eНажмите для управления"))
                    .build());

            // [16] Страна
            var country = plugin.getCountryManager().getCountryByClan(clan.getClanId());
            inv.setItem(16, new ItemBuilder(Material.BLUE_BANNER)
                    .name("&b&l" + (en ? "🏴 Country" : "🏴 Страна"))
                    .lore("",
                            country != null
                                    ? (en ? "&7Country: &f" : "&7Страна: &f") + country.getName()
                                    : (en ? "&7Not in a country" : "&7Не состоите в стране"),
                            "",
                            (en ? "&eClick to manage" : "&eНажмите для управления"))
                    .hideFlags()
                    .build());

            // [22] Рейтинг
            inv.setItem(22, new ItemBuilder(Material.GOLD_INGOT)
                    .name("&6&l" + (en ? "🏆 Ranking" : "🏆 Рейтинг"))
                    .lore("",
                            (en ? "&7Top clans by power" : "&7Топ кланов по мощи"),
                            "",
                            (en ? "&eClick to view" : "&eНажмите для просмотра"))
                    .build());

            // [18] Настройки
            if (clan.isLeader(player.getUniqueId())) {
                inv.setItem(18, new ItemBuilder(Material.COMPARATOR)
                        .name("&7&l" + (en ? "⚙ Settings" : "⚙ Настройки"))
                        .lore("",
                                (en ? "&7Clan settings" : "&7Настройки клана"),
                                "",
                                (en ? "&eClick to open" : "&eНажмите для открытия"))
                        .build());
            }

            // [26] Закрыть
            inv.setItem(26, new ItemBuilder(Material.BARRIER)
                    .name("&c" + (en ? "Close" : "Закрыть"))
                    .build());
        }

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    // ==================== ОБРАБОТКА КЛИКА ====================

    public static void handleClick(Player player, int slot) {
        EverWar plugin = EverWar.getInstance();
        Clan clan = plugin.getClanManager().getClanByPlayer(player);

        if (clan == null) {
            switch (slot) {
                case 4, 11 -> {
                    player.closeInventory();
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &eИспользуйте: &f/war clan create <название> <тег>");
                    MessageUtil.sendSuggest(player,
                            "&a[Нажмите чтобы ввести команду]",
                            "&aСоздать клан",
                            "/war clan create ");
                }
                case 15 -> {
                    player.closeInventory();
                    player.performCommand("war clan list");
                }
                case 22 -> {
                    player.closeInventory();
                    RankingGUI.open(player);
                }
                case 26 -> player.closeInventory();
            }
            return;
        }

        switch (slot) {
            case 4 -> {
                player.closeInventory();
                player.performCommand("war clan info");
            }
            case 10 -> {
                player.closeInventory();
                MembersGUI.open(player, clan, 0);
            }
            case 11 -> {
                player.closeInventory();
                TerritoryMapGUI.open(player);
            }
            case 12 -> {
                player.closeInventory();
                WarGUI.open(player);
            }
            case 13 -> {
                // Переключение щита
                player.closeInventory();
                boolean shieldActive = plugin.getTerritoryManager()
                        .isShieldActive(clan.getClanId());
                if (shieldActive) {
                    player.performCommand("war shield off");
                } else {
                    player.performCommand("war shield on");
                }
            }
            case 14 -> {
                player.closeInventory();
                DiplomacyGUI.open(player);
            }
            case 15 -> {
                player.closeInventory();
                SupplyGUI.open(player);
            }
            case 16 -> {
                player.closeInventory();
                CountryGUI.open(player);
            }
            case 18 -> {
                if (clan.isLeader(player.getUniqueId())) {
                    player.closeInventory();
                    SettingsGUI.open(player);
                }
            }
            case 22 -> {
                player.closeInventory();
                RankingGUI.open(player);
            }
            case 26 -> player.closeInventory();
        }
    }

    private static String getLeaderName(Clan clan) {
        ClanMember leader = clan.getMember(clan.getLeaderUUID());
        return leader != null ? leader.getPlayerName() : "Unknown";
    }
}