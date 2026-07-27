package com.ever.war.listeners;

import com.ever.war.EverWar;
import com.ever.war.models.Territory;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class TerritoryListener implements Listener {

    private final EverWar plugin;

    public TerritoryListener(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== ВЗАИМОДЕЙСТВИЕ С ENTITY НА ТЕРРИТОРИИ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("everwar.admin.bypass")) return;

        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        Territory territory = plugin.getTerritoryManager().getTerritoryByChunk(chunk);

        if (territory == null) return;

        var playerClan = plugin.getClanManager().getClanByPlayer(player);

        if (playerClan == null) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cЗащищённая территория!");
            return;
        }

        if (playerClan.getClanId().equals(territory.getOwnerClanId())) return;

        if (plugin.getDiplomacyManager().isAlly(
                playerClan.getClanId(), territory.getOwnerClanId())) return;

        // Во время войны — разрешено
        if (plugin.getWarManager().areAtWar(
                playerClan.getClanId(), territory.getOwnerClanId())) return;

        event.setCancelled(true);
        MessageUtil.sendActionBar(player, "&cЗащищённая территория!");
    }

    // ==================== УНИЧТОЖЕНИЕ ТРАНСПОРТА ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;
        if (player.hasPermission("everwar.admin.bypass")) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        Territory territory = plugin.getTerritoryManager().getTerritoryByChunk(chunk);

        if (territory == null) return;

        var playerClan = plugin.getClanManager().getClanByPlayer(player);

        if (playerClan == null) {
            event.setCancelled(true);
            return;
        }

        if (playerClan.getClanId().equals(territory.getOwnerClanId())) return;

        if (plugin.getDiplomacyManager().isAlly(
                playerClan.getClanId(), territory.getOwnerClanId())) return;

        if (plugin.getWarManager().areAtWar(
                playerClan.getClanId(), territory.getOwnerClanId())) return;

        event.setCancelled(true);
    }

    // ==================== ЗАГРУЗКА ЧАНКА (для мод-совместимости) ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Можно использовать для восстановления голограмм
        // или проверки территорий при загрузке чанков
        Chunk chunk = event.getChunk();
        String key = Territory.makeKey(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        Territory territory = plugin.getTerritoryManager().getTerritoryByKey(key);
        if (territory != null && territory.isCore()) {
            // Обновляем голограмму ядра при загрузке чанка
            if (plugin.getHologramHook() != null) {
                var clan = plugin.getClanManager().getClanById(territory.getOwnerClanId());
                if (clan != null) {
                    plugin.getHologramHook().updateHologram(territory, clan);
                }
            }
        }
    }
}