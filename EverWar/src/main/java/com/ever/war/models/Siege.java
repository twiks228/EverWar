package com.ever.war.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Siege {

    public enum SiegeStatus {
        ACTIVE,
        SUCCESS,
        FAILED
    }

    private final UUID siegeId;
    private final UUID attackerClanId;
    private final UUID defenderClanId;
    private final String chunkKey;
    private final String worldName;
    private final double siegeX;
    private final double siegeY;
    private final double siegeZ;
    private final long startedAt;
    private final int captureTime;
    private double progress;
    private SiegeStatus status;
    private long lastTickTime;

    // Лог событий осады
    private final List<String> eventLog = new ArrayList<>();

    // Конструктор новой осады
    public Siege(UUID siegeId,
                 UUID attackerClanId,
                 UUID defenderClanId,
                 String chunkKey,
                 String worldName,
                 double siegeX,
                 double siegeY,
                 double siegeZ,
                 int captureTime) {
        this.siegeId = siegeId;
        this.attackerClanId = attackerClanId;
        this.defenderClanId = defenderClanId;
        this.chunkKey = chunkKey;
        this.worldName = worldName;
        this.siegeX = siegeX;
        this.siegeY = siegeY;
        this.siegeZ = siegeZ;
        this.startedAt = Instant.now().getEpochSecond();
        this.captureTime = captureTime;
        this.progress = 0.0;
        this.status = SiegeStatus.ACTIVE;
        this.lastTickTime = this.startedAt;
    }

    // Конструктор загрузки из БД
    public Siege(UUID siegeId,
                 UUID attackerClanId,
                 UUID defenderClanId,
                 String chunkKey,
                 String worldName,
                 double siegeX,
                 double siegeY,
                 double siegeZ,
                 long startedAt,
                 int captureTime,
                 double progress) {
        this.siegeId = siegeId;
        this.attackerClanId = attackerClanId;
        this.defenderClanId = defenderClanId;
        this.chunkKey = chunkKey;
        this.worldName = worldName;
        this.siegeX = siegeX;
        this.siegeY = siegeY;
        this.siegeZ = siegeZ;
        this.startedAt = startedAt;
        this.captureTime = captureTime;
        this.progress = progress;
        this.status = SiegeStatus.ACTIVE;
        this.lastTickTime = Instant.now().getEpochSecond();
    }

    // ==================== МЕТОДЫ ====================

    /**
     * Добавить событие в лог осады
     */
    public void addEvent(String event) {
        String time = "[" + Instant.now().toString() + "] ";
        eventLog.add(time + event);
        if (eventLog.size() > 50) {
            eventLog.remove(0);
        }
    }

    /**
     * Тикнуть прогресс осады
     * Возвращает true если осада завершилась
     */
    public boolean tick(int attackersInZone, int defendersInZone) {
        if (status != SiegeStatus.ACTIVE) return true;

        long now = Instant.now().getEpochSecond();
        double delta = now - lastTickTime;
        lastTickTime = now;

        if (attackersInZone > 0 && defendersInZone == 0) {
            double gain = (delta / captureTime) * 100.0;
            progress = Math.min(100.0, progress + gain);
        } else if (defendersInZone > 0 && attackersInZone == 0) {
            double loss = (delta / captureTime) * 100.0;
            progress = Math.max(0.0, progress - loss);
        }

        if (progress >= 100.0) {
            status = SiegeStatus.SUCCESS;
            return true;
        }

        return false;
    }

    public void fail() {
        this.status = SiegeStatus.FAILED;
        addEvent("Siege failed");
    }

    public void succeed() {
        this.status = SiegeStatus.SUCCESS;
        this.progress = 100.0;
        addEvent("Siege succeeded");
    }

    public boolean isActive() {
        return status == SiegeStatus.ACTIVE;
    }

    public String getProgressBar() {
        int filled = (int) (progress / 10);
        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < 10; i++) {
            if (i == filled) bar.append("&7");
            bar.append("█");
        }
        return bar + " &f" + String.format("%.1f", progress) + "%";
    }

    public long getElapsedSeconds() {
        return Instant.now().getEpochSecond() - startedAt;
    }

    public long getRemainingSeconds() {
        double remaining = (100.0 - progress) / 100.0 * captureTime;
        return Math.max(0, (long) remaining);
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getSiegeId() { return siegeId; }
    public UUID getAttackerClanId() { return attackerClanId; }
    public UUID getDefenderClanId() { return defenderClanId; }
    public String getChunkKey() { return chunkKey; }
    public String getWorldName() { return worldName; }
    public double getSiegeX() { return siegeX; }
    public double getSiegeY() { return siegeY; }
    public double getSiegeZ() { return siegeZ; }
    public long getStartedAt() { return startedAt; }
    public int getCaptureTime() { return captureTime; }
    public double getProgress() { return progress; }
    public SiegeStatus getStatus() { return status; }
    public List<String> getEventLog() { return eventLog; }

    public void setProgress(double progress) { this.progress = progress; }
    public void setStatus(SiegeStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Siege other)) return false;
        return siegeId.equals(other.siegeId);
    }

    @Override
    public int hashCode() {
        return siegeId.hashCode();
    }

    @Override
    public String toString() {
        return "Siege{id=" + siegeId + ", chunk=" + chunkKey
                + ", progress=" + String.format("%.1f", progress) + "%}";
    }
}