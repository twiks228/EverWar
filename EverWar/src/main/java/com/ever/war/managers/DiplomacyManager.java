package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Alliance;
import com.ever.war.models.Clan;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DiplomacyManager {

    private final EverWar plugin;
    private final Map<String, Alliance> alliances = new HashMap<>();

    public DiplomacyManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== КЭШ ====================

    public void addAllianceToCache(Alliance alliance) {
        alliances.put(alliance.getKey(), alliance);
    }

    public void removeAllianceFromCache(UUID clanA, UUID clanB) {
        alliances.remove(Alliance.makeKey(clanA, clanB));
    }

    // ==================== ОТНОШЕНИЯ ====================

    public Alliance getAlliance(UUID clanA, UUID clanB) {
        return alliances.get(Alliance.makeKey(clanA, clanB));
    }

    public Alliance.Relation getRelation(UUID clanA, UUID clanB) {
        Alliance a = getAlliance(clanA, clanB);
        return a != null ? a.getRelation() : Alliance.Relation.NEUTRAL;
    }

    public boolean isAlly(UUID clanA, UUID clanB) {
        return getRelation(clanA, clanB) == Alliance.Relation.ALLY;
    }

    public boolean isEnemy(UUID clanA, UUID clanB) {
        return getRelation(clanA, clanB) == Alliance.Relation.ENEMY;
    }

    public boolean isNeutral(UUID clanA, UUID clanB) {
        return getRelation(clanA, clanB) == Alliance.Relation.NEUTRAL;
    }

    // ==================== ПРЕДЛОЖЕНИЕ СОЮЗА ====================

    public ProposeResult proposeAlly(Player sender, String targetClanName) {
        Clan senderClan = plugin.getClanManager().getClanByPlayer(sender.getUniqueId());
        if (senderClan == null) return ProposeResult.NOT_IN_CLAN;

        var member = senderClan.getMember(sender.getUniqueId());
        if (member == null || !member.getRole().canManageDiplomacy()) {
            return ProposeResult.NO_PERMISSION;
        }

        Clan targetClan = plugin.getClanManager().getClanByName(targetClanName);
        if (targetClan == null) return ProposeResult.TARGET_NOT_FOUND;

        if (senderClan.getClanId().equals(targetClan.getClanId())) {
            return ProposeResult.CANNOT_ALLY_SELF;
        }

        if (isAlly(senderClan.getClanId(), targetClan.getClanId())) {
            return ProposeResult.ALREADY_ALLY;
        }

        if (plugin.getWarManager().areAtWar(
                senderClan.getClanId(), targetClan.getClanId())) {
            return ProposeResult.AT_WAR;
        }

        // Создаём или обновляем запись
        String key = Alliance.makeKey(senderClan.getClanId(), targetClan.getClanId());
        Alliance alliance = alliances.getOrDefault(key,
                new Alliance(senderClan.getClanId(), targetClan.getClanId(),
                        Alliance.Relation.NEUTRAL));

        alliance.proposeAlly(senderClan.getClanId());
        alliances.put(key, alliance);

        // ✅ КРАСИВОЕ УВЕДОМЛЕНИЕ ОБОИМ КЛАНАМ

        // Отправителю — подтверждение
        for (var m : senderClan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &e🤝 Предложение союза отправлено клану &f"
                                    + targetClan.getName());
                }
            }
        }

        // Получателю — расширенное сообщение с кнопками
        for (var m : targetClan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &e🤝 &lПРЕДЛОЖЕНИЕ СОЮЗА");
                    MessageUtil.send(p,
                            "&7Клан &f" + senderClan.getName()
                                    + " &7[" + senderClan.getTag() + "]");
                    MessageUtil.send(p,
                            "&7Отправитель: &f" + sender.getName()
                                    + " &7(роль: " + member.getRole().getName(
                                    plugin.getConfigManager().getLanguage()) + ")");
                    MessageUtil.send(p, "");

                    // Кликабельные кнопки
                    MessageUtil.sendClickable(p,
                            "  &a&l[✓ ПРИНЯТЬ]",
                            "&aПринять союз с " + senderClan.getName(),
                            "/war diplomacy accept " + senderClan.getName());

                    MessageUtil.sendClickable(p,
                            "  &c&l[✗ ОТКЛОНИТЬ]",
                            "&cОтклонить предложение",
                            "/war diplomacy reject " + senderClan.getName());

                    MessageUtil.send(p, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    // Звук всем, title только тем кто может принять решение
                    MessageUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

                    if (m.getRole().canManageDiplomacy()) {
                        MessageUtil.sendTitle(p,
                                "&e🤝 Союз?",
                                "&fОт клана " + senderClan.getName(),
                                10, 60, 10);
                    }
                }
            }
        }

        return ProposeResult.SUCCESS;
    }

    // ==================== ПРИНЯТЬ СОЮЗ ====================

    public AcceptResult acceptAlly(Player accepter, String senderClanName) {
        Clan accepterClan = plugin.getClanManager().getClanByPlayer(accepter.getUniqueId());
        if (accepterClan == null) return AcceptResult.NOT_IN_CLAN;

        var member = accepterClan.getMember(accepter.getUniqueId());
        if (member == null || !member.getRole().canManageDiplomacy()) {
            return AcceptResult.NO_PERMISSION;
        }

        Clan senderClan = plugin.getClanManager().getClanByName(senderClanName);
        if (senderClan == null) return AcceptResult.SENDER_NOT_FOUND;

        String key = Alliance.makeKey(accepterClan.getClanId(), senderClan.getClanId());
        Alliance alliance = alliances.get(key);

        if (alliance == null || !alliance.isPendingAlly()) {
            return AcceptResult.NO_PROPOSAL;
        }

        alliance.acceptAlly();
        plugin.getStorageManager().saveDiplomacy(alliance);

        // Оповещаем оба клана
        notifyClanAlliance(accepterClan, senderClan, true);
        notifyClanAlliance(senderClan, accepterClan, true);

        if (plugin.getConfigManager().isBroadcastAlliance()) {
            MessageUtil.broadcast(
                    "&8[&6EverWar&8] &a🤝 Клан &f" + senderClan.getName()
                            + " &aи клан &f" + accepterClan.getName()
                            + " &aтеперь союзники!");
        }

        return AcceptResult.SUCCESS;
    }

    private void notifyClanAlliance(Clan clan, Clan ally, boolean accepted) {
        for (var m : clan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    if (accepted) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &a🤝 Клан &f" + ally.getName()
                                        + " &aтеперь ваш &aсоюзник&a!");
                        MessageUtil.sendTitle(p,
                                "&a🤝 СОЮЗ",
                                "&f" + ally.getName(),
                                10, 60, 10);
                        MessageUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    } else {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &cКлан &f" + ally.getName()
                                        + " &cотклонил предложение союза.");
                    }
                }
            }
        }
    }

    // ==================== ОТКЛОНИТЬ СОЮЗ ====================

    public RejectResult rejectAlly(Player rejecter, String senderClanName) {
        Clan rejecterClan = plugin.getClanManager().getClanByPlayer(rejecter.getUniqueId());
        if (rejecterClan == null) return RejectResult.NOT_IN_CLAN;

        Clan senderClan = plugin.getClanManager().getClanByName(senderClanName);
        if (senderClan == null) return RejectResult.SENDER_NOT_FOUND;

        String key = Alliance.makeKey(rejecterClan.getClanId(), senderClan.getClanId());
        Alliance alliance = alliances.get(key);

        if (alliance == null || !alliance.isPendingAlly()) {
            return RejectResult.NO_PROPOSAL;
        }

        alliance.rejectAlly();

        notifyClanAlliance(senderClan, rejecterClan, false);

        return RejectResult.SUCCESS;
    }

    // ==================== ВРАГИ ====================

    public void setEnemy(UUID clanA, UUID clanB) {
        setRelation(clanA, clanB, Alliance.Relation.ENEMY);

        Clan a = plugin.getClanManager().getClanById(clanA);
        Clan b = plugin.getClanManager().getClanById(clanB);

        if (a != null && b != null) {
            notifyClanEnemy(a, b);
            notifyClanEnemy(b, a);
        }
    }

    private void notifyClanEnemy(Clan clan, Clan enemy) {
        for (var m : clan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &c⚠ Клан &f" + enemy.getName()
                                    + " &cобъявлен &lврагом&c!");
                    MessageUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1f);
                }
            }
        }
    }

    // ==================== НЕЙТРАЛИТЕТ ====================

    public SetNeutralResult setNeutral(Player setter, String targetClanName) {
        Clan setterClan = plugin.getClanManager().getClanByPlayer(setter.getUniqueId());
        if (setterClan == null) return SetNeutralResult.NOT_IN_CLAN;

        var member = setterClan.getMember(setter.getUniqueId());
        if (member == null || !member.getRole().canManageDiplomacy()) {
            return SetNeutralResult.NO_PERMISSION;
        }

        Clan targetClan = plugin.getClanManager().getClanByName(targetClanName);
        if (targetClan == null) return SetNeutralResult.TARGET_NOT_FOUND;

        if (plugin.getWarManager().areAtWar(
                setterClan.getClanId(), targetClan.getClanId())) {
            return SetNeutralResult.AT_WAR;
        }

        setRelation(setterClan.getClanId(), targetClan.getClanId(), Alliance.Relation.NEUTRAL);

        // Оповещаем оба клана
        for (var m : setterClan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &7Отношения с кланом &f" + targetClan.getName()
                                    + " &7теперь нейтральны.");
                }
            }
        }
        for (var m : targetClan.getMemberList()) {
            if (m.isOnline()) {
                Player p = plugin.getServer().getPlayer(m.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &7Клан &f" + setterClan.getName()
                                    + " &7установил нейтралитет.");
                }
            }
        }

        return SetNeutralResult.SUCCESS;
    }

    private void setRelation(UUID clanA, UUID clanB, Alliance.Relation relation) {
        String key = Alliance.makeKey(clanA, clanB);
        Alliance alliance = alliances.getOrDefault(key,
                new Alliance(clanA, clanB, relation));
        alliance.setRelation(relation);
        alliances.put(key, alliance);
        plugin.getStorageManager().saveDiplomacy(alliance);
    }

    // ==================== УДАЛЕНИЕ ====================

    public void removeAllClanRelations(UUID clanId) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Alliance> entry : alliances.entrySet()) {
            if (entry.getValue().involves(clanId)) {
                toRemove.add(entry.getKey());
                plugin.getStorageManager().deleteDiplomacy(
                        entry.getValue().getClanA(),
                        entry.getValue().getClanB());
            }
        }
        toRemove.forEach(alliances::remove);
    }

    // ==================== СПИСКИ ====================

    public List<Clan> getAllies(UUID clanId) {
        List<Clan> result = new ArrayList<>();
        for (Alliance a : alliances.values()) {
            if (a.involves(clanId) && a.isAlly()) {
                UUID opponentId = a.getOpponent(clanId);
                Clan c = plugin.getClanManager().getClanById(opponentId);
                if (c != null) result.add(c);
            }
        }
        return result;
    }

    public List<Clan> getEnemies(UUID clanId) {
        List<Clan> result = new ArrayList<>();
        for (Alliance a : alliances.values()) {
            if (a.involves(clanId) && a.isEnemy()) {
                UUID opponentId = a.getOpponent(clanId);
                Clan c = plugin.getClanManager().getClanById(opponentId);
                if (c != null) result.add(c);
            }
        }
        return result;
    }

    public Collection<Alliance> getAllAlliances() {
        return alliances.values();
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    public enum ProposeResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, TARGET_NOT_FOUND,
        CANNOT_ALLY_SELF, ALREADY_ALLY, AT_WAR
    }

    public enum AcceptResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, SENDER_NOT_FOUND, NO_PROPOSAL
    }

    public enum RejectResult {
        SUCCESS, NOT_IN_CLAN, SENDER_NOT_FOUND, NO_PROPOSAL
    }

    public enum SetNeutralResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, TARGET_NOT_FOUND, AT_WAR
    }
}