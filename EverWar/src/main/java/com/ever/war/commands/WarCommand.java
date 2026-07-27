package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.gui.ClanMenuGUI;
import com.ever.war.managers.TerritoryManager;
import com.ever.war.models.Clan;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WarCommand implements CommandExecutor, TabCompleter {

    private final EverWar plugin;
    private final ClanCommand clanCommand;
    private final TerritoryCommand territoryCommand;
    private final DiplomacyCommand diplomacyCommand;
    private final WarfareCommand warfareCommand;
    private final SiegeCommand siegeCommand;
    private final SupplyCommand supplyCommand;

    public WarCommand(EverWar plugin) {
        this.plugin = plugin;
        this.clanCommand      = new ClanCommand(plugin);
        this.territoryCommand = new TerritoryCommand(plugin);
        this.diplomacyCommand = new DiplomacyCommand(plugin);
        this.warfareCommand   = new WarfareCommand(plugin);
        this.siegeCommand     = new SiegeCommand(plugin);
        this.supplyCommand    = new SupplyCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда только для игроков.");
            return true;
        }

        if (args.length == 0) {
            ClanMenuGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] subArgs = args.length > 1
                ? Arrays.copyOfRange(args, 1, args.length)
                : new String[0];

        switch (sub) {
            case "clan",      "клан"       -> clanCommand.execute(player, subArgs);
            case "territory", "территория",
                 "terr"                    -> territoryCommand.execute(player, subArgs);
            case "diplomacy", "дипломатия",
                 "diplo"                   -> diplomacyCommand.execute(player, subArgs);
            case "war",       "война"      -> warfareCommand.execute(player, subArgs);
            case "siege",     "осада"      -> siegeCommand.execute(player, subArgs);
            case "supply",    "снабжение",
                 "склад"                   -> supplyCommand.execute(player, subArgs);
            case "shield",    "щит"        -> handleShield(player, subArgs);
            case "deserter",  "дезертир",
                 "renegade"                -> handleDeserter(player, subArgs);
            case "top",       "топ"        -> handleTop(player, subArgs);
            case "map",       "карта"      -> handleMap(player);
            case "admin"                   -> handleAdmin(player, subArgs);
            case "help",      "помощь",
                 "?"                       -> handleHelp(player);
            case "country",   "страна"     -> handleCountry(player, subArgs);
            default -> {
                MessageUtil.sendMessage(player, "invalid-usage",
                        "{usage}", "/war help");
                MessageUtil.soundError(player);
            }
        }

        return true;
    }

    // ==================== ЩИТ ====================

    private void handleShield(Player player, String[] args) {
        if (args.length == 0) {
            sendShieldHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        String lang = plugin.getConfigManager().getLanguage();

        switch (sub) {
            case "on", "вкл" -> {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "not-in-clan");
                    return;
                }

                var member = clan.getMember(player.getUniqueId());
                if (member == null || !member.getRole().canClaimTerritory()) {
                    MessageUtil.sendMessage(player, "no-permission");
                    MessageUtil.soundError(player);
                    return;
                }

                if (plugin.getTerritoryManager()
                        .getClanTerritoryCount(clan.getClanId()) == 0) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &cУ вашего клана нет территорий!");
                    return;
                }

                int duration = 15 * 60;
                if (args.length > 1) {
                    try {
                        int minutes = Integer.parseInt(args[1]);
                        if (minutes < 1 || minutes > 15) {
                            MessageUtil.send(player,
                                    "&8[&6EverWar&8] &cВремя от &f1 &cдо &f15 &cминут.");
                            return;
                        }
                        duration = minutes * 60;
                    } catch (NumberFormatException e) {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cУкажите число минут (1-15).");
                        return;
                    }
                }

                if (plugin.getTerritoryManager().isShieldActive(clan.getClanId())) {
                    long rem = plugin.getTerritoryManager()
                            .getShieldRemainingSeconds(clan.getClanId());
                    String remStr = rem == -1
                            ? (lang.equals("en") ? "permanent" : "постоянный")
                            : TimeUtil.formatTime(rem, lang);
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &e🛡 Щит уже активен! Осталось: &f" + remStr);
                    return;
                }

                var result = plugin.getTerritoryManager()
                        .enableShield(clan.getClanId(), duration, false);

                switch (result) {
                    case SUCCESS -> MessageUtil.soundSuccess(player);
                    case DURATION_TOO_LONG -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cМаксимум &f15 минут &cбез разрешения админа.");
                        MessageUtil.soundError(player);
                    }
                    case INVALID_DURATION -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cНекорректное время.");
                        MessageUtil.soundError(player);
                    }
                    case NO_PERMISSION -> {
                        MessageUtil.sendMessage(player, "no-permission");
                        MessageUtil.soundError(player);
                    }
                }
            }

            case "off", "выкл" -> {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "not-in-clan");
                    return;
                }

                var member = clan.getMember(player.getUniqueId());
                if (member == null || !member.getRole().canClaimTerritory()) {
                    MessageUtil.sendMessage(player, "no-permission");
                    return;
                }

                if (!plugin.getTerritoryManager().isShieldActive(clan.getClanId())) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &7Щит уже выключен.");
                    return;
                }

                plugin.getTerritoryManager().disableShield(clan.getClanId());
                MessageUtil.soundSuccess(player);
            }

            case "permanent", "постоянный" -> {
                if (!player.hasPermission("everwar.admin.shield")) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &cТолько администратор может включить постоянный щит.");
                    MessageUtil.soundError(player);
                    return;
                }

                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "not-in-clan");
                    return;
                }

                plugin.getTerritoryManager().enablePermanentShield(clan.getClanId());
                MessageUtil.soundSuccess(player);
            }

            case "status", "статус" -> {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "not-in-clan");
                    return;
                }

                String status = plugin.getTerritoryManager()
                        .getShieldStatus(clan.getClanId(), lang);

                MessageUtil.send(player, "&8&l━━━━━ &a🛡 Статус щита &8&l━━━━━");
                MessageUtil.send(player, status);

                if (plugin.getTerritoryManager().isShieldActive(clan.getClanId())) {
                    long remaining = plugin.getTerritoryManager()
                            .getShieldRemainingSeconds(clan.getClanId());
                    if (remaining == -1) {
                        MessageUtil.send(player, "&7Тип: &aПостоянный &7(админ)");
                    } else {
                        MessageUtil.send(player, "&7Осталось: &f"
                                + TimeUtil.formatTime(remaining, lang));
                    }
                    MessageUtil.send(player, "&c⚠ Во время войны щит НЕ спасает!");
                } else {
                    MessageUtil.send(player, "&7Включите: &f/war shield on [минуты]");
                }
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━");
            }

            default -> sendShieldHelp(player);
        }
    }

    private void sendShieldHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "      &a🛡 &6&lЩит территории EverWar");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "&7По умолчанию: &cЩИТ ВЫКЛЮЧЕН");
        MessageUtil.send(player, "&7Любой может ломать/взрывать вашу базу");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&e/war shield on &7— Включить на 15 минут");
        MessageUtil.send(player, "&e/war shield on <1-15> &7— На N минут");
        MessageUtil.send(player, "&e/war shield off &7— Выключить");
        MessageUtil.send(player, "&e/war shield status &7— Статус");
        MessageUtil.send(player, "&7/war shield permanent &7— &cПостоянный (админ)");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&c⚠ Во время войны щит НЕ защищает!");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== ДЕЗЕРТИР ====================

    private void handleDeserter(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cТолько лидер может объявить клан дезертиром!");
            MessageUtil.soundError(player);
            return;
        }

        if (args.length == 0) {
            sendDeserterHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on", "вкл", "start" -> {
                if (clan.isDeserter()) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан уже дезертир!");
                    return;
                }

                int hours = 1;
                if (args.length > 1) {
                    try {
                        hours = Integer.parseInt(args[1]);
                        if (hours < 1 || hours > 24) {
                            MessageUtil.send(player,
                                    "&8[&6EverWar&8] &cОт 1 до 24 часов.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        MessageUtil.send(player, "&cУкажите число часов.");
                        return;
                    }
                }

                long until = Instant.now().getEpochSecond() + (hours * 3600L);
                clan.setDeserter(true, until);
                plugin.getStorageManager().saveClan(clan);

                MessageUtil.broadcast(
                        "&8[&6EverWar&8] &4&l⚠ КЛАН-ДЕЗЕРТИР! ⚠");
                MessageUtil.broadcast(
                        "&4Клан &f" + clan.getName() + " &7[" + clan.getTag() + "] "
                                + "&4объявил войну &lВСЕМ&4 на &f" + hours + " &4часов!");
                MessageUtil.broadcast(
                        "&7Атакуйте их без предупреждения! Награда: &e+10 мощи &7за убийство");

                for (var m : clan.getMemberList()) {
                    if (m.isOnline()) {
                        Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                        if (p != null) {
                            MessageUtil.sendTitle(p,
                                    "&4⚠ ДЕЗЕРТИРЫ",
                                    "&7Вы против всех на " + hours + "ч!",
                                    20, 60, 20);
                            MessageUtil.soundWar(p);
                        }
                    }
                }
            }

            case "off", "выкл", "stop" -> {
                if (!clan.isDeserter()) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан не в режиме дезертира.");
                    return;
                }

                clan.setDeserter(false, 0);
                plugin.getStorageManager().saveClan(clan);

                MessageUtil.broadcast(
                        "&8[&6EverWar&8] &7Клан &f" + clan.getName()
                                + " &7вышел из режима дезертира.");

                for (var m : clan.getMemberList()) {
                    if (m.isOnline()) {
                        Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                        if (p != null) {
                            MessageUtil.send(p,
                                    "&8[&6EverWar&8] &aВаш клан больше не дезертир.");
                        }
                    }
                }
            }

            case "status", "статус" -> {
                if (clan.isDeserter()) {
                    long remaining = clan.getDeserterRemainingSeconds();
                    String lang = plugin.getConfigManager().getLanguage();
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &4🚨 Режим дезертира &aАКТИВЕН");
                    MessageUtil.send(player,
                            "&7Осталось: &f" + TimeUtil.formatTime(remaining, lang));
                    MessageUtil.send(player,
                            "&7Вы можете атаковать &lВСЕХ &7включая союзников!");
                } else {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &7Режим дезертира &cВЫКЛ");
                }
            }

            default -> sendDeserterHelp(player);
        }
    }

    private void sendDeserterHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "     &4&l🚨 РЕЖИМ ДЕЗЕРТИРА");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&7Ваш клан становится &4&lпротив всех&7:");
        MessageUtil.send(player, "  &7• Можете атаковать &lлюбого &7игрока");
        MessageUtil.send(player, "  &7• Включая &aсоюзников");
        MessageUtil.send(player, "  &7• Все могут атаковать &lвас");
        MessageUtil.send(player, "  &7• За убийство дезертира: &e+10 мощи");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&e/war deserter on [часы] &7— Включить (макс 24ч)");
        MessageUtil.send(player, "&e/war deserter off &7— Выключить");
        MessageUtil.send(player, "&e/war deserter status &7— Статус");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&c⚠ Только лидер!");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== ТОП ====================

    private void handleTop(Player player, String[] args) {
        int limit = 10;
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
                limit = Math.min(50, Math.max(1, limit));
            } catch (NumberFormatException ignored) {}
        }

        MessageUtil.send(player, plugin.getLanguageManager().get("top-header"));

        var topClans = plugin.getClanManager().getTopClans(limit);
        if (topClans.isEmpty()) {
            MessageUtil.send(player, "&7Кланов пока нет.");
        }

        for (int i = 0; i < topClans.size(); i++) {
            var clan = topClans.get(i);
            String entry = plugin.getLanguageManager().get("top-entry",
                    "{place}",   String.valueOf(i + 1),
                    "{clan}",    clan.getName(),
                    "{power}",   String.format("%.0f", clan.getTotalPower()),
                    "{members}", String.valueOf(clan.getMemberCount()));
            MessageUtil.send(player, entry);
        }

        MessageUtil.send(player, plugin.getLanguageManager().get("top-footer"));
    }

    private void handleMap(Player player) {
        var lines = plugin.getTerritoryManager().generateMap(player);
        for (String line : lines) {
            MessageUtil.send(player, line);
        }
    }

    // ==================== СТРАНА ====================

    private void handleCountry(Player player, String[] args) {
        if (args.length == 0) {
            sendCountryHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        String[] subArgs = args.length > 1
                ? Arrays.copyOfRange(args, 1, args.length)
                : new String[0];

        switch (sub) {
            case "create", "создать" -> {
                if (subArgs.length < 2) {
                    MessageUtil.sendMessage(player, "invalid-usage",
                            "{usage}", "/war country create <name> <tag>");
                    return;
                }
                String name = subArgs[0];
                String tag  = subArgs[1];
                var result = plugin.getCountryManager().createCountry(player, name, tag);
                switch (result) {
                    case SUCCESS -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &aСтрана &f" + name
                                        + " &a[" + tag + "] &aсоздана!");
                        MessageUtil.soundSuccess(player);
                    }
                    case NOT_IN_CLAN         -> MessageUtil.sendMessage(player, "not-in-clan");
                    case NO_PERMISSION       -> MessageUtil.sendMessage(player, "no-permission");
                    case ALREADY_IN_COUNTRY  -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан уже в стране.");
                    case INVALID_NAME        -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cНазвание: 3-32 символа.");
                    case INVALID_TAG         -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cТег: 2-5 символов.");
                    case NAME_TAKEN          -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cНазвание уже занято.");
                    case TAG_TAKEN           -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cТег уже используется.");
                }
            }

            case "delete", "удалить" -> {
                var result = plugin.getCountryManager().deleteCountry(player);
                switch (result) {
                    case SUCCESS -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cСтрана расформирована.");
                        MessageUtil.soundSuccess(player);
                    }
                    case NOT_IN_CLAN       -> MessageUtil.sendMessage(player, "not-in-clan");
                    case NOT_IN_COUNTRY    -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан не в стране.");
                    case NO_PERMISSION     -> MessageUtil.sendMessage(player, "no-permission");
                    case COUNTRY_NOT_FOUND -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cСтрана не найдена.");
                }
            }

            case "invite", "пригласить" -> {
                if (subArgs.length < 1) {
                    MessageUtil.sendMessage(player, "invalid-usage",
                            "{usage}", "/war country invite <clan>");
                    return;
                }
                var result = plugin.getCountryManager().inviteClan(player, subArgs[0]);
                switch (result) {
                    case SUCCESS -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &eПриглашение отправлено клану &f" + subArgs[0]);
                        MessageUtil.soundSuccess(player);
                    }
                    case NOT_IN_CLAN         -> MessageUtil.sendMessage(player, "not-in-clan");
                    case NOT_IN_COUNTRY      -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан не в стране.");
                    case NO_PERMISSION       -> MessageUtil.sendMessage(player, "no-permission");
                    case TARGET_NOT_FOUND    -> MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", subArgs[0]);
                    case ALREADY_IN_COUNTRY  -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cЭтот клан уже в стране.");
                }
            }

            case "join", "вступить" -> {
                if (subArgs.length < 1) {
                    MessageUtil.sendMessage(player, "invalid-usage",
                            "{usage}", "/war country join <country>");
                    return;
                }
                var result = plugin.getCountryManager().joinCountry(player, subArgs[0]);
                switch (result) {
                    case SUCCESS -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &aВаш клан вступил в страну &f" + subArgs[0]);
                        MessageUtil.soundSuccess(player);
                    }
                    case NOT_IN_CLAN         -> MessageUtil.sendMessage(player, "not-in-clan");
                    case NO_PERMISSION       -> MessageUtil.sendMessage(player, "no-permission");
                    case ALREADY_IN_COUNTRY  -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан уже в стране.");
                    case COUNTRY_NOT_FOUND   -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cСтрана не найдена.");
                    case NO_INVITE           -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cНет приглашения в эту страну.");
                }
            }

            case "leave", "выйти" -> {
                var result = plugin.getCountryManager().leaveClan(player);
                switch (result) {
                    case SUCCESS -> {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cВаш клан покинул страну.");
                        MessageUtil.soundSuccess(player);
                    }
                    case NOT_IN_CLAN    -> MessageUtil.sendMessage(player, "not-in-clan");
                    case NOT_IN_COUNTRY -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cВаш клан не в стране.");
                    case IS_LEADER      -> MessageUtil.send(player,
                            "&8[&6EverWar&8] &cЛидер-клан не может покинуть страну.");
                }
            }

            case "info", "инфо" -> {
                String countryName = subArgs.length > 0 ? subArgs[0] : null;
                var country = countryName != null
                        ? plugin.getCountryManager().getCountryByName(countryName)
                        : null;

                if (country == null && countryName != null) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &cСтрана не найдена.");
                    return;
                }

                if (country == null) {
                    var clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                    if (clan == null) {
                        MessageUtil.sendMessage(player, "not-in-clan");
                        return;
                    }
                    country = plugin.getCountryManager().getCountryByClan(clan.getClanId());
                    if (country == null) {
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &cВаш клан не в стране.");
                        return;
                    }
                }

                String info = country.getInfoDisplay(plugin.getConfigManager().getLanguage());
                for (String line : info.split("\n")) {
                    MessageUtil.send(player, line);
                }
            }

            case "list", "список" -> {
                var countries = plugin.getCountryManager().getTopCountries(20);
                MessageUtil.send(player, "&8&l━━━━━ &b🏴 Страны &8&l━━━━━");
                if (countries.isEmpty()) {
                    MessageUtil.send(player, "&7Стран пока нет.");
                }
                for (int i = 0; i < countries.size(); i++) {
                    var c = countries.get(i);
                    MessageUtil.sendClickable(player,
                            "&e" + (i + 1) + ". &f" + c.getFormattedName()
                                    + " &7[" + c.getTag() + "] &7— &f"
                                    + c.getClanCount() + " &7кланов",
                            "&aИнформация",
                            "/war country info " + c.getName());
                }
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
            }

            default -> sendCountryHelp(player);
        }
    }

    private void sendCountryHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &b🏴 Страна &8&l━━━━━");
        MessageUtil.send(player, "&e/war country create <name> <tag> &7— Создать");
        MessageUtil.send(player, "&e/war country delete &7— Удалить");
        MessageUtil.send(player, "&e/war country invite <clan> &7— Пригласить");
        MessageUtil.send(player, "&e/war country join <country> &7— Вступить");
        MessageUtil.send(player, "&e/war country leave &7— Покинуть");
        MessageUtil.send(player, "&e/war country info [country] &7— Инфо");
        MessageUtil.send(player, "&e/war country list &7— Список");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== АДМИН ====================

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("everwar.admin")) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }

        if (args.length == 0) {
            sendAdminHelp(player);
            return;
        }

        String lang = plugin.getConfigManager().getLanguage();

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                MessageUtil.send(player, "&8[&6EverWar&8] &a✓ Плагин перезагружен.");
                MessageUtil.soundSuccess(player);
            }

            case "setpower" -> {
                if (args.length < 3) {
                    MessageUtil.send(player,
                            "&c/war admin setpower <player> <amount>");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    MessageUtil.sendMessage(player, "player-not-found",
                            "{player}", args[1]);
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    double current = plugin.getPowerManager().getPlayerPower(target.getUniqueId());
                    double diff = amount - current;
                    if (diff >= 0) {
                        plugin.getPowerManager().addPower(target.getUniqueId(), diff);
                    } else {
                        plugin.getPowerManager().removePower(target.getUniqueId(), -diff);
                    }
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &aМощь &f" + target.getName()
                                    + " &aустановлена: &f" + String.format("%.0f", amount));
                } catch (NumberFormatException e) {
                    MessageUtil.send(player, "&cНекорректное число.");
                }
            }

            case "forcedelete" -> {
                if (args.length < 2) {
                    MessageUtil.send(player,
                            "&c/war admin forcedelete <clan>");
                    return;
                }
                var clan = plugin.getClanManager().getClanByName(args[1]);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }
                String clanName = clan.getName();
                plugin.getTerritoryManager().removeAllClanTerritories(clan.getClanId());
                plugin.getWarManager().endAllClanWars(clan.getClanId());
                plugin.getSiegeManager().endAllClanSieges(clan.getClanId());
                plugin.getDiplomacyManager().removeAllClanRelations(clan.getClanId());
                plugin.getClanManager().removeClanFromCache(clan.getClanId());
                plugin.getStorageManager().deleteClan(clan.getClanId());
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &c✓ Клан &f" + clanName + " &cудалён.");
            }

            case "shield" -> {
                if (args.length < 3) {
                    MessageUtil.send(player,
                            "&c/war admin shield <clan> on/off/permanent [минуты]");
                    return;
                }
                var targetClan = plugin.getClanManager().getClanByName(args[1]);
                if (targetClan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }

                switch (args[2].toLowerCase()) {
                    case "on", "вкл" -> {
                        int duration = 60 * 60;
                        if (args.length > 3) {
                            try {
                                duration = Integer.parseInt(args[3]) * 60;
                            } catch (NumberFormatException e) {
                                MessageUtil.send(player, "&cУкажите число минут.");
                                return;
                            }
                        }
                        var result = plugin.getTerritoryManager()
                                .enableShield(targetClan.getClanId(), duration, true);
                        if (result == TerritoryManager.ShieldResult.SUCCESS) {
                            MessageUtil.send(player,
                                    "&8[&6EverWar&8] &a🛡 Щит клана &f"
                                            + targetClan.getName() + " &aвключён на &f"
                                            + (duration / 60) + " мин.");
                        }
                    }
                    case "off", "выкл" -> {
                        plugin.getTerritoryManager().disableShield(targetClan.getClanId());
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &c🛡 Щит &f" + targetClan.getName()
                                        + " &cвыключен.");
                    }
                    case "permanent", "постоянный" -> {
                        plugin.getTerritoryManager()
                                .enablePermanentShield(targetClan.getClanId());
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &a🛡 Клан &f" + targetClan.getName()
                                        + " &aполучил постоянный щит.");
                    }
                    default -> MessageUtil.send(player,
                            "&c/war admin shield <clan> on/off/permanent [минуты]");
                }
            }

            case "deserter" -> {
                if (args.length < 3) {
                    MessageUtil.send(player,
                            "&c/war admin deserter <clan> on/off [часы]");
                    return;
                }
                var targetClan = plugin.getClanManager().getClanByName(args[1]);
                if (targetClan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }

                switch (args[2].toLowerCase()) {
                    case "on" -> {
                        int hours = 1;
                        if (args.length > 3) {
                            try {
                                hours = Integer.parseInt(args[3]);
                            } catch (NumberFormatException ignored) {}
                        }
                        long until = Instant.now().getEpochSecond() + (hours * 3600L);
                        targetClan.setDeserter(true, until);
                        plugin.getStorageManager().saveClan(targetClan);
                        MessageUtil.broadcast(
                                "&4&l⚠ Админ объявил клан &f" + targetClan.getName()
                                        + " &4дезертиром на " + hours + " часов!");
                    }
                    case "off" -> {
                        targetClan.setDeserter(false, 0);
                        plugin.getStorageManager().saveClan(targetClan);
                        MessageUtil.send(player,
                                "&8[&6EverWar&8] &aКлан больше не дезертир.");
                    }
                }
            }

            case "info" -> {
                if (args.length < 2) {
                    MessageUtil.send(player, "&c/war admin info <clan>");
                    return;
                }
                var clan = plugin.getClanManager().getClanByName(args[1]);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }
                MessageUtil.send(player, "&8&l━━━━━ &cАдмин инфо &8&l━━━━━");
                MessageUtil.send(player, "&7UUID: &f" + clan.getClanId());
                MessageUtil.send(player, "&7Название: &f" + clan.getName());
                MessageUtil.send(player, "&7Тег: &f" + clan.getTag());
                MessageUtil.send(player, "&7Лидер: &f" + clan.getLeaderUUID());
                MessageUtil.send(player, "&7Участников: &f" + clan.getMemberCount());
                MessageUtil.send(player, "&7Территорий: &f"
                        + plugin.getTerritoryManager().getClanTerritoryCount(clan.getClanId()));
                MessageUtil.send(player, "&7Мощь: &f" + String.format("%.0f", clan.getTotalPower()));
                MessageUtil.send(player, plugin.getTerritoryManager()
                        .getShieldStatus(clan.getClanId(), lang));
                MessageUtil.send(player, "&7Дезертир: " +
                        (clan.isDeserter() ? "&4ДА" : "&aНет"));
                MessageUtil.send(player, "&7Атака союзников: " +
                        (clan.isAllowAttackAllies() ? "&cВКЛ" : "&aВЫКЛ"));
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
            }

            case "clearwars" -> {
                if (args.length < 2) {
                    MessageUtil.send(player, "&c/war admin clearwars <clan>");
                    return;
                }
                var clan = plugin.getClanManager().getClanByName(args[1]);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }
                plugin.getWarManager().endAllClanWars(clan.getClanId());
                MessageUtil.send(player, "&aВойны клана завершены.");
            }

            case "clearsieges" -> {
                if (args.length < 2) {
                    MessageUtil.send(player, "&c/war admin clearsieges <clan>");
                    return;
                }
                var clan = plugin.getClanManager().getClanByName(args[1]);
                if (clan == null) {
                    MessageUtil.sendMessage(player, "clan-not-found",
                            "{clan}", args[1]);
                    return;
                }
                plugin.getSiegeManager().endAllClanSieges(clan.getClanId());
                MessageUtil.send(player, "&aОсады клана завершены.");
            }

            case "list" -> {
                var clans = plugin.getClanManager().getTopClans(50);
                MessageUtil.send(player, "&8&l━━━━━ &cВсе кланы &8&l━━━━━");
                for (int i = 0; i < clans.size(); i++) {
                    var c = clans.get(i);
                    boolean shield = plugin.getTerritoryManager()
                            .isShieldActive(c.getClanId());
                    boolean deserter = c.isDeserter();
                    MessageUtil.send(player,
                            "&e" + (i + 1) + ". &f" + c.getName()
                                    + " &7[" + c.getTag() + "]"
                                    + " &7— &f" + c.getMemberCount() + " уч."
                                    + (shield ? " &a🛡" : " &7🛡")
                                    + (deserter ? " &4🚨" : ""));
                }
                MessageUtil.send(player,
                        "&7Всего: &f" + plugin.getClanManager().getClanCount());
                MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
            }

            default -> sendAdminHelp(player);
        }
    }

    private void sendAdminHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "     &c&l  EverWar — Администратор");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "&e/war admin reload &7— Перезагрузка");
        MessageUtil.send(player, "&e/war admin list &7— Список всех кланов");
        MessageUtil.send(player, "&e/war admin info <clan> &7— Инфо");
        MessageUtil.send(player, "&e/war admin setpower <player> <amount>");
        MessageUtil.send(player, "&e/war admin forcedelete <clan>");
        MessageUtil.send(player, "&e/war admin clearwars <clan>");
        MessageUtil.send(player, "&e/war admin clearsieges <clan>");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&6&lЩИТ:");
        MessageUtil.send(player, "&e/war admin shield <clan> on [мин]");
        MessageUtil.send(player, "&e/war admin shield <clan> off");
        MessageUtil.send(player, "&e/war admin shield <clan> permanent");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&4&lДЕЗЕРТИР:");
        MessageUtil.send(player, "&e/war admin deserter <clan> on [часы]");
        MessageUtil.send(player, "&e/war admin deserter <clan> off");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== ПОМОЩЬ ====================

    private void handleHelp(Player player) {
        String lang = plugin.getConfigManager().getLanguage();
        boolean isEn = lang.equals("en");

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "          &6&l⚔ EverWar " +
                (isEn ? "Help" : "Помощь"));
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "");

        MessageUtil.sendClickable(player, "&6/war &7— Главное меню (GUI)",
                "&aОткрыть", "/war");
        MessageUtil.sendClickable(player, "&e/war clan &7— Управление кланом",
                "&aКлан", "/war clan");
        MessageUtil.sendClickable(player, "&e/war territory &7— Территории",
                "&aТерритории", "/war territory");
        MessageUtil.sendClickable(player, "&e/war diplomacy &7— Дипломатия",
                "&aДипломатия", "/war diplomacy");
        MessageUtil.sendClickable(player, "&e/war war &7— Война",
                "&aВойна", "/war war");
        MessageUtil.sendClickable(player, "&e/war siege &7— Осада",
                "&aОсада", "/war siege");
        MessageUtil.sendClickable(player, "&e/war supply &7— Снабжение",
                "&aСклад", "/war supply");
        MessageUtil.sendClickable(player, "&a/war shield &7— Щит территории",
                "&aЩит", "/war shield");
        MessageUtil.sendClickable(player, "&4/war deserter &7— Режим дезертира",
                "&4Против всех", "/war deserter");
        MessageUtil.sendClickable(player, "&b/war country &7— Страна",
                "&bСтрана", "/war country");
        MessageUtil.sendClickable(player, "&e/war top &7— Рейтинг",
                "&aТоп", "/war top");
        MessageUtil.sendClickable(player, "&e/war map &7— Карта",
                "&aКарта", "/war map");

        MessageUtil.send(player, "");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan != null) {
            String shieldStatus = plugin.getTerritoryManager()
                    .getShieldStatus(clan.getClanId(), lang);
            MessageUtil.send(player, shieldStatus);
            if (clan.isDeserter()) {
                MessageUtil.send(player, "&4🚨 &lРЕЖИМ ДЕЗЕРТИРА АКТИВЕН!");
            }
        }

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== TAB COMPLETER ====================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        if (args.length == 1) {
            completions.addAll(List.of(
                    "clan", "territory", "diplomacy", "war", "siege",
                    "country", "shield", "supply", "deserter",
                    "top", "map", "help", "admin"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "clan" -> completions.addAll(List.of(
                        "create", "delete", "invite", "accept", "deny",
                        "leave", "kick", "info", "list", "members", "role"
                ));
                case "territory" -> completions.addAll(List.of(
                        "claim", "unclaim", "map", "setcore", "info", "upgrade"
                ));
                case "diplomacy" -> completions.addAll(List.of(
                        "ally", "enemy", "neutral", "accept", "reject", "list"
                ));
                case "war" -> completions.addAll(List.of(
                        "declare", "status", "surrender", "score"
                ));
                case "siege" -> completions.addAll(List.of(
                        "start", "stop", "status"
                ));
                case "country" -> completions.addAll(List.of(
                        "create", "delete", "invite", "join",
                        "leave", "info", "list"
                ));
                case "shield" -> completions.addAll(List.of(
                        "on", "off", "status", "permanent"
                ));
                case "supply" -> completions.addAll(List.of(
                        "status", "add", "gui", "help"
                ));
                case "deserter" -> completions.addAll(List.of(
                        "on", "off", "status"
                ));
                case "admin" -> completions.addAll(List.of(
                        "reload", "setpower", "forcedelete", "shield",
                        "deserter", "info", "clearwars", "clearsieges", "list"
                ));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "clan" -> {
                    if (args[1].equalsIgnoreCase("invite")
                            || args[1].equalsIgnoreCase("kick")
                            || args[1].equalsIgnoreCase("role")) {
                        plugin.getServer().getOnlinePlayers()
                                .forEach(p -> completions.add(p.getName()));
                    }
                    if (args[1].equalsIgnoreCase("info")) {
                        plugin.getClanManager().getAllClans()
                                .forEach(c -> completions.add(c.getName()));
                    }
                }
                case "diplomacy" -> plugin.getClanManager().getAllClans()
                        .forEach(c -> completions.add(c.getName()));
                case "war" -> {
                    if (args[1].equalsIgnoreCase("declare")) {
                        plugin.getClanManager().getAllClans()
                                .forEach(c -> completions.add(c.getName()));
                    }
                }
                case "country" -> {
                    if (args[1].equalsIgnoreCase("invite")) {
                        plugin.getClanManager().getAllClans()
                                .forEach(c -> completions.add(c.getName()));
                    }
                    if (args[1].equalsIgnoreCase("join")
                            || args[1].equalsIgnoreCase("info")) {
                        plugin.getCountryManager().getAllCountries()
                                .forEach(c -> completions.add(c.getName()));
                    }
                }
                case "shield" -> {
                    if (args[1].equalsIgnoreCase("on")) {
                        completions.addAll(List.of("1", "5", "10", "15"));
                    }
                }
                case "supply" -> {
                    if (args[1].equalsIgnoreCase("add")) {
                        completions.addAll(List.of("food", "materials", "fuel"));
                    }
                }
                case "deserter" -> {
                    if (args[1].equalsIgnoreCase("on")) {
                        completions.addAll(List.of("1", "3", "6", "12", "24"));
                    }
                }
                case "admin" -> {
                    switch (args[1].toLowerCase()) {
                        case "shield", "deserter", "forcedelete", "info",
                             "clearwars", "clearsieges" -> {
                            plugin.getClanManager().getAllClans()
                                    .forEach(c -> completions.add(c.getName()));
                        }
                        case "setpower" -> plugin.getServer().getOnlinePlayers()
                                .forEach(p -> completions.add(p.getName()));
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("admin")) {
                if (args[1].equalsIgnoreCase("shield")) {
                    completions.addAll(List.of("on", "off", "permanent"));
                }
                if (args[1].equalsIgnoreCase("deserter")) {
                    completions.addAll(List.of("on", "off"));
                }
            }
            if (args[0].equalsIgnoreCase("clan")
                    && args[1].equalsIgnoreCase("role")) {
                completions.addAll(List.of(
                        "LEADER", "GENERAL", "OFFICER", "FIGHTER", "RECRUIT"
                ));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .sorted()
                .collect(Collectors.toList());
    }
}