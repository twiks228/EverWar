package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Siege;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class SiegeCommand {

    private final EverWar plugin;

    public SiegeCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start", "начать" -> handleStart(player);
            case "stop", "стоп" -> handleStop(player);
            case "status", "статус" -> handleStatus(player);
            default -> sendHelp(player);
        }
    }

    private void handleStart(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getSiegeManager().startSiege(player, chunk);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.soundWar(player);
                MessageUtil.sendTitle(player,
                        "&c⚔ ОСАДА", "&7Точка осады установлена!", 10, 60, 10);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case NOT_CLAIMED -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cЭтот чанк никому не принадлежит.");
            case OWN_TERRITORY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя осаждать свою территорию.");
            case NOT_AT_WAR -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cВы не в состоянии войны с владельцем территории. Сначала объявите войну.");
            case ALREADY_SIEGED -> {
                MessageUtil.sendMessage(player, "siege-already");
                MessageUtil.soundError(player);
            }
            case NOT_ENOUGH_MONEY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНедостаточно денег. Стоимость: &f"
                            + plugin.getConfigManager().getSiegeCost());
        }
    }

    private void handleStop(Player player) {
        var result = plugin.getSiegeManager().stopSiege(player, null);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cОсада прекращена.");
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_ACTIVE_SIEGE -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cУ вашего клана нет активных осад.");
        }
    }

    private void handleStatus(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        String lang = plugin.getConfigManager().getLanguage();

        // Наши осады (мы атакуем)
        Siege ourSiege = plugin.getSiegeManager().getActiveSiegeByAttacker(clan.getClanId());

        MessageUtil.send(player, "&8&l━━━━━ &c⚔ "
                + (lang.equals("en") ? "Siege Status" : "Статус осады") + " &8&l━━━━━");

        if (ourSiege != null) {
            Clan defender = plugin.getClanManager().getClanById(ourSiege.getDefenderClanId());
            String defName = defender != null ? defender.getName() : "Unknown";

            MessageUtil.send(player, "");
            MessageUtil.send(player,
                    "&c⚔ " + (lang.equals("en") ? "Attacking: " : "Атакуем: ") + "&f" + defName);
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Chunk: " : "Чанк: ") + "&f" + ourSiege.getChunkKey());
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Progress: " : "Прогресс: ") + ourSiege.getProgressBar());
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Time: " : "Время: ") + "&f"
                            + TimeUtil.formatTime(ourSiege.getElapsedSeconds(), lang));
        } else {
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "No active siege (attacking)." : "Нет активных осад (атака)."));
        }

        // Осады на нашу территорию
        boolean beingSieged = false;
        for (Siege siege : plugin.getSiegeManager().getAllSieges()) {
            if (siege.getDefenderClanId().equals(clan.getClanId()) && siege.isActive()) {
                beingSieged = true;
                Clan attacker = plugin.getClanManager().getClanById(siege.getAttackerClanId());
                String attName = attacker != null ? attacker.getName() : "Unknown";

                MessageUtil.send(player, "");
                MessageUtil.send(player,
                        "&c🛡 " + (lang.equals("en") ? "Being attacked by: " : "Нас атакует: ")
                                + "&f" + attName);
                MessageUtil.send(player,
                        "&7" + (lang.equals("en") ? "Chunk: " : "Чанк: ") + "&f" + siege.getChunkKey());
                MessageUtil.send(player,
                        "&7" + (lang.equals("en") ? "Progress: " : "Прогресс: ") + siege.getProgressBar());
            }
        }

        if (!beingSieged) {
            MessageUtil.send(player,
                    "&7" + (lang.equals("en") ? "Territory is safe." : "Территория в безопасности."));
        }

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &c⚔ Осада &8&l━━━━━");
        MessageUtil.send(player, "&e/war siege start &7— Начать осаду чанка");
        MessageUtil.send(player, "&e/war siege stop &7— Прекратить осаду");
        MessageUtil.send(player, "&e/war siege status &7— Статус осады");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}