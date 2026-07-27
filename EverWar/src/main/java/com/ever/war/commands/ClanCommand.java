package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.gui.ClanMenuGUI;
import com.ever.war.gui.MembersGUI;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.models.ClanRole;
import com.ever.war.utils.MessageUtil;
import org.bukkit.entity.Player;

public class ClanCommand {

    private final EverWar plugin;

    public ClanCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create", "создать" -> handleCreate(player, args);
            case "delete", "удалить" -> handleDelete(player);
            case "invite", "пригласить" -> handleInvite(player, args);
            case "accept", "принять" -> handleAccept(player);
            case "deny", "отклонить" -> handleDeny(player);
            case "leave", "выйти" -> handleLeave(player);
            case "kick", "кик", "исключить" -> handleKick(player, args);
            case "info", "инфо" -> handleInfo(player, args);
            case "list", "список" -> handleList(player);
            case "members", "участники" -> handleMembers(player);
            case "role", "роль" -> handleRole(player, args);
            case "menu", "меню" -> ClanMenuGUI.open(player);
            default -> sendHelp(player);
        }
    }

    // ==================== CREATE ====================

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war clan create <name> <tag>");
            return;
        }

        String name = args[1];
        String tag = args[2].toUpperCase();

        var result = plugin.getClanManager().createClan(player, name, tag);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "clan-created",
                        "{clan}", name, "{tag}", tag);
                MessageUtil.soundSuccess(player);
            }
            case ALREADY_IN_CLAN -> {
                MessageUtil.sendMessage(player, "already-in-clan");
                MessageUtil.soundError(player);
            }
            case INVALID_NAME -> {
                MessageUtil.sendMessage(player, "clan-name-invalid",
                        "{min}", String.valueOf(plugin.getConfigManager().getMinNameLength()),
                        "{max}", String.valueOf(plugin.getConfigManager().getMaxNameLength()));
                MessageUtil.soundError(player);
            }
            case INVALID_TAG -> {
                MessageUtil.sendMessage(player, "clan-tag-invalid",
                        "{length}", String.valueOf(plugin.getConfigManager().getTagLength()));
                MessageUtil.soundError(player);
            }
            case NAME_TAKEN -> {
                MessageUtil.sendMessage(player, "clan-name-taken",
                        "{name}", name);
                MessageUtil.soundError(player);
            }
            case TAG_TAKEN -> {
                MessageUtil.sendMessage(player, "clan-tag-taken",
                        "{tag}", tag);
                MessageUtil.soundError(player);
            }
            case NOT_ENOUGH_MONEY -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cНедостаточно денег. Нужно: &f"
                                + plugin.getConfigManager().getCreateCost());
                MessageUtil.soundError(player);
            }
        }
    }

    // ==================== DELETE ====================

    private void handleDelete(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        var result = plugin.getClanManager().deleteClan(player);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "clan-deleted",
                        "{clan}", clan.getName());
                MessageUtil.soundSuccess(player);
                MessageUtil.broadcast(plugin.getLanguageManager().get("clan-deleted",
                        "{clan}", clan.getName()));
            }
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cОшибка: клан не найден.");
        }
    }

    // ==================== INVITE ====================

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war clan invite <nick>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "player-not-found",
                    "{player}", args[1]);
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        var result = plugin.getClanManager().invitePlayer(player, target);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "clan-invited",
                        "{player}", target.getName());
                MessageUtil.soundSuccess(player);

                String clanName = clan != null ? clan.getName() : "Unknown";
                MessageUtil.sendMessage(target, "clan-invite-received",
                        "{clan}", clanName);

                // Кликабельное принятие
                MessageUtil.sendClickable(target,
                        "&a[ПРИНЯТЬ]",
                        "&aНажмите чтобы принять приглашение",
                        "/war clan accept");
                MessageUtil.sendClickable(target,
                        " &c[ОТКЛОНИТЬ]",
                        "&cНажмите чтобы отклонить",
                        "/war clan deny");

                MessageUtil.soundSuccess(target);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case TARGET_IN_CLAN -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cИгрок &f" + target.getName()
                                + " &cуже в клане.");
                MessageUtil.soundError(player);
            }
            case CLAN_FULL -> {
                MessageUtil.sendMessage(player, "clan-full",
                        "{max}", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                MessageUtil.soundError(player);
            }
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cОшибка: клан не найден.");
        }
    }

    // ==================== ACCEPT ====================

    private void handleAccept(Player player) {
        var result = plugin.getClanManager().acceptInvite(player);

        switch (result) {
            case SUCCESS -> {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                String clanName = clan != null ? clan.getName() : "Unknown";

                MessageUtil.sendMessage(player, "clan-joined",
                        "{player}", player.getName(), "{clan}", clanName);
                MessageUtil.soundSuccess(player);

                // Оповещаем всех участников
                if (clan != null) {
                    for (ClanMember m : clan.getMemberList()) {
                        if (m.isOnline() && !m.getPlayerUUID().equals(player.getUniqueId())) {
                            Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                            if (p != null) {
                                MessageUtil.sendMessage(p, "clan-joined",
                                        "{player}", player.getName(), "{clan}", clanName);
                            }
                        }
                    }
                }
            }
            case NO_INVITE -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cУ вас нет приглашений.");
                MessageUtil.soundError(player);
            }
            case INVITE_EXPIRED -> {
                MessageUtil.sendMessage(player, "clan-invite-expired",
                        "{clan}", "");
                MessageUtil.soundError(player);
            }
            case ALREADY_IN_CLAN -> MessageUtil.sendMessage(player, "already-in-clan");
            case CLAN_FULL -> MessageUtil.sendMessage(player, "clan-full",
                    "{max}", String.valueOf(plugin.getConfigManager().getMaxMembers()));
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cКлан больше не существует.");
        }
    }

    // ==================== DENY ====================

    private void handleDeny(Player player) {
        var result = plugin.getClanManager().denyInvite(player);
        switch (result) {
            case SUCCESS -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &7Приглашение отклонено.");
            case NO_INVITE -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cУ вас нет приглашений.");
        }
    }

    // ==================== LEAVE ====================

    private void handleLeave(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        var result = plugin.getClanManager().leavePlayer(player);

        switch (result) {
            case SUCCESS -> {
                String clanName = clan != null ? clan.getName() : "Unknown";
                MessageUtil.sendMessage(player, "clan-left",
                        "{player}", player.getName());
                MessageUtil.soundSuccess(player);

                // Оповещаем клан
                if (clan != null) {
                    for (ClanMember m : clan.getMemberList()) {
                        if (m.isOnline()) {
                            Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                            if (p != null) {
                                MessageUtil.sendMessage(p, "clan-left",
                                        "{player}", player.getName());
                            }
                        }
                    }
                }
            }
            case IS_LEADER -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cЛидер не может покинуть клан. Передайте лидерство или удалите клан.");
                MessageUtil.soundError(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cОшибка.");
        }
    }

    // ==================== KICK ====================

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war clan kick <nick>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "player-not-found",
                    "{player}", args[1]);
            return;
        }

        var result = plugin.getClanManager().kickPlayer(player, target);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "clan-kicked",
                        "{player}", target.getName());
                MessageUtil.send(target,
                        "&8[&6EverWar&8] &cВас исключили из клана.");
                MessageUtil.soundSuccess(player);
                MessageUtil.soundError(target);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case TARGET_NOT_IN_CLAN -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cИгрок не в вашем клане.");
            case CANNOT_KICK_HIGHER -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя кикнуть игрока с более высоким рангом.");
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cОшибка.");
        }
    }

    // ==================== INFO ====================

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        if (args.length >= 2) {
            clan = plugin.getClanManager().getClanByName(args[1]);
            if (clan == null) {
                MessageUtil.sendMessage(player, "clan-not-found",
                        "{clan}", args[1]);
                return;
            }
        } else {
            clan = plugin.getClanManager().getClanByPlayer(player);
            if (clan == null) {
                MessageUtil.sendMessage(player, "not-in-clan");
                return;
            }
        }

        String lang = plugin.getConfigManager().getLanguage();
        String info = clan.getInfoDisplay(lang);
        for (String line : info.split("\n")) {
            MessageUtil.send(player, line);
        }

        // Союзники/Враги
        var allies = plugin.getDiplomacyManager().getAllies(clan.getClanId());
        var enemies = plugin.getDiplomacyManager().getEnemies(clan.getClanId());

        String alliesStr = allies.isEmpty() ? "&7-" :
                allies.stream().map(Clan::getName).reduce((a, b) -> a + ", " + b).orElse("-");
        String enemiesStr = enemies.isEmpty() ? "&7-" :
                enemies.stream().map(Clan::getName).reduce((a, b) -> a + ", " + b).orElse("-");

        MessageUtil.send(player, plugin.getLanguageManager().get("info-allies",
                "{allies}", alliesStr));
        MessageUtil.send(player, plugin.getLanguageManager().get("info-enemies",
                "{enemies}", enemiesStr));
        MessageUtil.send(player, plugin.getLanguageManager().get("info-footer"));
    }

    // ==================== LIST ====================

    private void handleList(Player player) {
        var clans = plugin.getClanManager().getTopClans(20);
        String lang = plugin.getConfigManager().getLanguage();

        MessageUtil.send(player, "&8&l━━━━━ &6 " +
                (lang.equals("en") ? "Clan List" : "Список кланов") + " &8&l━━━━━");

        for (int i = 0; i < clans.size(); i++) {
            var c = clans.get(i);
            MessageUtil.sendClickable(player,
                    "&e" + (i + 1) + ". " + c.getColor() + c.getName()
                            + " &7[" + c.getTag() + "] &7- &f"
                            + c.getMemberCount() + " &7уч. &f"
                            + String.format("%.0f", c.getTotalPower()) + " &7мощи",
                    "&aНажмите для информации",
                    "/war clan info " + c.getName());
        }

        if (clans.isEmpty()) {
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "No clans yet." : "Кланов пока нет."));
        }

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== MEMBERS ====================

    private void handleMembers(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        MembersGUI.open(player, clan, 0);
    }

    // ==================== ROLE ====================

    private void handleRole(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war clan role <nick> <role>");
            MessageUtil.send(player,
                    "&7Роли: LEADER, GENERAL, OFFICER, FIGHTER, RECRUIT");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "player-not-found",
                    "{player}", args[1]);
            return;
        }

        ClanRole role = ClanRole.fromString(args[2]);
        var result = plugin.getClanManager().setRole(player, target, role);

        String lang = plugin.getConfigManager().getLanguage();

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "role-changed",
                        "{player}", target.getName(),
                        "{role}", role.getName(lang));
                MessageUtil.send(target,
                        "&8[&6EverWar&8] &eВам назначена роль: "
                                + role.getChatColor() + role.getIcon() + " "
                                + role.getName(lang));
                MessageUtil.soundSuccess(player);
                MessageUtil.soundSuccess(target);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "role-no-permission");
                MessageUtil.soundError(player);
            }
            case TARGET_NOT_IN_CLAN -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cИгрок не в вашем клане.");
            case CANNOT_ASSIGN_HIGHER -> {
                MessageUtil.sendMessage(player, "role-cannot-promote");
                MessageUtil.soundError(player);
            }
            case CANNOT_MANAGE_TARGET -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя менять роль этому игроку.");
            case NOT_LEADER -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cТолько лидер может передать лидерство.");
            case CLAN_NOT_FOUND -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cОшибка.");
        }
    }

    // ==================== ПОМОЩЬ ====================

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &6 Клан &8&l━━━━━");
        MessageUtil.send(player, "&e/war clan create <name> <tag> &7— Создать клан");
        MessageUtil.send(player, "&e/war clan delete &7— Удалить клан");
        MessageUtil.send(player, "&e/war clan invite <nick> &7— Пригласить");
        MessageUtil.send(player, "&e/war clan accept &7— Принять приглашение");
        MessageUtil.send(player, "&e/war clan deny &7— Отклонить");
        MessageUtil.send(player, "&e/war clan leave &7— Покинуть");
        MessageUtil.send(player, "&e/war clan kick <nick> &7— Исключить");
        MessageUtil.send(player, "&e/war clan info [clan] &7— Информация");
        MessageUtil.send(player, "&e/war clan list &7— Список кланов");
        MessageUtil.send(player, "&e/war clan members &7— Участники");
        MessageUtil.send(player, "&e/war clan role <nick> <role> &7— Назначить роль");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}