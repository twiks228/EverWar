package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.utils.MessageUtil;
import org.bukkit.entity.Player;

public class DiplomacyCommand {

    private final EverWar plugin;

    public DiplomacyCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "ally", "союз" -> handleAlly(player, args);
            case "enemy", "враг" -> handleEnemy(player, args);
            case "neutral", "нейтрал" -> handleNeutral(player, args);
            case "accept", "принять" -> handleAcceptAlly(player, args);
            case "reject", "отклонить" -> handleRejectAlly(player, args);
            case "list", "список" -> handleList(player);
            default -> sendHelp(player);
        }
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war diplomacy ally <clan>");
            return;
        }

        var result = plugin.getDiplomacyManager().proposeAlly(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &eПредложение союза отправлено клану &f" + args[1]);
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case TARGET_NOT_FOUND -> MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            case CANNOT_ALLY_SELF -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя заключить союз с собой.");
            case ALREADY_ALLY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cВы уже союзники.");
            case AT_WAR -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя заключить союз во время войны.");
        }
    }

    private void handleAcceptAlly(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war diplomacy accept <clan>");
            return;
        }

        var result = plugin.getDiplomacyManager().acceptAlly(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "alliance-accepted",
                        "{clan}", args[1]);
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case SENDER_NOT_FOUND -> MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            case NO_PROPOSAL -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНет предложения союза от этого клана.");
        }
    }

    private void handleRejectAlly(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war diplomacy reject <clan>");
            return;
        }

        var result = plugin.getDiplomacyManager().rejectAlly(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "alliance-rejected",
                        "{clan}", args[1]);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case SENDER_NOT_FOUND -> MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            case NO_PROPOSAL -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНет предложения от этого клана.");
        }
    }

    private void handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war diplomacy enemy <clan>");
            return;
        }

        var clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        var member = clan.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canManageDiplomacy()) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }

        var targetClan = plugin.getClanManager().getClanByName(args[1]);
        if (targetClan == null) {
            MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            return;
        }

        if (clan.getClanId().equals(targetClan.getClanId())) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя объявить врагом самих себя.");
            return;
        }

        plugin.getDiplomacyManager().setEnemy(clan.getClanId(), targetClan.getClanId());
        MessageUtil.sendMessage(player, "enemy-declared",
                "{clan}", targetClan.getName());
        MessageUtil.soundWar(player);
    }

    private void handleNeutral(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war diplomacy neutral <clan>");
            return;
        }

        var result = plugin.getDiplomacyManager().setNeutral(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "neutral-set",
                        "{clan}", args[1]);
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> MessageUtil.sendMessage(player, "no-permission");
            case TARGET_NOT_FOUND -> MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            case AT_WAR -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя установить нейтралитет во время войны.");
        }
    }

    private void handleList(Player player) {
        var clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        String lang = plugin.getConfigManager().getLanguage();
        var allies = plugin.getDiplomacyManager().getAllies(clan.getClanId());
        var enemies = plugin.getDiplomacyManager().getEnemies(clan.getClanId());

        MessageUtil.send(player, "&8&l━━━━━ &6 Дипломатия &8&l━━━━━");

        MessageUtil.send(player, "&a🤝 "
                + (lang.equals("en") ? "Allies:" : "Союзники:"));
        if (allies.isEmpty()) {
            MessageUtil.send(player, "  &7-");
        } else {
            for (var a : allies) {
                MessageUtil.sendClickable(player,
                        "  &a• &f" + a.getName() + " &7[" + a.getTag() + "]",
                        "&aИнформация о клане",
                        "/war clan info " + a.getName());
            }
        }

        MessageUtil.send(player, "&c⚔ "
                + (lang.equals("en") ? "Enemies:" : "Враги:"));
        if (enemies.isEmpty()) {
            MessageUtil.send(player, "  &7-");
        } else {
            for (var e : enemies) {
                MessageUtil.sendClickable(player,
                        "  &c• &f" + e.getName() + " &7[" + e.getTag() + "]",
                        "&cИнформация о клане",
                        "/war clan info " + e.getName());
            }
        }

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &6 Дипломатия &8&l━━━━━");
        MessageUtil.send(player, "&e/war diplomacy ally <clan> &7— Предложить союз");
        MessageUtil.send(player, "&e/war diplomacy accept <clan> &7— Принять союз");
        MessageUtil.send(player, "&e/war diplomacy reject <clan> &7— Отклонить");
        MessageUtil.send(player, "&e/war diplomacy enemy <clan> &7— Объявить врагом");
        MessageUtil.send(player, "&e/war diplomacy neutral <clan> &7— Нейтралитет");
        MessageUtil.send(player, "&e/war diplomacy list &7— Список отношений");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}