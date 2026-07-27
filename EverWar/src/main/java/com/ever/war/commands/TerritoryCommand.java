package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.gui.TerritoryMapGUI;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class TerritoryCommand {

    private final EverWar plugin;

    public TerritoryCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "claim", "захватить" -> handleClaim(player);
            case "unclaim", "освободить" -> handleUnclaim(player);
            case "map", "карта" -> handleMap(player, args);
            case "setcore", "ядро" -> handleSetCore(player);
            case "info", "инфо" -> handleInfo(player);
            case "upgrade", "улучшить" -> handleUpgrade(player);
            default -> sendHelp(player);
        }
    }

    private void handleClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getTerritoryManager().claimChunk(player, chunk);

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        String clanName = clan != null ? clan.getName() : "";

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "territory-claimed",
                        "{clan}", clanName);
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case ALREADY_CLAIMED_OWN -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cЭтот чанк уже ваш.");
                MessageUtil.soundError(player);
            }
            case ALREADY_CLAIMED_OTHER -> {
                Territory t = plugin.getTerritoryManager().getTerritoryByChunk(chunk);
                Clan owner = t != null ? plugin.getClanManager().getClanById(t.getOwnerClanId()) : null;
                String ownerName = owner != null ? owner.getName() : "Unknown";
                MessageUtil.sendMessage(player, "territory-denied",
                        "{clan}", ownerName);
                MessageUtil.soundError(player);
            }
            case MAX_REACHED -> {
                int maxChunks = Math.min(
                        plugin.getConfigManager().getMaxChunks(),
                        (clan != null ? clan.getMemberCount() : 1)
                                * plugin.getConfigManager().getChunksPerPlayer());
                MessageUtil.sendMessage(player, "territory-max-reached",
                        "{max}", String.valueOf(maxChunks));
                MessageUtil.soundError(player);
            }
            case NOT_ENOUGH_MONEY -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cНедостаточно денег. Стоимость: &f"
                                + plugin.getConfigManager().getClaimCost());
                MessageUtil.soundError(player);
            }
        }
    }

    private void handleUnclaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getTerritoryManager().unclaimChunk(player, chunk);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.sendMessage(player, "territory-unclaimed");
                MessageUtil.soundSuccess(player);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case NOT_CLAIMED -> MessageUtil.sendMessage(player, "territory-not-claimed");
            case NOT_YOUR_TERRITORY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cЭто не территория вашего клана.");
            case IS_CORE -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНельзя освободить ядро базы!");
        }
    }

    private void handleMap(Player player, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("gui")) {
            TerritoryMapGUI.open(player);
        } else {
            var lines = plugin.getTerritoryManager().generateMap(player);
            for (String line : lines) {
                MessageUtil.send(player, line);
            }
        }
    }

    private void handleSetCore(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getTerritoryManager().setCore(player, chunk);

        switch (result) {
            case SUCCESS -> {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &a⭐ Ядро базы установлено здесь!");
                MessageUtil.soundSuccess(player);
                MessageUtil.sendTitle(player,
                        "&6⭐ Ядро базы", "&7установлено", 10, 40, 10);
            }
            case NOT_IN_CLAN -> MessageUtil.sendMessage(player, "not-in-clan");
            case NO_PERMISSION -> {
                MessageUtil.sendMessage(player, "no-permission");
                MessageUtil.soundError(player);
            }
            case NOT_CLAIMED -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cСначала захватите этот чанк.");
            case NOT_YOUR_TERRITORY -> MessageUtil.send(player,
                    "&8[&6EverWar&8] &cЭто не территория вашего клана.");
        }
    }

    private void handleInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Territory territory = plugin.getTerritoryManager().getTerritoryByChunk(chunk);

        if (territory == null) {
            MessageUtil.sendMessage(player, "territory-wilderness");
            return;
        }

        Clan owner = plugin.getClanManager().getClanById(territory.getOwnerClanId());
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player);
        String lang = plugin.getConfigManager().getLanguage();

        String ownerName = owner != null ? owner.getName() : "Unknown";
        boolean own = playerClan != null
                && territory.getOwnerClanId().equals(playerClan.getClanId());

        MessageUtil.send(player, "&8&l━━━━━ &6 Территория &8&l━━━━━");
        MessageUtil.send(player, "&7Чанк: &fX:" + territory.getChunkX()
                + " Z:" + territory.getChunkZ());
        MessageUtil.send(player, "&7Владелец: " + (own ? "&a" : "&f") + ownerName);
        MessageUtil.send(player, "&7Ядро: " + (territory.isCore() ? "&a⭐ Да" : "&7Нет"));
        MessageUtil.send(player, "&7HP: " + territory.getHpDisplay());
        MessageUtil.send(player, "&7Защита: Ур. &f" + territory.getDefenseLevel());
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleUpgrade(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        Territory territory = plugin.getTerritoryManager().getTerritoryByChunk(chunk);
        if (territory == null || !territory.getOwnerClanId().equals(clan.getClanId())) {
            MessageUtil.send(player, "&8[&6EverWar&8] &cЭто не территория вашего клана.");
            return;
        }

        double cost = plugin.getConfigManager().getUpgradeBaseCost()
                * (territory.getDefenseLevel() + 1);

        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &cНедостаточно денег. Стоимость: &f" + cost);
                return;
            }
        }

        if (!territory.upgrade()) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cМаксимальный уровень защиты (5).");
            return;
        }

        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().withdraw(player, cost);
        }

        plugin.getStorageManager().saveTerritory(territory);

        MessageUtil.send(player,
                "&8[&6EverWar&8] &a🛡 Защита улучшена до уровня &f"
                        + territory.getDefenseLevel()
                        + "&a! HP: &f" + territory.getHpDisplay());
        MessageUtil.soundSuccess(player);
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &6 Территория &8&l━━━━━");
        MessageUtil.send(player, "&e/war territory claim &7— Захватить чанк");
        MessageUtil.send(player, "&e/war territory unclaim &7— Освободить чанк");
        MessageUtil.send(player, "&e/war territory map &7— Карта в чате");
        MessageUtil.send(player, "&e/war territory map gui &7— Карта GUI");
        MessageUtil.send(player, "&e/war territory setcore &7— Установить ядро");
        MessageUtil.send(player, "&e/war territory info &7— Информация о чанке");
        MessageUtil.send(player, "&e/war territory upgrade &7— Улучшить защиту");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}