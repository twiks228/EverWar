package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.models.ClanRole;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClanManager {

    private final EverWar plugin;

    // Главный кэш кланов: clanId -> Clan
    private final Map<UUID, Clan> clansById = new HashMap<>();

    // Быстрый поиск по имени
    private final Map<String, UUID> clansByName = new HashMap<>();

    // Быстрый поиск по тегу
    private final Map<String, UUID> clansByTag = new HashMap<>();

    // Быстрый поиск клана по UUID игрока
    private final Map<UUID, UUID> playerClanMap = new HashMap<>();

    // Приглашения: playerUUID -> clanId
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    public ClanManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== КЭШРОВАНИЕ ====================

    public void addClanToCache(Clan clan) {
        clansById.put(clan.getClanId(), clan);
        clansByName.put(clan.getName().toLowerCase(), clan.getClanId());
        clansByTag.put(clan.getTag().toLowerCase(), clan.getClanId());

        // Обновляем маппинг игроков
        for (UUID memberUUID : clan.getMemberUUIDs()) {
            playerClanMap.put(memberUUID, clan.getClanId());
        }
    }

    public void removeClanFromCache(UUID clanId) {
        Clan clan = clansById.remove(clanId);
        if (clan != null) {
            clansByName.remove(clan.getName().toLowerCase());
            clansByTag.remove(clan.getTag().toLowerCase());
            for (UUID memberUUID : clan.getMemberUUIDs()) {
                playerClanMap.remove(memberUUID);
            }
        }
    }

    // ==================== СОЗДАНИЕ / УДАЛЕНИЕ КЛАНА ====================

    public CreateResult createClan(Player leader, String name, String tag) {
        String lang = plugin.getConfigManager().getLanguage();

        // Проверяем — уже в клане?
        if (playerClanMap.containsKey(leader.getUniqueId())) {
            return CreateResult.ALREADY_IN_CLAN;
        }

        // Проверяем длину имени
        int minLen = plugin.getConfigManager().getMinNameLength();
        int maxLen = plugin.getConfigManager().getMaxNameLength();
        if (name.length() < minLen || name.length() > maxLen) {
            return CreateResult.INVALID_NAME;
        }

        // Проверяем длину тега
        int tagLen = plugin.getConfigManager().getTagLength();
        if (tag.length() != tagLen) {
            return CreateResult.INVALID_TAG;
        }

        // Проверяем уникальность имени
        if (clansByName.containsKey(name.toLowerCase())) {
            return CreateResult.NAME_TAKEN;
        }

        // Проверяем уникальность тега
        if (clansByTag.containsKey(tag.toLowerCase())) {
            return CreateResult.TAG_TAKEN;
        }

        // Проверяем баланс (если Vault подключён)
        double cost = plugin.getConfigManager().getCreateCost();
        if (cost > 0 && plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            if (!plugin.getVaultHook().has(leader, cost)) {
                return CreateResult.NOT_ENOUGH_MONEY;
            }
            plugin.getVaultHook().withdraw(leader, cost);
        }

        // Создаём клан
        UUID clanId = UUID.randomUUID();
        Clan clan = new Clan(clanId, name, tag, leader.getUniqueId(), leader.getName());

        // Сохраняем в кэш
        addClanToCache(clan);
        playerClanMap.put(leader.getUniqueId(), clanId);

        // Сохраняем в БД
        plugin.getStorageManager().saveClan(clan);
        plugin.getStorageManager().saveClanMember(clanId, clan.getMember(leader.getUniqueId()));

        // Создаём снабжение для клана
        plugin.getSupplyManager().createSupplyForClan(clanId);

        // LuckPerms — назначаем группу лидера
        if (plugin.getLuckPermsHook() != null) {
            plugin.getLuckPermsHook().setGroup(leader, "clan_leader");
        }

        return CreateResult.SUCCESS;
    }

    public DeleteResult deleteClan(Player leader) {
        UUID clanId = playerClanMap.get(leader.getUniqueId());
        if (clanId == null) return DeleteResult.NOT_IN_CLAN;

        Clan clan = clansById.get(clanId);
        if (clan == null) return DeleteResult.CLAN_NOT_FOUND;

        if (!clan.isLeader(leader.getUniqueId())) {
            return DeleteResult.NO_PERMISSION;
        }

        // Удаляем территории клана
        plugin.getTerritoryManager().removeAllClanTerritories(clanId);

        // Заканчиваем войны клана
        plugin.getWarManager().endAllClanWars(clanId);

        // Заканчиваем осады
        plugin.getSiegeManager().endAllClanSieges(clanId);

        // Удаляем дипломатию
        plugin.getDiplomacyManager().removeAllClanRelations(clanId);

        // Убираем из кэша
        removeClanFromCache(clanId);

        // Удаляем из БД
        plugin.getStorageManager().deleteClan(clanId);

        // LuckPerms — сбрасываем группы всех участников
        if (plugin.getLuckPermsHook() != null) {
            for (ClanMember member : clan.getMemberList()) {
                plugin.getLuckPermsHook().removeAllClanGroups(member.getPlayerUUID());
            }
        }

        return DeleteResult.SUCCESS;
    }

    // ==================== ПРИГЛАШЕНИЕ ====================

    public InviteResult invitePlayer(Player sender, Player target) {
        UUID senderClanId = playerClanMap.get(sender.getUniqueId());
        if (senderClanId == null) return InviteResult.NOT_IN_CLAN;

        Clan clan = clansById.get(senderClanId);
        if (clan == null) return InviteResult.CLAN_NOT_FOUND;

        ClanMember senderMember = clan.getMember(sender.getUniqueId());
        if (!senderMember.getRole().canInvite()) return InviteResult.NO_PERMISSION;

        if (playerClanMap.containsKey(target.getUniqueId())) {
            return InviteResult.TARGET_IN_CLAN;
        }

        if (clan.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            return InviteResult.CLAN_FULL;
        }

        // Приглашаем
        clan.invite(target.getUniqueId());
        pendingInvites.put(target.getUniqueId(), senderClanId);

        return InviteResult.SUCCESS;
    }

    public JoinResult acceptInvite(Player player) {
        UUID clanId = pendingInvites.get(player.getUniqueId());
        if (clanId == null) return JoinResult.NO_INVITE;

        Clan clan = clansById.get(clanId);
        if (clan == null) {
            pendingInvites.remove(player.getUniqueId());
            return JoinResult.CLAN_NOT_FOUND;
        }

        if (!clan.hasInvite(player.getUniqueId())) {
            pendingInvites.remove(player.getUniqueId());
            return JoinResult.INVITE_EXPIRED;
        }

        if (playerClanMap.containsKey(player.getUniqueId())) {
            return JoinResult.ALREADY_IN_CLAN;
        }

        if (clan.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            return JoinResult.CLAN_FULL;
        }

        // Добавляем участника
        ClanMember member = new ClanMember(player.getUniqueId(), player.getName(), ClanRole.RECRUIT);
        clan.addMember(member);
        playerClanMap.put(player.getUniqueId(), clanId);
        pendingInvites.remove(player.getUniqueId());

        // Сохраняем
        plugin.getStorageManager().saveClanMember(clanId, member);

        // LuckPerms
        if (plugin.getLuckPermsHook() != null) {
            plugin.getLuckPermsHook().setGroup(player, "clan_recruit");
        }

        return JoinResult.SUCCESS;
    }

    public DenyResult denyInvite(Player player) {
        UUID clanId = pendingInvites.remove(player.getUniqueId());
        if (clanId == null) return DenyResult.NO_INVITE;

        Clan clan = clansById.get(clanId);
        if (clan != null) {
            clan.removeInvite(player.getUniqueId());
        }

        return DenyResult.SUCCESS;
    }

    // ==================== ВЫХОД / ИСКЛЮЧЕНИЕ ====================

    public LeaveResult leavePlayer(Player player) {
        UUID clanId = playerClanMap.get(player.getUniqueId());
        if (clanId == null) return LeaveResult.NOT_IN_CLAN;

        Clan clan = clansById.get(clanId);
        if (clan == null) return LeaveResult.CLAN_NOT_FOUND;

        if (clan.isLeader(player.getUniqueId())) {
            return LeaveResult.IS_LEADER;
        }

        clan.removeMember(player.getUniqueId());
        playerClanMap.remove(player.getUniqueId());

        // Пересчитываем мощь
        clan.recalculatePower();

        // Удаляем из БД
        plugin.getStorageManager().deleteClanMember(player.getUniqueId());
        plugin.getStorageManager().saveClan(clan);

        // LuckPerms
        if (plugin.getLuckPermsHook() != null) {
            plugin.getLuckPermsHook().removeAllClanGroups(player.getUniqueId());
        }

        return LeaveResult.SUCCESS;
    }

    public KickResult kickPlayer(Player kicker, Player target) {
        UUID clanId = playerClanMap.get(kicker.getUniqueId());
        if (clanId == null) return KickResult.NOT_IN_CLAN;

        Clan clan = clansById.get(clanId);
        if (clan == null) return KickResult.CLAN_NOT_FOUND;

        ClanMember kickerMember = clan.getMember(kicker.getUniqueId());
        if (!kickerMember.getRole().canKick()) return KickResult.NO_PERMISSION;

        if (!clan.hasMember(target.getUniqueId())) return KickResult.TARGET_NOT_IN_CLAN;

        ClanMember targetMember = clan.getMember(target.getUniqueId());

        // Нельзя кикнуть того кто выше по рангу
        if (!kickerMember.getRole().canManage(targetMember.getRole())) {
            return KickResult.CANNOT_KICK_HIGHER;
        }

        clan.removeMember(target.getUniqueId());
        playerClanMap.remove(target.getUniqueId());
        clan.recalculatePower();

        plugin.getStorageManager().deleteClanMember(target.getUniqueId());
        plugin.getStorageManager().saveClan(clan);

        if (plugin.getLuckPermsHook() != null) {
            plugin.getLuckPermsHook().removeAllClanGroups(target.getUniqueId());
        }

        return KickResult.SUCCESS;
    }

    // ==================== РОЛИ ====================

    public RoleResult setRole(Player setter, Player target, ClanRole newRole) {
        UUID clanId = playerClanMap.get(setter.getUniqueId());
        if (clanId == null) return RoleResult.NOT_IN_CLAN;

        if (!playerClanMap.getOrDefault(target.getUniqueId(), UUID.randomUUID()).equals(clanId)) {
            return RoleResult.TARGET_NOT_IN_CLAN;
        }

        Clan clan = clansById.get(clanId);
        if (clan == null) return RoleResult.CLAN_NOT_FOUND;

        ClanMember setterMember = clan.getMember(setter.getUniqueId());
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (!setterMember.getRole().canAssignRoles()) return RoleResult.NO_PERMISSION;
        if (!setterMember.getRole().canManage(newRole)) return RoleResult.CANNOT_ASSIGN_HIGHER;
        if (!setterMember.getRole().canManage(targetMember.getRole())) return RoleResult.CANNOT_MANAGE_TARGET;

        // Если назначаем LEADER — передаём лидерство
        if (newRole == ClanRole.LEADER) {
            if (!clan.isLeader(setter.getUniqueId())) return RoleResult.NOT_LEADER;
            // Понижаем текущего лидера до GENERAL
            setterMember.setRole(ClanRole.GENERAL);
            clan.setLeaderUUID(target.getUniqueId());

            if (plugin.getLuckPermsHook() != null) {
                plugin.getLuckPermsHook().setGroup(setter, "clan_general");
            }
        }

        clan.setMemberRole(target.getUniqueId(), newRole);

        // Сохраняем обоих
        plugin.getStorageManager().saveClanMember(clanId, setterMember);
        plugin.getStorageManager().saveClanMember(clanId, targetMember);
        plugin.getStorageManager().saveClan(clan);

        // LuckPerms
        if (plugin.getLuckPermsHook() != null) {
            String group = switch (newRole) {
                case LEADER  -> "clan_leader";
                case GENERAL -> "clan_general";
                case OFFICER -> "clan_officer";
                case FIGHTER -> "clan_fighter";
                case RECRUIT -> "clan_recruit";
            };
            plugin.getLuckPermsHook().setGroup(target, group);
        }

        return RoleResult.SUCCESS;
    }

    // ==================== ПОИСК ====================

    public Clan getClanById(UUID clanId) {
        return clansById.get(clanId);
    }

    public Clan getClanByName(String name) {
        UUID id = clansByName.get(name.toLowerCase());
        return id != null ? clansById.get(id) : null;
    }

    public Clan getClanByTag(String tag) {
        UUID id = clansByTag.get(tag.toLowerCase());
        return id != null ? clansById.get(id) : null;
    }

    public Clan getClanByPlayer(UUID playerUUID) {
        UUID clanId = playerClanMap.get(playerUUID);
        return clanId != null ? clansById.get(clanId) : null;
    }

    public Clan getClanByPlayer(Player player) {
        return getClanByPlayer(player.getUniqueId());
    }

    public boolean isInClan(UUID playerUUID) {
        return playerClanMap.containsKey(playerUUID);
    }

    public boolean isInClan(Player player) {
        return isInClan(player.getUniqueId());
    }

    public Collection<Clan> getAllClans() {
        return clansById.values();
    }

    public int getClanCount() {
        return clansById.size();
    }

    // Топ кланов по мощи
    public List<Clan> getTopClans(int limit) {
        List<Clan> sorted = new ArrayList<>(clansById.values());
        sorted.sort(Comparator.comparingDouble(Clan::getTotalPower).reversed());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    // Обновить онлайн статус участника
    public void updateMemberOnline(Player player, boolean online) {
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) return;

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member != null) {
            member.setOnline(online);
            member.setPlayerName(player.getName()); // обновляем имя на случай смены ника
            plugin.getStorageManager().saveClanMember(clan.getClanId(), member);
        }
    }

    // ==================== РЕЗУЛЬТАТЫ ОПЕРАЦИЙ ====================

    public enum CreateResult {
        SUCCESS, ALREADY_IN_CLAN, INVALID_NAME, INVALID_TAG,
        NAME_TAKEN, TAG_TAKEN, NOT_ENOUGH_MONEY
    }

    public enum DeleteResult {
        SUCCESS, NOT_IN_CLAN, CLAN_NOT_FOUND, NO_PERMISSION
    }

    public enum InviteResult {
        SUCCESS, NOT_IN_CLAN, CLAN_NOT_FOUND, NO_PERMISSION,
        TARGET_IN_CLAN, CLAN_FULL
    }

    public enum JoinResult {
        SUCCESS, NO_INVITE, CLAN_NOT_FOUND, INVITE_EXPIRED,
        ALREADY_IN_CLAN, CLAN_FULL
    }

    public enum DenyResult {
        SUCCESS, NO_INVITE
    }

    public enum LeaveResult {
        SUCCESS, NOT_IN_CLAN, CLAN_NOT_FOUND, IS_LEADER
    }

    public enum KickResult {
        SUCCESS, NOT_IN_CLAN, CLAN_NOT_FOUND, NO_PERMISSION,
        TARGET_NOT_IN_CLAN, CANNOT_KICK_HIGHER
    }

    public enum RoleResult {
        SUCCESS, NOT_IN_CLAN, CLAN_NOT_FOUND, NO_PERMISSION,
        TARGET_NOT_IN_CLAN, CANNOT_ASSIGN_HIGHER, CANNOT_MANAGE_TARGET, NOT_LEADER
    }
}