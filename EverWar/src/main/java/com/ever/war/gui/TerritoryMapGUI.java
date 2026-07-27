package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class TerritoryMapGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        // Заголовок без цветовых кодов вначале — GUI Listener их не поймёт
        String title = en
                ? "§0§lTerritory Map §7[🗺]"
                : "§0§lКарта территорий §7[🗺]";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Clan playerClan = plugin.getClanManager().getClanByPlayer(player);
        Chunk center = player.getLocation().getChunk();
        String worldName = player.getWorld().getName();

        // Верхняя рамка (0-8)
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }
        // Нижняя рамка (45-53)
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Заголовок в центре
        inv.setItem(4, new ItemBuilder(Material.FILLED_MAP)
                .name("&6&l" + (en ? "🗺 Territory Map" : "🗺 Карта территорий"))
                .lore("",
                        "&7X: &f" + center.getX() + " &7Z: &f" + center.getZ(),
                        (en ? "&7World: &f" : "&7Мир: &f") + worldName,
                        "",
                        "&aЗелёный &7— " + (en ? "Your clan" : "Ваш клан"),
                        "&bГолубой &7— " + (en ? "Ally" : "Союзник"),
                        "&cКрасный &7— " + (en ? "Enemy" : "Враг"),
                        "&fБелый &7— " + (en ? "Other" : "Другой клан"),
                        "&7Серый &7— " + (en ? "Wilderness" : "Дикая местность"),
                        "&eЖёлтый &7— " + (en ? "Your position" : "Вы здесь"))
                .glow()
                .build());

        // Информация о щите/дезертире (слоты 1, 7)
        if (playerClan != null) {
            boolean shieldActive = plugin.getTerritoryManager()
                    .isShieldActive(playerClan.getClanId());
            inv.setItem(1, new ItemBuilder(shieldActive
                    ? Material.SHIELD : Material.WOODEN_SWORD)
                    .name("&a🛡 " + (en ? "Your Shield" : "Ваш щит"))
                    .lore("",
                            plugin.getTerritoryManager().getShieldStatus(
                                    playerClan.getClanId(), lang))
                    .hideFlags()
                    .build());

            if (playerClan.isDeserter()) {
                inv.setItem(7, new ItemBuilder(Material.RED_BANNER)
                        .name("&4&l🚨 " + (en ? "DESERTER MODE" : "РЕЖИМ ДЕЗЕРТИРА"))
                        .lore("", "&cВаш клан против всех!")
                        .hideFlags()
                        .build());
            }
        }

        // ==================== КАРТА 9x4 (слоты 9-44) ====================
        int mapWidth = 9;
        int mapHeight = 4;
        int startX = center.getX() - mapWidth / 2;
        int startZ = center.getZ() - mapHeight / 2;

        for (int dz = 0; dz < mapHeight; dz++) {
            for (int dx = 0; dx < mapWidth; dx++) {
                int slot = 9 + dz * 9 + dx;
                int cx = startX + dx;
                int cz = startZ + dz;

                String key = Territory.makeKey(worldName, cx, cz);
                Territory territory = plugin.getTerritoryManager().getTerritoryByKey(key);

                boolean isPlayer = (cx == center.getX() && cz == center.getZ());

                if (isPlayer) {
                    // Позиция игрока
                    inv.setItem(slot, new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                            .name("&e&l✦ " + (en ? "YOU ARE HERE" : "ВЫ ЗДЕСЬ"))
                            .lore("",
                                    "&7X: &f" + cx + " &7Z: &f" + cz,
                                    territory != null
                                            ? "&7Owner: &f" + getOwnerName(territory)
                                            : "&8" + (en ? "Wilderness" : "Дикая местность"))
                            .glow()
                            .build());

                } else if (territory == null) {
                    // Ничейная территория
                    inv.setItem(slot, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                            .name("&8&l" + (en ? "Wilderness" : "Дикая местность"))
                            .lore("",
                                    "&7X: &f" + cx + " &7Z: &f" + cz,
                                    "",
                                    (en ? "&eClick to teleport info" : "&eЧанк не принадлежит никому"),
                                    (en ? "&7Command: /war territory claim" : "&7Команда: /war territory claim"))
                            .build());

                } else {
                    // Занятый чанк
                    UUID ownerId = territory.getOwnerClanId();
                    Clan owner = plugin.getClanManager().getClanById(ownerId);
                    String ownerName = owner != null ? owner.getName() : "Unknown";
                    String ownerTag = owner != null ? owner.getTag() : "???";
                    boolean shieldOn = plugin.getTerritoryManager().isShieldActive(ownerId);
                    boolean ownerDeserter = owner != null && owner.isDeserter();

                    Material glass;
                    String prefix;
                    String status;

                    if (playerClan != null && ownerId.equals(playerClan.getClanId())) {
                        glass = Material.GREEN_STAINED_GLASS_PANE;
                        prefix = "&a&l";
                        status = "&a" + (en ? "Your territory" : "Ваша территория");
                    } else if (playerClan != null && plugin.getDiplomacyManager()
                            .isAlly(playerClan.getClanId(), ownerId)) {
                        glass = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                        prefix = "&b";
                        status = "&b" + (en ? "Ally" : "Союзник");
                    } else if (playerClan != null && plugin.getDiplomacyManager()
                            .isEnemy(playerClan.getClanId(), ownerId)) {
                        glass = Material.RED_STAINED_GLASS_PANE;
                        prefix = "&c";
                        status = "&c" + (en ? "Enemy" : "Враг");
                    } else if (ownerDeserter) {
                        glass = Material.PURPLE_STAINED_GLASS_PANE;
                        prefix = "&5";
                        status = "&4" + (en ? "DESERTER" : "ДЕЗЕРТИР");
                    } else {
                        glass = Material.WHITE_STAINED_GLASS_PANE;
                        prefix = "&f";
                        status = "&7" + (en ? "Other" : "Другой");
                    }

                    inv.setItem(slot, new ItemBuilder(glass)
                            .name(prefix + ownerName + " &7[" + ownerTag + "]")
                            .lore("",
                                    "&7X: &f" + cx + " &7Z: &f" + cz,
                                    status,
                                    (territory.isCore() ? "&6⭐ " + (en ? "BASE CORE" : "ЯДРО БАЗЫ") : ""),
                                    "&7HP: " + territory.getHpDisplay(),
                                    (en ? "&7Defense: Lv." : "&7Защита: Ур.") + territory.getDefenseLevel(),
                                    "",
                                    "&7🛡 " + (shieldOn ? "&aShield ON" : "&cShield OFF"),
                                    ownerDeserter ? "&4🚨 DESERTER MODE!" : "")
                            .build());
                }
            }
        }

        // ==================== НИЖНЯЯ ПАНЕЛЬ ====================

        // Обновить (45)
        inv.setItem(45, new ItemBuilder(Material.COMPASS)
                .name("&e🔄 " + (en ? "Refresh Map" : "Обновить карту"))
                .lore("", (en ? "&7Click to refresh" : "&7Нажмите для обновления"))
                .build());

        // Инфо о центре (49)
        Territory centerTerritory = plugin.getTerritoryManager().getTerritoryByChunk(center);
        if (centerTerritory != null) {
            Clan centerOwner = plugin.getClanManager()
                    .getClanById(centerTerritory.getOwnerClanId());
            inv.setItem(49, new ItemBuilder(Material.NAME_TAG)
                    .name("&e📍 " + (en ? "Current Chunk" : "Текущий чанк"))
                    .lore("",
                            (en ? "&7Owner: &f" : "&7Владелец: &f")
                                    + (centerOwner != null ? centerOwner.getName() : "Unknown"),
                            "&7HP: " + centerTerritory.getHpDisplay(),
                            centerTerritory.isCore() ? "&6⭐ Ядро базы" : "")
                    .build());
        } else {
            inv.setItem(49, new ItemBuilder(Material.PAPER)
                    .name("&e📍 " + (en ? "Current Chunk" : "Текущий чанк"))
                    .lore("",
                            "&8" + (en ? "Wilderness — free to claim!" : "Дикая — можно захватить!"),
                            "",
                            (en ? "&eCommand: /war territory claim" : "&eКоманда: /war territory claim"))
                    .build());
        }

        // Карта в чате (50)
        inv.setItem(50, new ItemBuilder(Material.MAP)
                .name("&e📜 " + (en ? "Chat Map" : "Карта в чате"))
                .lore("", (en ? "&7Show text map in chat" : "&7Показать текстовую карту"))
                .build());

        // Назад (53)
        inv.setItem(53, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back to menu" : "Назад в меню"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    private static String getOwnerName(Territory territory) {
        EverWar plugin = EverWar.getInstance();
        Clan c = plugin.getClanManager().getClanById(territory.getOwnerClanId());
        return c != null ? c.getName() : "Unknown";
    }

    public static void handleClick(Player player, int slot) {
        switch (slot) {
            case 45 -> { // Обновить
                player.closeInventory();
                open(player);
            }
            case 49 -> { // Инфо о центре
                player.closeInventory();
                player.performCommand("war territory info");
            }
            case 50 -> { // Карта в чат
                player.closeInventory();
                player.performCommand("war map");
            }
            case 53 -> { // Назад
                player.closeInventory();
                ClanMenuGUI.open(player);
            }
            default -> {
                // Клик по чанку на карте (9-44)
                if (slot >= 9 && slot <= 44) {
                    var item = player.getOpenInventory().getItem(slot);
                    if (item != null && item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                        player.closeInventory();
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &7Встаньте на нужный чанк и напишите &f/war territory claim");
                    }
                }
            }
        }
    }
}