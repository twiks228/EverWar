package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Supply;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SupplyManager {

    private final EverWar plugin;

    // clanId -> Supply
    private final Map<UUID, Supply> supplies = new HashMap<>();

    public SupplyManager(EverWar plugin) {
        this.plugin = plugin;
    }

    public void addSupplyToCache(Supply supply) {
        supplies.put(supply.getClanId(), supply);
    }

    public void createSupplyForClan(UUID clanId) {
        if (!supplies.containsKey(clanId)) {
            Supply supply = new Supply(clanId);
            supplies.put(clanId, supply);
            plugin.getStorageManager().saveSupply(supply);
        }
    }

    public Supply getSupply(UUID clanId) {
        return supplies.computeIfAbsent(clanId, id -> {
            Supply s = new Supply(id);
            plugin.getStorageManager().saveSupply(s);
            return s;
        });
    }

    public boolean hasEnoughForWar(UUID clanId, int food, int materials) {
        Supply supply = getSupply(clanId);
        return supply.hasEnoughForWar(food, materials);
    }

    public void consumeForWar(UUID clanId, int food, int materials) {
        Supply supply = getSupply(clanId);
        supply.removeFood(food);
        supply.removeMaterials(materials);
        plugin.getStorageManager().saveSupply(supply);
    }

    public void addFood(UUID clanId, int amount) {
        Supply supply = getSupply(clanId);
        supply.addFood(amount);
        plugin.getStorageManager().saveSupply(supply);
    }

    public void addMaterials(UUID clanId, int amount) {
        Supply supply = getSupply(clanId);
        supply.addMaterials(amount);
        plugin.getStorageManager().saveSupply(supply);
    }

    public void addFuel(UUID clanId, int amount) {
        Supply supply = getSupply(clanId);
        supply.addFuel(amount);
        plugin.getStorageManager().saveSupply(supply);
    }

    public Collection<Supply> getAllSupplies() {
        return supplies.values();
    }
}