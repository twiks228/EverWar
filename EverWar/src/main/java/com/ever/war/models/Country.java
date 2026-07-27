package com.ever.war.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Country {

    private final UUID countryId;
    private String name;
    private String tag;
    private UUID leaderClanId;
    private String description;
    private String color;
    private final long createdAt;
    private String capitalChunkKey;

    // Кланы в составе страны
    private final List<UUID> clanIds;

    // Приглашения кланов (clanId -> время истечения)
    private final Map<UUID, Long> pendingInvites;

    // Статистика
    private int totalWarsWon;
    private int totalWarsLost;

    // Конструктор создания новой страны
    public Country(UUID countryId, String name, String tag, UUID leaderClanId, String leaderClanName) {
        this.countryId = countryId;
        this.name = name;
        this.tag = tag;
        this.leaderClanId = leaderClanId;
        this.description = "";
        this.color = "&b";
        this.createdAt = Instant.now().getEpochSecond();
        this.capitalChunkKey = null;
        this.clanIds = new ArrayList<>();
        this.pendingInvites = new HashMap<>();
        this.totalWarsWon = 0;
        this.totalWarsLost = 0;

        // Лидер-клан автоматически в стране
        clanIds.add(leaderClanId);
    }

    // Конструктор загрузки из БД
    public Country(UUID countryId,
                   String name,
                   String tag,
                   UUID leaderClanId,
                   String description,
                   String color,
                   long createdAt,
                   String capitalChunkKey) {
        this.countryId = countryId;
        this.name = name;
        this.tag = tag;
        this.leaderClanId = leaderClanId;
        this.description = description;
        this.color = color;
        this.createdAt = createdAt;
        this.capitalChunkKey = capitalChunkKey;
        this.clanIds = new ArrayList<>();
        this.pendingInvites = new HashMap<>();
        this.totalWarsWon = 0;
        this.totalWarsLost = 0;
    }

    // ==================== КЛАНЫ В СТРАНЕ ====================

    public void addClan(UUID clanId) {
        if (!clanIds.contains(clanId)) {
            clanIds.add(clanId);
            pendingInvites.remove(clanId);
        }
    }

    public void removeClan(UUID clanId) {
        clanIds.remove(clanId);
    }

    public boolean hasClan(UUID clanId) {
        return clanIds.contains(clanId);
    }

    public List<UUID> getClanIds() {
        return new ArrayList<>(clanIds);
    }

    public int getClanCount() {
        return clanIds.size();
    }

    public boolean isLeaderClan(UUID clanId) {
        return leaderClanId.equals(clanId);
    }

    // ==================== ПРИГЛАШЕНИЯ ====================

    public void invite(UUID clanId) {
        pendingInvites.put(clanId, Instant.now().getEpochSecond() + 600);
    }

    public boolean hasInvite(UUID clanId) {
        Long expiry = pendingInvites.get(clanId);
        if (expiry == null) return false;
        if (Instant.now().getEpochSecond() > expiry) {
            pendingInvites.remove(clanId);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID clanId) {
        pendingInvites.remove(clanId);
    }

    public void cleanExpiredInvites() {
        long now = Instant.now().getEpochSecond();
        pendingInvites.entrySet().removeIf(e -> e.getValue() < now);
    }

    // ==================== СТАТИСТИКА ====================

    public void addWarWon() { totalWarsWon++; }
    public void addWarLost() { totalWarsLost++; }

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
            sb.append("&b&l Country: &f").append(name).append(" &7[").append(tag).append("]\n");
            sb.append("&7 Leader Clan: &f").append(leaderClanId).append("\n");
            sb.append("&7 Clans: &f").append(getClanCount()).append("\n");
            sb.append("&7 Wars W/L: &a").append(totalWarsWon).append("&7/&c").append(totalWarsLost).append("\n");
        } else {
            sb.append("&b&l Страна: &f").append(name).append(" &7[").append(tag).append("]\n");
            sb.append("&7 Лидер-клан: &f").append(leaderClanId).append("\n");
            sb.append("&7 Кланов: &f").append(getClanCount()).append("\n");
            sb.append("&7 Войн П/П: &a").append(totalWarsWon).append("&7/&c").append(totalWarsLost).append("\n");
        }
        sb.append("&8&l━━━━━━━━━━━━━━━━━━━━━━");
        return sb.toString();
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getCountryId() { return countryId; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public UUID getLeaderClanId() { return leaderClanId; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
    public long getCreatedAt() { return createdAt; }
    public String getCapitalChunkKey() { return capitalChunkKey; }
    public int getTotalWarsWon() { return totalWarsWon; }
    public int getTotalWarsLost() { return totalWarsLost; }

    public void setName(String name) { this.name = name; }
    public void setTag(String tag) { this.tag = tag; }
    public void setLeaderClanId(UUID leaderClanId) { this.leaderClanId = leaderClanId; }
    public void setDescription(String description) { this.description = description; }
    public void setColor(String color) { this.color = color; }
    public void setCapitalChunkKey(String capitalChunkKey) { this.capitalChunkKey = capitalChunkKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country other)) return false;
        return countryId.equals(other.countryId);
    }

    @Override
    public int hashCode() {
        return countryId.hashCode();
    }

    @Override
    public String toString() {
        return "Country{id=" + countryId + ", name=" + name
                + ", tag=" + tag + ", clans=" + clanIds.size() + "}";
    }
}