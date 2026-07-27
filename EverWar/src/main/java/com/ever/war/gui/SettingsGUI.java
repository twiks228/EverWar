package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SettingsGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null || !clan.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }

        String title = en ? "§0§l⚙ Clan Settings" : "§0§l⚙ Настройки клана";
        Inventory inv = Bukkit.createInventory(null, 36, title);

        // Фон
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Заголовок
        inv.setItem(4, new ItemBuilder(Material.COMPARATOR)
                .name("&7&l" + (en ? "Clan Settings" : "Настройки клана"))
                .lore("",
                        "&7" + clan.getName() + " &7[" + clan.getTag() + "]",
                        (en ? "&7Leader: &f" : "&7Лидер: &f") + player.getName())
                .build());

        // ==================== АТАКА СВОИХ ====================
        inv.setItem(10, new ItemBuilder(clan.isFriendlyFire()
                ? Material.RED_WOOL : Material.GREEN_WOOL)
                .name("&e⚔ " + (en ? "Friendly Fire" : "Атака своих"))
                .lore("",
                        (en ? "&7Attack own clan members" : "&7Разрешить атаковать своих"),
                        "",
                        (en ? "&7Status: " : "&7Статус: ") +
                                (clan.isFriendlyFire() ? "&aВКЛ" : "&cВЫКЛ"),
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                .build());

        // ==================== АТАКА СОЮЗНИКОВ ====================
        inv.setItem(12, new ItemBuilder(clan.isAllowAttackAllies()
                ? Material.RED_WOOL : Material.LIGHT_BLUE_WOOL)
                .name("&b🤝 " + (en ? "Attack Allies" : "Атака союзников"))
                .lore("",
                        (en ? "&7Allow attacking allied clans" : "&7Можно атаковать союзников"),
                        "",
                        (en ? "&7Status: " : "&7Статус: ") +
                                (clan.isAllowAttackAllies() ? "&cВКЛ" : "&aВЫКЛ"),
                        "",
                        (en ? "&c⚠ WARNING: BETRAYAL!" : "&c⚠ ВНИМАНИЕ: ПРЕДАТЕЛЬСТВО!"),
                        (en ? "&7You can hit allies!" : "&7Вы сможете бить союзников!"),
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                .build());

        // ==================== ОТКРЫТЫЙ КЛАН ====================
        inv.setItem(14, new ItemBuilder(clan.isOpen()
                ? Material.OAK_DOOR : Material.IRON_DOOR)
                .name("&e🚪 " + (en ? "Open Clan" : "Открытый клан"))
                .lore("",
                        (en ? "&7Anyone can join without invite" : "&7Любой может вступить без приглашения"),
                        "",
                        (en ? "&7Status: " : "&7Статус: ") +
                                (clan.isOpen()
                                        ? "&a" + (en ? "Open" : "Открыт")
                                        : "&c" + (en ? "Closed" : "Закрыт")),
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                .build());

        // ==================== ПУБЛИЧНАЯ ИНФО ====================
        inv.setItem(16, new ItemBuilder(Material.BOOK)
                .name("&e📖 " + (en ? "Public Info" : "Публичная информация"))
                .lore("",
                        (en ? "&7Others can see clan info" : "&7Другие видят информацию клана"),
                        "",
                        (en ? "&7Status: " : "&7Статус: ") +
                                (clan.isPublicInfo() ? "&aВКЛ" : "&cВЫКЛ"),
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                .build());

        // ==================== ЩИТ ТЕРРИТОРИИ ====================
        boolean shieldActive = plugin.getTerritoryManager()
                .isShieldActive(clan.getClanId());

        String shieldLore1;
        String shieldLore2;
        if (shieldActive) {
            long rem = plugin.getTerritoryManager()
                    .getShieldRemainingSeconds(clan.getClanId());
            if (rem == -1) {
                shieldLore1 = "&aПОСТОЯННЫЙ";
                shieldLore2 = "&7Выдан администратором";
            } else {
                shieldLore1 = "&aВКЛ";
                shieldLore2 = "&7Осталось: " + TimeUtil.formatTime(rem, lang);
            }
        } else {
            shieldLore1 = "&cВЫКЛ";
            shieldLore2 = "&7Территория уязвима!";
        }

        inv.setItem(19, new ItemBuilder(shieldActive
                ? Material.SHIELD : Material.WOODEN_SWORD)
                .name("&a🛡 " + (en ? "Territory Shield" : "Щит территории"))
                .lore("",
                        (en ? "&7Status: " : "&7Статус: ") + shieldLore1,
                        shieldLore2,
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"),
                        (en ? "&7Max 15 min without admin" : "&7Макс 15 мин без админа"))
                .hideFlags()
                .build());

        // ==================== РЕЖИМ ДЕЗЕРТИРА ====================
        boolean isDeserter = clan.isDeserter();

        String deserterLore1;
        if (isDeserter) {
            long rem = clan.getDeserterRemainingSeconds();
            deserterLore1 = "&4АКТИВЕН &7(" + TimeUtil.formatTime(rem, lang) + ")";
        } else {
            deserterLore1 = "&aВЫКЛ";
        }

        inv.setItem(22, new ItemBuilder(isDeserter
                ? Material.RED_BANNER : Material.WHITE_BANNER)
                .name("&4&l🚨 " + (en ? "Deserter Mode" : "Режим дезертира"))
                .lore("",
                        (en ? "&7Clan vs everyone" : "&7Клан против ВСЕХ"),
                        "",
                        (en ? "&7Status: " : "&7Статус: ") + deserterLore1,
                        "",
                        (en ? "&c⚠ Attack anyone!" : "&c⚠ Атакуйте всех!"),
                        (en ? "&c⚠ Everyone can attack you!" : "&c⚠ Все могут вас атаковать!"),
                        (en ? "&e+10 power for killing deserter"
                                : "&e+10 мощи за убийство дезертира"),
                        "",
                        (en ? "&eClick to toggle" : "&eНажмите для переключения"))
                .hideFlags()
                .build());

        // ==================== ЯЗЫК ====================
        inv.setItem(25, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&e🌐 " + (en ? "Language" : "Язык"))
                .lore("",
                        (en ? "&7Server language: " : "&7Язык сервера: ")
                                + "&f" + lang.toUpperCase(),
                        "",
                        (en ? "&7Change in config.yml" : "&7Измените в config.yml"))
                .build());

        // ==================== УДАЛЕНИЕ ====================
        inv.setItem(31, new ItemBuilder(Material.TNT)
                .name("&c&l" + (en ? "❌ DELETE CLAN" : "❌ УДАЛИТЬ КЛАН"))
                .lore("",
                        (en ? "&c⚠ CANNOT BE UNDONE!" : "&c⚠ НЕЛЬЗЯ ОТМЕНИТЬ!"),
                        (en ? "&7All territories will be lost" : "&7Все территории будут потеряны"),
                        (en ? "&7All members will be kicked" : "&7Все участники будут выгнаны"),
                        "",
                        (en ? "&cClick to delete" : "&cНажмите для удаления"))
                .build());

        // ==================== НАЗАД ====================
        inv.setItem(35, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back to menu" : "Назад в меню"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot) {
        EverWar plugin = EverWar.getInstance();
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null || !clan.isLeader(player.getUniqueId())) return;

        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        switch (slot) {
            case 10 -> { // Friendly Fire
                clan.setFriendlyFire(!clan.isFriendlyFire());
                plugin.getStorageManager().saveClan(clan);
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &e⚔ " + (en ? "Friendly Fire: " : "Атака своих: ")
                                + (clan.isFriendlyFire() ? "&aВКЛ" : "&cВЫКЛ"));
                open(player);
            }

            case 12 -> { // Attack Allies
                boolean newState = !clan.isAllowAttackAllies();
                clan.setAllowAttackAllies(newState);
                plugin.getStorageManager().saveClan(clan);
                if (newState) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &c⚠ Атака союзников: &cВКЛ &7(ПРЕДАТЕЛЬСТВО!)");
                    MessageUtil.send(player,
                            "&7Ваши союзники будут удивлены...");
                    // Оповещаем клан
                    for (var m : clan.getMemberList()) {
                        if (m.isOnline()) {
                            Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                            if (p != null && !p.equals(player)) {
                                MessageUtil.send(p,
                                        "&8[&6EverWar&8] &e⚠ Лидер разрешил атаку союзников!");
                            }
                        }
                    }
                } else {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &a✓ Атака союзников: &aВЫКЛ &7(нормально)");
                }
                open(player);
            }

            case 14 -> { // Open Clan
                clan.setOpen(!clan.isOpen());
                plugin.getStorageManager().saveClan(clan);
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &e🚪 Открытый клан: "
                                + (clan.isOpen() ? "&aВКЛ" : "&cВЫКЛ"));
                open(player);
            }

            case 16 -> { // Public Info
                clan.setPublicInfo(!clan.isPublicInfo());
                plugin.getStorageManager().saveClan(clan);
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &e📖 Публичная инфо: "
                                + (clan.isPublicInfo() ? "&aВКЛ" : "&cВЫКЛ"));
                open(player);
            }

            case 19 -> { // Shield
                player.closeInventory();
                boolean shieldActive = plugin.getTerritoryManager()
                        .isShieldActive(clan.getClanId());
                if (shieldActive) {
                    player.performCommand("war shield off");
                } else {
                    player.performCommand("war shield on");
                }
            }

            case 22 -> { // Deserter
                player.closeInventory();
                if (clan.isDeserter()) {
                    player.performCommand("war deserter off");
                } else {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &c⚠ &lРЕЖИМ ДЕЗЕРТИРА");
                    MessageUtil.send(player,
                            "&7Ваш клан станет &lпротив всех&7 включая союзников!");
                    MessageUtil.send(player, "");
                    MessageUtil.sendClickable(player,
                            "&4[Включить на 1 час]",
                            "&cАтакуй всех!",
                            "/war deserter on 1");
                    MessageUtil.sendClickable(player,
                            "&4[Включить на 3 часа]",
                            "&cАтакуй всех!",
                            "/war deserter on 3");
                    MessageUtil.sendClickable(player,
                            "&4[Включить на 24 часа]",
                            "&cАтакуй всех!",
                            "/war deserter on 24");
                }
            }

            case 31 -> { // Delete
                player.closeInventory();
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.send(player,
                        "&c&l⚠ УДАЛЕНИЕ КЛАНА &f" + clan.getName());
                MessageUtil.send(player,
                        "&7Все данные будут &lпотеряны навсегда&7:");
                MessageUtil.send(player, "  &7• Все территории");
                MessageUtil.send(player, "  &7• Все участники");
                MessageUtil.send(player, "  &7• Вся статистика");
                MessageUtil.send(player, "  &7• Склад ресурсов");
                MessageUtil.send(player, "");
                MessageUtil.sendClickable(player,
                        "&c&l[✗ ПОДТВЕРДИТЬ УДАЛЕНИЕ]",
                        "&cЭто нельзя отменить!",
                        "/war clan delete");
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━");
            }

            case 35 -> { // Back
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
        }
    }
}