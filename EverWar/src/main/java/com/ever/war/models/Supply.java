package com.ever.war.models;

import java.time.Instant;
import java.util.UUID;

public class Supply {

    private final UUID clanId;
    private int food;
    private int materials;
    private int fuel;
    private long lastUpdated;

    public Supply(UUID clanId) {
        this.clanId = clanId;
        this.food = 0;
        this.materials = 0;
        this.fuel = 0;
        this.lastUpdated = Instant.now().getEpochSecond();
    }

    // Конструктор из БД
    public Supply(UUID clanId, int food, int materials, int fuel, long lastUpdated) {
        this.clanId = clanId;
        this.food = food;
        this.materials = materials;
        this.fuel = fuel;
        this.lastUpdated = lastUpdated;
    }

    // ==================== МЕТОДЫ ====================

    public boolean hasEnough(int requiredFood, int requiredMaterials, int requiredFuel) {
        return food >= requiredFood
                && materials >= requiredMaterials
                && fuel >= requiredFuel;
    }

    public boolean consume(int consumeFood, int consumeMaterials, int consumeFuel) {
        if (!hasEnough(consumeFood, consumeMaterials, consumeFuel)) {
            return false;
        }
        food -= consumeFood;
        materials -= consumeMaterials;
        fuel -= consumeFuel;
        touch();
        return true;
    }

    public void addFood(int amount) {
        food = Math.max(0, food + amount);
        touch();
    }

    public void addMaterials(int amount) {
        materials = Math.max(0, materials + amount);
        touch();
    }

    public void addFuel(int amount) {
        fuel = Math.max(0, fuel + amount);
        touch();
    }

    public void removeFood(int amount) {
        food = Math.max(0, food - amount);
        touch();
    }

    public void removeMaterials(int amount) {
        materials = Math.max(0, materials - amount);
        touch();
    }

    public void removeFuel(int amount) {
        fuel = Math.max(0, fuel - amount);
        touch();
    }

    private void touch() {
        lastUpdated = Instant.now().getEpochSecond();
    }

    // Дисплей состояния склада
    public String getStatusDisplay(String lang) {
        if (lang.equalsIgnoreCase("en")) {
            return "&7Food: &f" + food
                    + " &7| Materials: &f" + materials
                    + " &7| Fuel: &f" + fuel;
        } else {
            return "&7Еда: &f" + food
                    + " &7| Материалы: &f" + materials
                    + " &7| Топливо: &f" + fuel;
        }
    }

    // Достаточно ли для войны
    public boolean hasEnoughForWar(int requiredFood, int requiredMaterials) {
        return food >= requiredFood && materials >= requiredMaterials;
    }

    // ==================== GETTERS / SETTERS ====================

    public UUID getClanId() { return clanId; }
    public int getFood() { return food; }
    public int getMaterials() { return materials; }
    public int getFuel() { return fuel; }
    public long getLastUpdated() { return lastUpdated; }

    public void setFood(int food) { this.food = Math.max(0, food); touch(); }
    public void setMaterials(int materials) { this.materials = Math.max(0, materials); touch(); }
    public void setFuel(int fuel) { this.fuel = Math.max(0, fuel); touch(); }

    @Override
    public String toString() {
        return "Supply{clan=" + clanId + ", food=" + food
                + ", materials=" + materials + ", fuel=" + fuel + "}";
    }
}