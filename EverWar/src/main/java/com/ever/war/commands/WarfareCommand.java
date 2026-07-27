package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.War;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.entity.Player;

import java.util.List;

public class WarfareCommand {

    private final EverWar plugin;

    public WarfareCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "declare", "объявить" -> handleDeclare(player, args);
            case "status", "статус" -> handleStatus(player);
            case "surrender", "сдаться" -> handleSurrender(player);
            case "score", "счёт" -> handleScore(player);
            default -> sendHelp(player);
        }
    }

    private void handleDeclare(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage",
                    "{usage}", "/war war declare <clan>");
            return;
        }

        var result = plugin.getWarManager().declareWar(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.soundWar(player);
                MessageUtil.sendTitle(player,
                        "&4⚔ ВОЙНА", "&cОбъявлена!", 10, 60, 10);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case TARGET_NOT_FOUND -> MessageUtil.sendMessage(player, "clan-not-found",
                    "{clan}", args[1]);
            case CANNOT_WAR_SELF -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя объявить войну самим себе.");
            case ALREADY_AT_WAR -> {
                MessageUtil.sendMessage(player, "war-already");
                MessageUtil.soundError(player);
            }
            case IS_ALLY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя объявить войну союзнику. Сначала разорвите союз.");
            case NOT_ENOUGH_SUPPLY -> {
                MessageUtil.sendMessage(player, "war-no-supply");
                MessageUtil.soundError(player);
            }
            case NOT_ENOUGH_PLAYERS -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНужно минимум &f"
                            + plugin.getConfigManager().getMinPlayersForWar()
                            + " &cигроков онлайн в клане.");
        }
    }

    private void handleStatus(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        String lang = plugin.getConfigManager().getLanguage();
        List<War> wars = plugin.getWarManager().getClanWars(clan.getClanId());

        if (wars.isEmpty()) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &7" + (lang.equals("en")
                            ? "No active wars." : "Нет активных войн."));
            return;
        }

        MessageUtil.send(player, "&8&l━━━━━ &4⚔ "
                + (lang.equals("en") ? "Wars" : "Войны") + " &8&l━━━━━");

        for (War war : wars) {
            Clan attacker = plugin.getClanManager().getClanById(war.getAttackerClanId());
            Clan defender = plugin.getClanManager().getClanById(war.getDefenderClanId());

            String attackerName = attacker != null ? attacker.getName() : "Unknown";
            String defenderName = defender != null ? defender.getName() : "Unknown";

            MessageUtil.send(player, "");
            MessageUtil.send(player,
                    "&c" + attackerName + " &7vs &c" + defenderName);
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Status: " : "Статус: ")
                            + war.getStatusDisplay(lang));

            if (war.getStatus() == War.WarStatus.PREPARATION) {
                long remaining = war.getSecondsUntilStart();
                MessageUtil.send(player,
                        "&e⏳ " + (lang.equals("en") ? "Starts in: " : "Начало через: ")
                                + "&f" + TimeUtil.formatTime(remaining, lang));
            }

            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Score: " : "Счёт: ")
                            + war.getScoreDisplay(attackerName, defenderName));
        }

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleSurrender(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }

        List<War> wars = plugin.getWarManager().getClanWars(clan.getClanId());

        if (wars.isEmpty()) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &7Нет активных войн.");
            return;
        }

        // Сдаёмся во всех войнах
        for (War war : wars) {
            if (war.getStatus() != War.WarStatus.ENDED) {
                java.util.UUID opponentId = war.getOpponent(clan.getClanId());
                plugin.getWarManager().endWar(war, opponentId);
            }
        }

        MessageUtil.send(player,
                "&8[&6EverWar&8] &c💀 Ваш клан сдался во всех войнах.");
        MessageUtil.broadcast(
                "&8[&6EverWar&8] &c💀 Клан &f" + clan.getName() + " &cсдался!");
    }

    private void handleScore(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        List<War> wars = plugin.getWarManager().getClanWars(clan.getClanId());

        for (War war : wars) {
            if (war.getStatus() == War.WarStatus.ACTIVE) {
                Clan attacker = plugin.getClanManager().getClanById(war.getAttackerClanId());
                Clan defender = plugin.getClanManager().getClanById(war.getDefenderClanId());
                String attackerName = attacker != null ? attacker.getName() : "?";
                String defenderName = defender != null ? defender.getName() : "?";

                MessageUtil.sendMessage(player, "war-score",
                        "{attacker}", attackerName,
                        "{attacker_score}", String.valueOf(war.getAttackerScore()),
                        "{defender}", defenderName,
                        "{defender_score}", String.valueOf(war.getDefenderScore()));
            }
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &4⚔ Война &8&l━━━━━");
        MessageUtil.send(player, "&e/war war declare <clan> &7— Объявить войну");
        MessageUtil.send(player, "&e/war war status &7— Статус войн");
        MessageUtil.send(player, "&e/war war score &7— Счёт текущей войны");
        MessageUtil.send(player, "&e/war war surrender &7— Сдаться");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}