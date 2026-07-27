package com.ever.war.models;

import java.time.Instant;
import java.util.UUID;

public class Territory {

    private final String chunkKey; // "world:x:z"
    private UUID ownerClanId;
    private String worldName;
    private int chunkX;
    private int chunkZ;
    private boolean isCore; // это ядро базы?
    private long claimedAt;
    private int defenseLevel; // уровень укрепления (0-5)
    private double hp; // HP территории
    private double maxHp;

    public Territory(String chunkKey,
                     UUID ownerClanId,
                     String worldName,
                     int chunkX,
                     int chunkZ) {
        this.chunkKey = chunkKey;
        this.ownerClanId = ownerClanId;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.isCore = false;
        this.claimedAt = Instant.now().getEpochSecond();
        this.defenseLevel = 0;
        this.maxHp = 1000.0;
        this.hp = maxHp;
    }

    // Полный конструктор для БД
    public Territory(String chunkKey,
                     UUID ownerClanId,
                     String worldName,
                     int chunkX,
                     int chunkZ,
                     boolean isCore,
                     long claimedAt,
                     int defenseLevel,
                     double hp,
                     double maxHp) {
        this.chunkKey = chunkKey;
        this.ownerClanId = ownerClanId;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.isCore = isCore;
        this.claimedAt = claimedAt;
        this.defenseLevel = defenseLevel;
        this.hp = hp;
        this.maxHp = maxHp;
    }

    // ==================== МЕТОДЫ ====================

    // Создать ключ из чанка
    public static String makeKey(String worldName, int chunkX, int chunkZ) {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }

    // Нанести урон территории
    public boolean damage(double amount) {
        this.hp -= amount;
        if (this.hp <= 0) {
            this.hp = 0;
            return true; // территория пала
        }
        return false;
    }

    // Починить территорию
    public void repair(double amount) {
        this.hp = Math.min(maxHp, this.hp + amount);
    }

    // Улучшить защиту
    public boolean upgrade() {
        if (defenseLevel >= 5) return false;
        defenseLevel++;
        maxHp += 500;
        hp = maxHp; // восстанавливаем при апгрейде
        return true;
    }

    // Процент HP
    public double getHpPercent() {
        return (hp / maxHp) * 100.0;
    }

    // Статус HP для дисплея
    public String getHpDisplay() {
        double percent = getHpPercent();
        if (percent > 75) return "&a" + String.format("%.0f", hp) + "/" + String.format("%.0f", maxHp);
        if (percent > 35) return "&e" + String.format("%.0f", hp) + "/" + String.format("%.0f", maxHp);
        return "&c" + String.format("%.0f", hp) + "/" + String.format("%.0f", maxHp);
    }

    // Символ на карте
    public String getMapSymbol(UUID viewerClanId, boolean isAlly, boolean isEnemy) {
        if (ownerClanId.equals(viewerClanId)) {
            return isCore ? "&a+" : "&a■";
        } else if (isAlly) {
            return "&b■";
        } else if (isEnemy) {
            return "&c■";
        } else {
            return "&7■";
        }
    }

    // ==================== GETTERS / SETTERS ====================

    public String getChunkKey() { return chunkKey; }
    public UUID getOwnerClanId() { return ownerClanId; }
    public String getWorldName() { return worldName; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean isCore() { return isCore; }
    public long getClaimedAt() { return claimedAt; }
    public int getDefenseLevel() { return defenseLevel; }
    public double getHp() { return hp; }
    public double getMaxHp() { return maxHp; }

    public void setOwnerClanId(UUID ownerClanId) { this.ownerClanId = ownerClanId; }
    public void setCore(boolean core) { isCore = core; }
    public void setHp(double hp) { this.hp = hp; }
    public void setMaxHp(double maxHp) { this.maxHp = maxHp; }
    public void setDefenseLevel(int defenseLevel) { this.defenseLevel = defenseLevel; }
    public void setClaimedAt(long claimedAt) { this.claimedAt = claimedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Territory other)) return false;
        return chunkKey.equals(other.chunkKey);
    }

    @Override
    public int hashCode() {
        return chunkKey.hashCode();
    }

    @Override
    public String toString() {
        return "Territory{key=" + chunkKey + ", owner=" + ownerClanId +
                ", core=" + isCore + ", hp=" + hp + "}";
    }
}