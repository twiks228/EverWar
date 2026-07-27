package com.ever.war.config;

import com.ever.war.EverWar;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final EverWar plugin;
    private FileConfiguration config;

    // Clan settings
    private int minNameLength;
    private int maxNameLength;
    private int tagLength;
    private int maxMembers;
    private double createCost;

    // Territory settings
    private int chunksPerPlayer;
    private int maxChunks;
    private double claimCost;

    // War settings
    private int preparationTime;
    private int warDuration;
    private int minPlayersForWar;
    private int killPoints;
    private int capturePoints;

    // Siege settings
    private int siegeCaptureTime;
    private int siegeRadius;
    private double siegeCost;

    // Supply settings
    private int foodPerWar;
    private int materialsPerWar;

    // Base settings
    private double coreHp;
    private double upgradeBaseCost;

    // Power settings
    private double startingPower;
    private double killPower;
    private double deathPower;
    private double maxPower;

    // Language
    private String language;

    // Messages broadcast
    private boolean broadcastWar;
    private boolean broadcastSiege;
    private boolean broadcastAlliance;

    // Protection
    private boolean friendlyFire;
    private boolean explosionProtection;

    public ConfigManager(EverWar plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        language = config.getString("language", "ru");

        // Clan
        minNameLength = config.getInt("clan.min-name-length", 3);
        maxNameLength = config.getInt("clan.max-name-length", 24);
        tagLength = config.getInt("clan.tag-length", 4);
        maxMembers = config.getInt("clan.max-members", 50);
        createCost = config.getDouble("clan.create-cost", 10000.0);

        // Territory
        chunksPerPlayer = config.getInt("territory.chunks-per-player", 3);
        maxChunks = config.getInt("territory.max-chunks", 150);
        claimCost = config.getDouble("territory.claim-cost", 500.0);

        // War
        preparationTime = config.getInt("war.preparation-time", 600);
        warDuration = config.getInt("war.duration", 0);
        minPlayersForWar = config.getInt("war.min-players", 3);
        killPoints = config.getInt("war.kill-points", 10);
        capturePoints = config.getInt("war.capture-points", 50);

        // Siege
        siegeCaptureTime = config.getInt("siege.capture-time", 300);
        siegeRadius = config.getInt("siege.radius", 10);
        siegeCost = config.getDouble("siege.cost", 2000.0);

        // Supply
        foodPerWar = config.getInt("supply.food-per-war", 100);
        materialsPerWar = config.getInt("supply.materials-per-war", 50);

        // Base
        coreHp = config.getDouble("base.core-hp", 1000.0);
        upgradeBaseCost = config.getDouble("base.upgrade-cost", 5000.0);

        // Power
        startingPower = config.getDouble("power.starting-power", 100.0);
        killPower = config.getDouble("power.kill-power", 5.0);
        deathPower = config.getDouble("power.death-power", 3.0);
        maxPower = config.getDouble("power.max-power", 10000.0);

        // Messages
        broadcastWar = config.getBoolean("messages.broadcast-war", true);
        broadcastSiege = config.getBoolean("messages.broadcast-siege", true);
        broadcastAlliance = config.getBoolean("messages.broadcast-alliance", true);

        // Protection
        friendlyFire = config.getBoolean("protection.friendly-fire", false);
        explosionProtection = config.getBoolean("protection.explosion-protection", true);
    }

    // ==================== GETTERS ====================

    public String getLanguage() { return language; }
    public int getMinNameLength() { return minNameLength; }
    public int getMaxNameLength() { return maxNameLength; }
    public int getTagLength() { return tagLength; }
    public int getMaxMembers() { return maxMembers; }
    public double getCreateCost() { return createCost; }
    public int getChunksPerPlayer() { return chunksPerPlayer; }
    public int getMaxChunks() { return maxChunks; }
    public double getClaimCost() { return claimCost; }
    public int getPreparationTime() { return preparationTime; }
    public int getWarDuration() { return warDuration; }
    public int getMinPlayersForWar() { return minPlayersForWar; }
    public int getKillPoints() { return killPoints; }
    public int getCapturePoints() { return capturePoints; }
    public int getSiegeCaptureTime() { return siegeCaptureTime; }
    public int getSiegeRadius() { return siegeRadius; }
    public double getSiegeCost() { return siegeCost; }
    public int getFoodPerWar() { return foodPerWar; }
    public int getMaterialsPerWar() { return materialsPerWar; }
    public double getCoreHp() { return coreHp; }
    public double getUpgradeBaseCost() { return upgradeBaseCost; }
    public double getStartingPower() { return startingPower; }
    public double getKillPower() { return killPower; }
    public double getDeathPower() { return deathPower; }
    public double getMaxPower() { return maxPower; }
    public boolean isBroadcastWar() { return broadcastWar; }
    public boolean isBroadcastSiege() { return broadcastSiege; }
    public boolean isBroadcastAlliance() { return broadcastAlliance; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public boolean isExplosionProtection() { return explosionProtection; }
}