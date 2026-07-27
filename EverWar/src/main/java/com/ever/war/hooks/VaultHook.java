package com.ever.war.hooks;

import com.ever.war.EverWar;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final EverWar plugin;
    private Economy economy;
    private boolean enabled;

    public VaultHook(EverWar plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            enabled = false;
            return false;
        }

        economy = rsp.getProvider();
        enabled = (economy != null);
        return enabled;
    }

    public boolean isEnabled() {
        return enabled && economy != null;
    }

    /**
     * Проверить баланс игрока
     */
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Есть ли у игрока достаточно денег
     */
    public boolean has(Player player, double amount) {
        if (!isEnabled()) return true; // Если нет экономики — всё бесплатно
        return economy.has(player, amount);
    }

    /**
     * Снять деньги
     */
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return true;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Начислить деньги
     */
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) return true;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Форматировать сумму
     */
    public String format(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }

    /**
     * Получить название валюты
     */
    public String getCurrencyName() {
        if (!isEnabled()) return "coins";
        return economy.currencyNamePlural();
    }

    public Economy getEconomy() {
        return economy;
    }
}