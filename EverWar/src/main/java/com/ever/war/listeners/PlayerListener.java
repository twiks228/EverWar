package com.ever.war.listeners;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import com.ever.war.utils.ColorUtil;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final EverWar plugin;

    // Кэш — в каком чанке игрок сейчас находится
    private final Map<UUID, String> playerChunkCache = new HashMap<>();

    public PlayerListener(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== ВХОД ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Сохраняем данные игрока
        String lang = plugin.getConfigManager().getLanguage();
        plugin.getStorageManager().savePlayerData(
                player.getUniqueId(), player.getName(), lang);

        // Обновляем статус онлайн в клане
        plugin.getClanManager().updateMemberOnline(player, true);

        // Кэшируем текущий чанк
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = Territory.makeKey(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        playerChunkCache.put(player.getUniqueId(), chunkKey);

        // Показываем приветствие если в клане
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan != null) {
            int online = clan.getOnlineMembers().size();
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &7Клан: " + clan.getFormattedTag()
                            + " &7| Онлайн: &f" + online + "&7/&f" + clan.getMemberCount());

            // Если идёт война — предупреждаем
            var wars = plugin.getWarManager().getClanWars(clan.getClanId());
            if (!wars.isEmpty()) {
                MessageUtil.send(player,
                        "&8[&6EverWar&8] &c⚔ Внимание! Ваш клан в состоянии войны! (/war war status)");
                MessageUtil.soundWar(player);
            }

            // Если идёт осада
            for (var siege : plugin.getSiegeManager().getAllSieges()) {
                if (siege.getDefenderClanId().equals(clan.getClanId()) && siege.isActive()) {
                    MessageUtil.send(player,
                            "&8[&6EverWar&8] &c🛡 ВНИМАНИЕ! Ваша территория под осадой! (/war siege status)");
                    MessageUtil.soundWar(player);
                    break;
                }
            }

            // Оповещаем клан
            for (var member : clan.getMemberList()) {
                if (member.isOnline()
                        && !member.getPlayerUUID().equals(player.getUniqueId())) {
                    Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                    if (p != null) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &a" + player.getName() + " &aвошёл на сервер.");
                    }
                }
            }
        }
    }

    // ==================== ВЫХОД ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Обновляем статус
        plugin.getClanManager().updateMemberOnline(player, false);

        // Чистим кэш
        playerChunkCache.remove(player.getUniqueId());

        // Оповещаем клан
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan != null) {
            for (var member : clan.getMemberList()) {
                if (member.isOnline()
                        && !member.getPlayerUUID().equals(player.getUniqueId())) {
                    Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                    if (p != null) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &c" + player.getName() + " &cвышел с сервера.");
                    }
                }
            }
        }
    }

    // ==================== ПЕРЕМЕЩЕНИЕ (СМЕНА ЧАНКА) ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Оптимизация: проверяем только если сменился блок
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Chunk newChunk = event.getTo().getChunk();
        String newKey = Territory.makeKey(
                newChunk.getWorld().getName(), newChunk.getX(), newChunk.getZ());

        String oldKey = playerChunkCache.get(player.getUniqueId());

        // Если чанк не изменился — пропускаем
        if (newKey.equals(oldKey)) return;

        playerChunkCache.put(player.getUniqueId(), newKey);

        // Получаем территории
        Territory oldTerritory = oldKey != null
                ? plugin.getTerritoryManager().getTerritoryByKey(oldKey) : null;
        Territory newTerritory = plugin.getTerritoryManager().getTerritoryByKey(newKey);

        // Проверяем — сменился ли владелец территории
        UUID oldOwner = oldTerritory != null ? oldTerritory.getOwnerClanId() : null;
        UUID newOwner = newTerritory != null ? newTerritory.getOwnerClanId() : null;

        // Если владелец не изменился — пропускаем
        if (oldOwner == newOwner) return;
        if (oldOwner != null && oldOwner.equals(newOwner)) return;

        // Показываем сообщение о смене территории
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player);
        String lang = plugin.getConfigManager().getLanguage();

        if (newTerritory == null) {
            // Вышли на ничейную территорию
            String msg = lang.equals("en") ? "&7~ Wilderness" : "&7~ Дикая местность";
            MessageUtil.sendActionBar(player, msg);
        } else {
            Clan ownerClan = plugin.getClanManager().getClanById(newOwner);
            String ownerName = ownerClan != null ? ownerClan.getName() : "Unknown";
            String ownerTag = ownerClan != null ? ownerClan.getTag() : "???";
            String color;

            if (playerClan != null && newOwner.equals(playerClan.getClanId())) {
                // Своя территория
                color = "&a";
                String msg = color + "~ " + ownerName + " [" + ownerTag + "]"
                        + (newTerritory.isCore() ? " ⭐" : "");
                MessageUtil.sendActionBar(player, msg);
            } else if (playerClan != null
                    && plugin.getDiplomacyManager().isAlly(
                    playerClan.getClanId(), newOwner)) {
                // Территория союзника
                color = "&b";
                String label = lang.equals("en") ? "Ally" : "Союзник";
                MessageUtil.sendActionBar(player,
                        color + "~ " + ownerName + " [" + ownerTag + "] &7(" + label + ")");
            } else if (playerClan != null
                    && plugin.getDiplomacyManager().isEnemy(
                    playerClan.getClanId(), newOwner)) {
                // Территория врага
                color = "&c";
                String label = lang.equals("en") ? "Enemy" : "Враг";
                MessageUtil.sendActionBar(player,
                        color + "~ " + ownerName + " [" + ownerTag + "] &c(" + label + ")");
            } else {
                // Чужая территория
                color = "&e";
                MessageUtil.sendActionBar(player,
                        color + "~ " + ownerName + " [" + ownerTag + "]");
            }

            // Предупреждение если территория под осадой
            var siege = plugin.getSiegeManager().getSiegeByChunk(newKey);
            if (siege != null && siege.isActive()) {
                MessageUtil.sendTitle(player,
                        "&c⚔ ОСАДА", "&7Эта территория под осадой!", 5, 30, 5);
            }
        }
    }

    // ==================== РЕСПАУН ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Обновляем кэш чанка
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Chunk chunk = player.getLocation().getChunk();
            String key = Territory.makeKey(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            playerChunkCache.put(player.getUniqueId(), key);
        }, 5L);
    }
}