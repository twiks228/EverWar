package com.ever.war.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class War {

    public enum WarStatus {
        PREPARATION,  // Подготовка
        ACTIVE,       // Активная война
        ENDED         // Завершена
    }

    private final UUID warId;
    private final UUID attackerClanId;
    private final UUID defenderClanId;
    private WarStatus status;
    private long declaredAt;
    private long startedAt;
    private long endedAt;
    private int attackerScore;
    private int defenderScore;
    private UUID winnerClanId;
    private final List<String> eventLog;
    private long preparationEndTime;

    // Конструктор новой войны
    public War(UUID warId, UUID attackerClanId, UUID defenderClanId, int prepSeconds) {
        this.warId = warId;
        this.attackerClanId = attackerClanId;
        this.defenderClanId = defenderClanId;
        this.status = WarStatus.PREPARATION;
        this.declaredAt = Instant.now().getEpochSecond();
        this.startedAt = 0;
        this.endedAt = 0;
        this.attackerScore = 0;
        this.defenderScore = 0;
        this.winnerClanId = null;
        this.eventLog = new ArrayList<>();
        this.preparationEndTime = declaredAt + prepSeconds;
    }

    // Конструктор из БД
    public War(UUID warId,
               UUID attackerClanId,
               UUID defenderClanId,
               WarStatus status,
               long declaredAt,
               long startedAt,
               long endedAt,
               int attackerScore,
               int defenderScore,
               UUID winnerClanId,
               long preparationEndTime) {
        this.warId = warId;
        this.attackerClanId = attackerClanId;
        this.defenderClanId = defenderClanId;
        this.status = status;
        this.declaredAt = declaredAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.attackerScore = attackerScore;
        this.defenderScore = defenderScore;
        this.winnerClanId = winnerClanId;
        this.eventLog = new ArrayList<>();
        this.preparationEndTime = preparationEndTime;
    }

    // ==================== МЕТОДЫ ====================

    public void start() {
        this.status = WarStatus.ACTIVE;
        this.startedAt = Instant.now().getEpochSecond();
        addEvent("War started!");
    }

    public void end(UUID winnerClanId) {
        this.status = WarStatus.ENDED;
        this.endedAt = Instant.now().getEpochSecond();
        this.winnerClanId = winnerClanId;
        addEvent("War ended. Winner: " + winnerClanId);
    }

    // Добавить очки атакующим
    public void addAttackerScore(int points) {
        this.attackerScore += points;
    }

    // Добавить очки защитникам
    public void addDefenderScore(int points) {
        this.defenderScore += points;
    }

    // Добавить событие в лог
    public void addEvent(String event) {
        String time = "[" + Instant.now().toString() + "] ";
        eventLog.add(time + event);
        // Держим лог не больше 100 записей
        if (eventLog.size() > 100) {
            eventLog.remove(0);
        }
    }

    // Проверить — закончилось ли время подготовки
    public boolean isPreparationOver() {
        return Instant.now().getEpochSecond() >= preparationEndTime;
    }

    // Секунды до начала войны
    public long getSecondsUntilStart() {
        long remaining = preparationEndTime - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    // Это атакующий клан?
    public boolean isAttacker(UUID clanId) {
        return attackerClanId.equals(clanId);
    }

    // Это защитник?
    public boolean isDefender(UUID clanId) {
        return defenderClanId.equals(clanId);
    }

    // Участвует ли клан в войне?
    public boolean involves(UUID clanId) {
        return attackerClanId.equals(clanId) || defenderClanId.equals(clanId);
    }

    // Получить противника
    public UUID getOpponent(UUID clanId) {
        if (attackerClanId.equals(clanId)) return defenderClanId;
        if (defenderClanId.equals(clanId)) return attackerClanId;
        return null;
    }

    // Лидирует ли атакующий?
    public boolean isAttackerLeading() {
        return attackerScore >= defenderScore;
    }

    // Определить победителя по очкам
    public UUID determineWinner() {
        if (attackerScore > defenderScore) return attackerClanId;
        if (defenderScore > attackerScore) return defenderClanId;
        return null; // ничья
    }

    // Дисплей счёта
    public String getScoreDisplay(String attackerName, String defenderName) {
        return "&f" + attackerName + " &7" + attackerScore
                + " &8: &7" + defenderScore + " &f" + defenderName;
    }

    // Статус на нужном языке
    public String getStatusDisplay(String lang) {
        return switch (status) {
            case PREPARATION -> lang.equalsIgnoreCase("en") ? "&ePreparation" : "&eПодготовка";
            case ACTIVE      -> lang.equalsIgnoreCase("en") ? "&cActive"      : "&cАктивна";
            case ENDED       -> lang.equalsIgnoreCase("en") ? "&7Ended"       : "&7Завершена";
        };
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getWarId() { return warId; }
    public UUID getAttackerClanId() { return attackerClanId; }
    public UUID getDefenderClanId() { return defenderClanId; }
    public WarStatus getStatus() { return status; }
    public long getDeclaredAt() { return declaredAt; }
    public long getStartedAt() { return startedAt; }
    public long getEndedAt() { return endedAt; }
    public int getAttackerScore() { return attackerScore; }
    public int getDefenderScore() { return defenderScore; }
    public UUID getWinnerClanId() { return winnerClanId; }
    public List<String> getEventLog() { return eventLog; }
    public long getPreparationEndTime() { return preparationEndTime; }

    public void setStatus(WarStatus status) { this.status = status; }
    public void setAttackerScore(int score) { this.attackerScore = score; }
    public void setDefenderScore(int score) { this.defenderScore = score; }
    public void setWinnerClanId(UUID uuid) { this.winnerClanId = uuid; }
    public void setStartedAt(long t) { this.startedAt = t; }
    public void setEndedAt(long t) { this.endedAt = t; }
    public void setPreparationEndTime(long t) { this.preparationEndTime = t; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof War other)) return false;
        return warId.equals(other.warId);
    }

    @Override
    public int hashCode() {
        return warId.hashCode();
    }

    @Override
    public String toString() {
        return "War{id=" + warId + ", attacker=" + attackerClanId
                + ", defender=" + defenderClanId + ", status=" + status + "}";
    }
}