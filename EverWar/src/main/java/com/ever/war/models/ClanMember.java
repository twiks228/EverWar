package com.ever.war.models;

import java.time.Instant;
import java.util.UUID;

public class ClanMember {

    private final UUID playerUUID;
    private String playerName;
    private ClanRole role;
    private long joinedAt;
    private long lastSeen;
    private int kills;
    private int deaths;
    private double power;
    private boolean online;

    public ClanMember(UUID playerUUID, String playerName, ClanRole role) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.role = role;
        this.joinedAt = Instant.now().getEpochSecond();
        this.lastSeen = Instant.now().getEpochSecond();
        this.kills = 0;
        this.deaths = 0;
        this.power = 100.0;
        this.online = false;
    }

    // Полный конструктор для загрузки из БД
    public ClanMember(UUID playerUUID,
                      String playerName,
                      ClanRole role,
                      long joinedAt,
                      long lastSeen,
                      int kills,
                      int deaths,
                      double power) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.role = role;
        this.joinedAt = joinedAt;
        this.lastSeen = lastSeen;
        this.kills = kills;
        this.deaths = deaths;
        this.power = power;
        this.online = false;
    }

    // ==================== GETTERS ====================

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public ClanRole getRole() { return role; }
    public long getJoinedAt() { return joinedAt; }
    public long getLastSeen() { return lastSeen; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public double getPower() { return power; }
    public boolean isOnline() { return online; }

    // ==================== SETTERS ====================

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setRole(ClanRole role) {
        this.role = role;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public void setOnline(boolean online) {
        this.online = online;
        if (online) {
            this.lastSeen = Instant.now().getEpochSecond();
        }
    }

    // ==================== МЕТОДЫ ====================

    public void addKill() {
        this.kills++;
        this.power += 5.0;
    }

    public void addDeath() {
        this.deaths++;
        this.power = Math.max(0, this.power - 3.0);
    }

    public void addPower(double amount) {
        this.power += amount;
    }

    public void removePower(double amount) {
        this.power = Math.max(0, this.power - amount);
    }

    public double getKillDeathRatio() {
        if (deaths == 0) return kills;
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    // Отображаемый статус для GUI
    public String getStatusDisplay(String lang) {
        if (online) {
            return lang.equalsIgnoreCase("en") ? "&a● Online" : "&a● Онлайн";
        } else {
            return lang.equalsIgnoreCase("en") ? "&7● Offline" : "&7● Оффлайн";
        }
    }

    // Отображение роли с цветом
    public String getRoleDisplay(String lang) {
        return role.getChatColor() + role.getName(lang);
    }

    @Override
    public String toString() {
        return "ClanMember{" +
                "uuid=" + playerUUID +
                ", name=" + playerName +
                ", role=" + role +
                ", power=" + power +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClanMember other)) return false;
        return playerUUID.equals(other.playerUUID);
    }

    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
}