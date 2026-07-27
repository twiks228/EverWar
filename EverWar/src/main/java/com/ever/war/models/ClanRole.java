package com.ever.war.models;

public enum ClanRole {

    LEADER("Лидер", "Leader", "LEADER", 5, "⭐"),
    GENERAL("Генерал", "General", "GENERAL", 4, "🎖"),
    OFFICER("Офицер", "Officer", "OFFICER", 3, "🔱"),
    FIGHTER("Боец", "Fighter", "FIGHTER", 2, "⚔"),
    RECRUIT("Рекрут", "Recruit", "RECRUIT", 1, "🔰");

    private final String nameRu;
    private final String nameEn;
    private final String id;
    private final int level;
    private final String icon;

    ClanRole(String nameRu, String nameEn, String id, int level, String icon) {
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.id = id;
        this.level = level;
        this.icon = icon;
    }

    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getId() { return id; }
    public int getLevel() { return level; }
    public String getIcon() { return icon; }

    // Получить название по языку
    public String getName(String lang) {
        return lang.equalsIgnoreCase("en") ? nameEn : nameRu;
    }

    // Может ли эта роль управлять другой ролью
    public boolean canManage(ClanRole other) {
        return this.level > other.level;
    }

    // Может ли приглашать
    public boolean canInvite() {
        return this.level >= OFFICER.level;
    }

    // Может ли кикать
    public boolean canKick() {
        return this.level >= GENERAL.level;
    }

    // Может ли захватывать территорию
    public boolean canClaimTerritory() {
        return this.level >= OFFICER.level;
    }

    // Может ли объявлять войну
    public boolean canDeclareWar() {
        return this.level >= GENERAL.level;
    }

    // Может ли управлять дипломатией
    public boolean canManageDiplomacy() {
        return this.level >= GENERAL.level;
    }

    // Может ли управлять настройками клана
    public boolean canManageSettings() {
        return this.level >= LEADER.level;
    }

    // Может ли назначать роли
    public boolean canAssignRoles() {
        return this.level >= OFFICER.level;
    }

    // Может ли управлять складом
    public boolean canManageSupply() {
        return this.level >= OFFICER.level;
    }

    // Найти роль по id строке
    public static ClanRole fromString(String s) {
        if (s == null) return RECRUIT;
        for (ClanRole role : values()) {
            if (role.id.equalsIgnoreCase(s)
                    || role.nameRu.equalsIgnoreCase(s)
                    || role.nameEn.equalsIgnoreCase(s)) {
                return role;
            }
        }
        return RECRUIT;
    }

    // Цвет для чата
    public String getChatColor() {
        return switch (this) {
            case LEADER  -> "&6";
            case GENERAL -> "&c";
            case OFFICER -> "&e";
            case FIGHTER -> "&a";
            case RECRUIT -> "&7";
        };
    }

    // Следующая роль (для повышения)
    public ClanRole getNext() {
        return switch (this) {
            case RECRUIT -> FIGHTER;
            case FIGHTER -> OFFICER;
            case OFFICER -> GENERAL;
            case GENERAL -> LEADER;
            case LEADER  -> LEADER;
        };
    }

    // Предыдущая роль (для понижения)
    public ClanRole getPrevious() {
        return switch (this) {
            case LEADER  -> GENERAL;
            case GENERAL -> OFFICER;
            case OFFICER -> FIGHTER;
            case FIGHTER -> RECRUIT;
            case RECRUIT -> RECRUIT;
        };
    }
}