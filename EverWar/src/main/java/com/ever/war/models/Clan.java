package com.ever.war.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Clan {

    private final UUID clanId;
    private String name;
    private String tag;
    private UUID leaderUUID;
    private String description;
    private String color;
    private long createdAt;
    private boolean open;

    // Участники
    private final Map<UUID, ClanMember> members;
    private final Map<UUID, Long> pendingInvites;

    // Настройки
    private boolean friendlyFire;
    private boolean publicInfo;
    private boolean allowAttackAllies;  // Атака союзников
    private boolean deserter;           // Клан-дезертир (против всех)
    private long deserterUntil;         // До какого времени дезертир

    // Статистика
    private int totalKills;
    private int totalDeaths;
    private int warsWon;
    private int warsLost;
    private int territoriesCaptured;
    private double totalPower;

    // Конструктор нового клана
    public Clan(UUID clanId, String name, String tag, UUID leaderUUID, String leaderName) {
        this.clanId = clanId;
        this.name = name;
        this.tag = tag;
        this.leaderUUID = leaderUUID;
        this.description = "";
        this.color = "&6";
        this.createdAt = Instant.now().getEpochSecond();
        this.open = false;
        this.friendlyFire = false;
        this.publicInfo = true;
        this.allowAttackAllies = false;
        this.deserter = false;
        this.deserterUntil = 0;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.warsWon = 0;
        this.warsLost = 0;
        this.territoriesCaptured = 0;
        this.totalPower = 0;

        this.members = new HashMap<>();
        this.pendingInvites = new HashMap<>();

        ClanMember leader = new ClanMember(leaderUUID, leaderName, ClanRole.LEADER);
        members.put(leaderUUID, leader);
    }

    // Полный конструктор для БД
    public Clan(UUID clanId,
                String name,
                String tag,
                UUID leaderUUID,
                String description,
                String color,
                long createdAt,
                boolean open,
                boolean friendlyFire,
                boolean publicInfo,
                int totalKills,
                int totalDeaths,
                int warsWon,
                int warsLost,
                int territoriesCaptured) {
        this.clanId = clanId;
        this.name = name;
        this.tag = tag;
        this.leaderUUID = leaderUUID;
        this.description = description;
        this.color = color;
        this.createdAt = createdAt;
        this.open = open;
        this.friendlyFire = friendlyFire;
        this.publicInfo = publicInfo;
        this.allowAttackAllies = false;
        this.deserter = false;
        this.deserterUntil = 0;
        this.totalKills = totalKills;
        this.totalDeaths = totalDeaths;
        this.warsWon = warsWon;
        this.warsLost = warsLost;
        this.territoriesCaptured = territoriesCaptured;
        this.totalPower = 0;

        this.members = new HashMap<>();
        this.pendingInvites = new HashMap<>();
    }

    // ==================== УЧАСТНИКИ ====================

    public void addMember(ClanMember member) {
        members.put(member.getPlayerUUID(), member);
        pendingInvites.remove(member.getPlayerUUID());
        recalculatePower();
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        recalculatePower();
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public ClanMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public Map<UUID, ClanMember> getMembers() {
        return members;
    }

    public List<ClanMember> getMemberList() {
        return new ArrayList<>(members.values());
    }

    public int getMemberCount() {
        return members.size();
    }

    public List<ClanMember> getOnlineMembers() {
        List<ClanMember> online = new ArrayList<>();
        for (ClanMember m : members.values()) {
            if (m.isOnline()) online.add(m);
        }
        return online;
    }

    public List<UUID> getMemberUUIDs() {
        return new ArrayList<>(members.keySet());
    }

    // ==================== ПРИГЛАШЕНИЯ ====================

    public void invite(UUID uuid) {
        pendingInvites.put(uuid, Instant.now().getEpochSecond() + 300);
    }

    public boolean hasInvite(UUID uuid) {
        Long expiry = pendingInvites.get(uuid);
        if (expiry == null) return false;
        if (Instant.now().getEpochSecond() > expiry) {
            pendingInvites.remove(uuid);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    public void cleanExpiredInvites() {
        long now = Instant.now().getEpochSecond();
        pendingInvites.entrySet().removeIf(e -> e.getValue() < now);
    }

    // ==================== РОЛИ ====================

    public ClanRole getMemberRole(UUID uuid) {
        ClanMember member = members.get(uuid);
        return member != null ? member.getRole() : null;
    }

    public void setMemberRole(UUID uuid, ClanRole role) {
        ClanMember member = members.get(uuid);
        if (member != null) {
            member.setRole(role);
            if (role == ClanRole.LEADER) {
                leaderUUID = uuid;
            }
        }
    }

    public boolean isLeader(UUID uuid) {
        return leaderUUID.equals(uuid);
    }

    public List<ClanMember> getMembersByRole(ClanRole role) {
        List<ClanMember> result = new ArrayList<>();
        for (ClanMember m : members.values()) {
            if (m.getRole() == role) result.add(m);
        }
        return result;
    }

    // ==================== ДЕЗЕРТИР ====================

    /**
     * Проверяет — является ли клан дезертиром сейчас
     * Автоматически сбрасывает если время истекло
     */
    public boolean isDeserter() {
        if (deserter && deserterUntil > 0
                && Instant.now().getEpochSecond() > deserterUntil) {
            this.deserter = false;
            this.deserterUntil = 0;
        }
        return deserter;
    }

    public void setDeserter(boolean deserter, long untilTimestamp) {
        this.deserter = deserter;
        this.deserterUntil = untilTimestamp;
    }

    public long getDeserterUntil() {
        return deserterUntil;
    }

    public long getDeserterRemainingSeconds() {
        if (!isDeserter()) return 0;
        return Math.max(0, deserterUntil - Instant.now().getEpochSecond());
    }

    // ==================== АТАКА СОЮЗНИКОВ ====================

    public boolean isAllowAttackAllies() {
        return allowAttackAllies;
    }

    public void setAllowAttackAllies(boolean allowAttackAllies) {
        this.allowAttackAllies = allowAttackAllies;
    }

    // ==================== МОЩЬ ====================

    public double getTotalPower() {
        return totalPower;
    }

    public void recalculatePower() {
        double total = 0;
        for (ClanMember m : members.values()) {
            total += m.getPower();
        }
        this.totalPower = total;
    }

    // ==================== СТАТИСТИКА ====================

    public void addKill() { totalKills++; }
    public void addDeath() { totalDeaths++; }
    public void addWarWon() { warsWon++; }
    public void addWarLost() { warsLost++; }
    public void addTerritoryCaptured() { territoriesCaptured++; }

    // ==================== ДИСПЛЕЙ ====================

    public String getFormattedName() {
        return color + name;
    }

    public String getFormattedTag() {
        return color + "[" + tag + "]";
    }

    public String getInfoDisplay(String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("&8&l━━━━━━━━━━━━━━━━━━━━━━\n");
        if (lang.equalsIgnoreCase("en")) {
            sb.append("&6&l Clan: &f").append(name).append(" &7[").append(tag).append("]\n");
            sb.append("&7 Leader: &f").append(getLeaderName()).append("\n");
            sb.append("&7 Members: &f").append(getMemberCount()).append("\n");
            sb.append("&7 Power: &f").append(String.format("%.0f", totalPower)).append("\n");
            sb.append("&7 Kills: &f").append(totalKills).append(" &7| Deaths: &f").append(totalDeaths).append("\n");
            sb.append("&7 Wars W/L: &a").append(warsWon).append("&7/&c").append(warsLost).append("\n");
            if (isDeserter()) {
                sb.append("&4&l 🚨 DESERTER MODE ACTIVE!\n");
            }
        } else {
            sb.append("&6&l Клан: &f").append(name).append(" &7[").append(tag).append("]\n");
            sb.append("&7 Лидер: &f").append(getLeaderName()).append("\n");
            sb.append("&7 Участников: &f").append(getMemberCount()).append("\n");
            sb.append("&7 Мощь: &f").append(String.format("%.0f", totalPower)).append("\n");
            sb.append("&7 Убийств: &f").append(totalKills).append(" &7| Смертей: &f").append(totalDeaths).append("\n");
            sb.append("&7 Войн П/П: &a").append(warsWon).append("&7/&c").append(warsLost).append("\n");
            if (isDeserter()) {
                sb.append("&4&l 🚨 РЕЖИМ ДЕЗЕРТИРА АКТИВЕН!\n");
            }
        }
        sb.append("&8&l━━━━━━━━━━━━━━━━━━━━━━");
        return sb.toString();
    }

    private String getLeaderName() {
        ClanMember leader = members.get(leaderUUID);
        return leader != null ? leader.getPlayerName() : "Unknown";
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getClanId() { return clanId; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public UUID getLeaderUUID() { return leaderUUID; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
    public long getCreatedAt() { return createdAt; }
    public boolean isOpen() { return open; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public boolean isPublicInfo() { return publicInfo; }
    public int getTotalKills() { return totalKills; }
    public int getTotalDeaths() { return totalDeaths; }
    public int getWarsWon() { return warsWon; }
    public int getWarsLost() { return warsLost; }
    public int getTerritoriesCaptured() { return territoriesCaptured; }

    public void setName(String name) { this.name = name; }
    public void setTag(String tag) { this.tag = tag; }
    public void setLeaderUUID(UUID leaderUUID) { this.leaderUUID = leaderUUID; }
    public void setDescription(String description) { this.description = description; }
    public void setColor(String color) { this.color = color; }
    public void setOpen(boolean open) { this.open = open; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public void setPublicInfo(boolean publicInfo) { this.publicInfo = publicInfo; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }
    public void setWarsWon(int warsWon) { this.warsWon = warsWon; }
    public void setWarsLost(int warsLost) { this.warsLost = warsLost; }
    public void setTerritoriesCaptured(int territoriesCaptured) {
        this.territoriesCaptured = territoriesCaptured;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Clan other)) return false;
        return clanId.equals(other.clanId);
    }

    @Override
    public int hashCode() {
        return clanId.hashCode();
    }

    @Override
    public String toString() {
        return "Clan{id=" + clanId + ", name=" + name + ", tag=" + tag
                + ", members=" + members.size() + "}";
    }
}