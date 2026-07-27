package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.War;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarManager {

    private final EverWar plugin;

    // warId -> War
    private final Map<UUID, War> warsById = new HashMap<>();

    // clanId -> список warId (активные войны клана)
    private final Map<UUID, List<UUID>> clanWars = new HashMap<>();

    // Таймер проверки войн (ticks)
    private int taskId = -1;

    public WarManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== КЭШ ====================

    public void addWarToCache(War war) {
        warsById.put(war.getWarId(), war);
        clanWars.computeIfAbsent(war.getAttackerClanId(), k -> new ArrayList<>())
                .add(war.getWarId());
        clanWars.computeIfAbsent(war.getDefenderClanId(), k -> new ArrayList<>())
                .add(war.getWarId());
    }

    public void removeWarFromCache(UUID warId) {
        War war = warsById.remove(warId);
        if (war != null) {
            List<UUID> attList = clanWars.get(war.getAttackerClanId());
            if (attList != null) attList.remove(warId);
            List<UUID> defList = clanWars.get(war.getDefenderClanId());
            if (defList != null) defList.remove(warId);
        }
    }

    // ==================== ОБЪЯВЛЕНИЕ ВОЙНЫ ====================

    public DeclareResult declareWar(Player declarer, String targetClanName) {
        Clan attackerClan = plugin.getClanManager().getClanByPlayer(declarer.getUniqueId());
        if (attackerClan == null) return DeclareResult.NOT_IN_CLAN;

        var member = attackerClan.getMember(declarer.getUniqueId());
        if (member == null || !member.getRole().canDeclareWar()) {
            return DeclareResult.NO_PERMISSION;
        }

        Clan defenderClan = plugin.getClanManager().getClanByName(targetClanName);
        if (defenderClan == null) return DeclareResult.TARGET_NOT_FOUND;

        if (attackerClan.getClanId().equals(defenderClan.getClanId())) {
            return DeclareResult.CANNOT_WAR_SELF;
        }

        // Уже в войне?
        if (areAtWar(attackerClan.getClanId(), defenderClan.getClanId())) {
            return DeclareResult.ALREADY_AT_WAR;
        }

        // Проверяем союз
        if (plugin.getDiplomacyManager().isAlly(
                attackerClan.getClanId(), defenderClan.getClanId())) {
            return DeclareResult.IS_ALLY;
        }

        // Проверяем снабжение
        int requiredFood = plugin.getConfigManager().getFoodPerWar();
        int requiredMaterials = plugin.getConfigManager().getMaterialsPerWar();
        if (!plugin.getSupplyManager().hasEnoughForWar(
                attackerClan.getClanId(), requiredFood, requiredMaterials)) {
            return DeclareResult.NOT_ENOUGH_SUPPLY;
        }

        // Минимум игроков онлайн
        int minPlayers = plugin.getConfigManager().getMinPlayersForWar();
        if (attackerClan.getOnlineMembers().size() < minPlayers) {
            return DeclareResult.NOT_ENOUGH_PLAYERS;
        }

        // Расходуем снабжение
        plugin.getSupplyManager().consumeForWar(
                attackerClan.getClanId(), requiredFood, requiredMaterials);

        // Устанавливаем отношение "враги"
        plugin.getDiplomacyManager().setEnemy(
                attackerClan.getClanId(), defenderClan.getClanId());

        // Создаём войну
        int prepTime = plugin.getConfigManager().getPreparationTime();
        UUID warId = UUID.randomUUID();
        War war = new War(warId, attackerClan.getClanId(),
                defenderClan.getClanId(), prepTime);

        addWarToCache(war);
        plugin.getStorageManager().saveWar(war);

        // Оповещение сервера
        String attackerName = attackerClan.getName();
        String defenderName = defenderClan.getName();

        if (plugin.getConfigManager().isBroadcastWar()) {
            String msg = plugin.getMessagesConfig().get("war-declared",
                    "{attacker}", attackerName,
                    "{defender}", defenderName);
            broadcastColored(msg);
        }

        war.addEvent("War declared by " + declarer.getName());

        // Запускаем таймер если ещё не запущен
        startWarTimer();

        return DeclareResult.SUCCESS;
    }

    // ==================== УПРАВЛЕНИЕ ВОЙНОЙ ====================

    public void startWar(War war) {
        war.start();
        plugin.getStorageManager().saveWar(war);

        Clan attacker = plugin.getClanManager().getClanById(war.getAttackerClanId());
        Clan defender = plugin.getClanManager().getClanById(war.getDefenderClanId());

        if (attacker != null && defender != null) {
            String msg = plugin.getMessagesConfig().get("war-started",
                    "{attacker}", attacker.getName(),
                    "{defender}", defender.getName());
            broadcastColored(msg);
        }
    }

    public void endWar(War war, UUID winnerClanId) {
        war.end(winnerClanId);
        plugin.getStorageManager().saveWar(war);

        Clan winner = plugin.getClanManager().getClanById(winnerClanId);
        UUID loserClanId = war.getOpponent(winnerClanId);
        Clan loser = plugin.getClanManager().getClanById(loserClanId);

        if (winner != null) {
            winner.addWarWon();
            plugin.getStorageManager().saveClan(winner);
        }
        if (loser != null) {
            loser.addWarLost();
            plugin.getStorageManager().saveClan(loser);
        }

        if (winner != null && loser != null) {
            String winMsg = plugin.getMessagesConfig().get("war-won",
                    "{winner}", winner.getName(),
                    "{loser}", loser.getName());
            broadcastColored(winMsg);
        }

        removeWarFromCache(war.getWarId());
    }

    public void endAllClanWars(UUID clanId) {
        List<UUID> warIds = new ArrayList<>(
                clanWars.getOrDefault(clanId, new ArrayList<>()));
        for (UUID warId : warIds) {
            War war = warsById.get(warId);
            if (war != null && war.getStatus() != War.WarStatus.ENDED) {
                UUID opponent = war.getOpponent(clanId);
                if (opponent != null) {
                    endWar(war, opponent);
                } else {
                    war.end(null);
                    plugin.getStorageManager().saveWar(war);
                    removeWarFromCache(warId);
                }
            }
        }
    }

    // ==================== ОЧКИ ====================

    public void addKillScore(UUID killerClanId, UUID victimClanId) {
        War war = getActivWar(killerClanId, victimClanId);
        if (war == null) return;

        int points = plugin.getConfigManager().getKillPoints();

        if (war.isAttacker(killerClanId)) {
            war.addAttackerScore(points);
        } else {
            war.addDefenderScore(points);
        }

        plugin.getStorageManager().saveWar(war);
    }

    public void addCaptureScore(UUID capturingClanId, UUID losingClanId) {
        War war = getActivWar(capturingClanId, losingClanId);
        if (war == null) return;

        int points = plugin.getConfigManager().getCapturePoints();

        if (war.isAttacker(capturingClanId)) {
            war.addAttackerScore(points);
        } else {
            war.addDefenderScore(points);
        }

        plugin.getStorageManager().saveWar(war);
    }

    // ==================== ТАЙМЕР ====================

    private void startWarTimer() {
        if (taskId != -1) return;

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<War> toProcess = new ArrayList<>(warsById.values());

            for (War war : toProcess) {
                if (war.getStatus() == War.WarStatus.PREPARATION) {
                    if (war.isPreparationOver()) {
                        startWar(war);
                    }
                }
            }

            // Если войн нет — останавливаем таймер
            if (warsById.isEmpty()) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            }

        }, 20L, 20L).getTaskId(); // каждую секунду
    }

    // ==================== ПОИСК ====================

    public War getActivWar(UUID clanA, UUID clanB) {
        List<UUID> warIds = clanWars.getOrDefault(clanA, new ArrayList<>());
        for (UUID warId : warIds) {
            War war = warsById.get(warId);
            if (war != null
                    && war.involves(clanB)
                    && war.getStatus() == War.WarStatus.ACTIVE) {
                return war;
            }
        }
        return null;
    }

    public War getWarBetween(UUID clanA, UUID clanB) {
        List<UUID> warIds = clanWars.getOrDefault(clanA, new ArrayList<>());
        for (UUID warId : warIds) {
            War war = warsById.get(warId);
            if (war != null && war.involves(clanB)
                    && war.getStatus() != War.WarStatus.ENDED) {
                return war;
            }
        }
        return null;
    }

    public boolean areAtWar(UUID clanA, UUID clanB) {
        return getWarBetween(clanA, clanB) != null;
    }

    public List<War> getClanWars(UUID clanId) {
        List<UUID> warIds = clanWars.getOrDefault(clanId, new ArrayList<>());
        List<War> result = new ArrayList<>();
        for (UUID warId : warIds) {
            War w = warsById.get(warId);
            if (w != null) result.add(w);
        }
        return result;
    }

    public War getWarById(UUID warId) {
        return warsById.get(warId);
    }

    public Collection<War> getAllWars() {
        return warsById.values();
    }

    public int getWarCount() {
        return warsById.size();
    }

    // ==================== УТИЛИТЫ ====================

    private void broadcastColored(String msg) {
        Bukkit.broadcastMessage(msg.replace("&", "\u00A7"));
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    public enum DeclareResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, TARGET_NOT_FOUND,
        CANNOT_WAR_SELF, ALREADY_AT_WAR, IS_ALLY,
        NOT_ENOUGH_SUPPLY, NOT_ENOUGH_PLAYERS
    }
}