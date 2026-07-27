package com.ever.war.models;

import java.time.Instant;
import java.util.UUID;

public class Alliance {

    public enum Relation {
        ALLY,    // Союзники
        ENEMY,   // Враги
        NEUTRAL  // Нейтральные
    }

    private final UUID clanA;
    private final UUID clanB;
    private Relation relation;
    private final long createdAt;

    // Для предложений союза
    private boolean pendingAlly;   // ожидает подтверждения союза
    private UUID proposedBy;       // кто предложил

    public Alliance(UUID clanA, UUID clanB, Relation relation, long createdAt) {
        this.clanA = clanA;
        this.clanB = clanB;
        this.relation = relation;
        this.createdAt = createdAt;
        this.pendingAlly = false;
        this.proposedBy = null;
    }

    // Новый альянс
    public Alliance(UUID clanA, UUID clanB, Relation relation) {
        this(clanA, clanB, relation, Instant.now().getEpochSecond());
    }

    // ==================== МЕТОДЫ ====================

    public boolean involves(UUID clanId) {
        return clanA.equals(clanId) || clanB.equals(clanId);
    }

    public UUID getOpponent(UUID clanId) {
        if (clanA.equals(clanId)) return clanB;
        if (clanB.equals(clanId)) return clanA;
        return null;
    }

    public boolean isAlly() {
        return relation == Relation.ALLY;
    }

    public boolean isEnemy() {
        return relation == Relation.ENEMY;
    }

    public boolean isNeutral() {
        return relation == Relation.NEUTRAL;
    }

    // Предложить союз
    public void proposeAlly(UUID proposedBy) {
        this.pendingAlly = true;
        this.proposedBy = proposedBy;
    }

    // Принять союз
    public void acceptAlly() {
        this.relation = Relation.ALLY;
        this.pendingAlly = false;
        this.proposedBy = null;
    }

    // Отклонить предложение
    public void rejectAlly() {
        this.pendingAlly = false;
        this.proposedBy = null;
    }

    // Установить отношение
    public void setRelation(Relation relation) {
        this.relation = relation;
        this.pendingAlly = false;
    }

    // Получить ключ для хранения (всегда в одном порядке)
    public static String makeKey(UUID a, UUID b) {
        // Меньший UUID первым — чтобы (A,B) == (B,A)
        if (a.compareTo(b) < 0) {
            return a + ":" + b;
        } else {
            return b + ":" + a;
        }
    }

    public String getKey() {
        return makeKey(clanA, clanB);
    }

    // Отображение отношения на нужном языке
    public String getRelationDisplay(String lang) {
        return switch (relation) {
            case ALLY    -> lang.equalsIgnoreCase("en") ? "&a🤝 Ally"   : "&a🤝 Союзник";
            case ENEMY   -> lang.equalsIgnoreCase("en") ? "&c⚔ Enemy"  : "&c⚔ Враг";
            case NEUTRAL -> lang.equalsIgnoreCase("en") ? "&7● Neutral" : "&7● Нейтрал";
        };
    }

    // Цвет для карты
    public String getMapColor() {
        return switch (relation) {
            case ALLY    -> "&b";
            case ENEMY   -> "&c";
            case NEUTRAL -> "&7";
        };
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getClanA() { return clanA; }
    public UUID getClanB() { return clanB; }
    public Relation getRelation() { return relation; }
    public long getCreatedAt() { return createdAt; }
    public boolean isPendingAlly() { return pendingAlly; }
    public UUID getProposedBy() { return proposedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alliance other)) return false;
        return getKey().equals(other.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        return "Alliance{" + clanA + " <-> " + clanB + " : " + relation + "}";
    }
}