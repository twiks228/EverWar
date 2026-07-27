package com.ever.war.listeners;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * СИСТЕМА ЗАЩИТЫ ТЕРРИТОРИЙ (БЕЗ WORLDGUARD)
 * 
 * КЛЮЧЕВОЕ ПРАВИЛО:
 * По умолчанию защита территории ВЫКЛЮЧЕНА!
 * Любой может ломать, строить, взрывать ТНТ, дронами, гранатами и т.д.
 * 
 * Защиту можно включить:
 * - /war shield on        — временно на 15 минут (для всех с ролью Офицер+)
 * - /war shield on <мин>  — на указанное время (макс 15 мин без админа)
 * - /war shield off       — выключить защиту
 * - /war shield permanent — постоянная защита (только с разрешения админа)
 * - /war admin shield <clan> on/off/permanent — админ управление
 * 
 * Что делает защита когда ВКЛЮЧЕНА:
 * - Чужие не могут ломать/строить на территории
 * - Взрывы не разрушают блоки на территории
 * - Огонь не распространяется
 * - Поршни не пушат блоки через границы
 * - Жидкость не течёт через границы
 * 
 * Что делает защита когда ВЫКЛЮЧЕНА:
 * - Всё работает как обычно — ТНТ взрывает, гранаты летят, дроны бомбят
 * - Полный реализм войны
 * 
 * ВОЙНА всегда отключает защиту на вражеской территории!
 * Даже если shield включён — враги во время войны могут разрушать.
 */
public class BlockListener implements Listener {

    private final EverWar plugin;

    public BlockListener(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== ЛОМАНИЕ БЛОКОВ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player, block.getLocation());

        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
            Territory t = getTerritoryAt(block.getLocation());
            Clan owner = t != null
                    ? plugin.getClanManager().getClanById(t.getOwnerClanId()) : null;
            String ownerName = owner != null ? owner.getName() : "Unknown";
            MessageUtil.sendMessage(player, "territory-denied",
                    "{clan}", ownerName);
            MessageUtil.soundError(player);
        }
        // WAR_ZONE и ALLOWED — разрешено
        // NO_SHIELD — разрешено (защита выключена)
    }

    // ==================== СТРОИТЕЛЬСТВО ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player, block.getLocation());

        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
            Territory t = getTerritoryAt(block.getLocation());
            Clan owner = t != null
                    ? plugin.getClanManager().getClanById(t.getOwnerClanId()) : null;
            String ownerName = owner != null ? owner.getName() : "Unknown";
            MessageUtil.sendMessage(player, "territory-denied",
                    "{clan}", ownerName);
            MessageUtil.soundError(player);
        }
    }

    // ==================== ВЁДРА ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player, event.getBlock().getLocation());
        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cЗащита территории включена!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player, event.getBlock().getLocation());
        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cЗащита территории включена!");
        }
    }

    // ==================== РАМКИ / КАРТИНЫ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player,
                event.getEntity().getLocation());
        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return;
        if (player.hasPermission("everwar.admin.bypass")) return;

        ProtectionResult result = checkProtection(player,
                event.getEntity().getLocation());
        if (result == ProtectionResult.DENIED) {
            event.setCancelled(true);
        }
    }

    // ==================== ВЗАИМОДЕЙСТВИЕ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission("everwar.admin.bypass")) return;

        Block block = event.getClickedBlock();
        Material type = block.getType();

        if (isProtectedInteractable(type)) {
            ProtectionResult result = checkProtection(player, block.getLocation());
            if (result == ProtectionResult.DENIED) {
                event.setCancelled(true);
                MessageUtil.sendActionBar(player, "&cЗащита территории включена!");
            }
        }
    }

    // ==================== ВЗРЫВЫ (ТНТ, КРИПЕР, ДРОНЫ, ГРАНАТЫ, МОДЫ) ====================

    /**
     * ЛОГИКА ВЗРЫВОВ:
     * 
     * 1. НИЧЕЙНАЯ ТЕРРИТОРИЯ:
     *    - Взрывы работают ВСЕГДА
     * 
     * 2. ТЕРРИТОРИЯ С ВЫКЛЮЧЕННОЙ ЗАЩИТОЙ (shield off):
     *    - Взрывы работают ВСЕГДА — ТНТ, гранаты, дроны, ракеты, всё летит!
     *    - Это реализм — если не включил щит, база уязвима
     *    - Урон наносится HP территории
     * 
     * 3. ТЕРРИТОРИЯ С ВКЛЮЧЁННОЙ ЗАЩИТОЙ (shield on):
     *    - Свои взрывы — НЕ разрушают (защита от саботажа)
     *    - Взрывы от врагов ВО ВРЕМЯ ВОЙНЫ — РАЗРУШАЮТ! (щит не спасает от войны)
     *    - Взрывы от нейтральных/случайные — НЕ разрушают
     *    - Союзные взрывы — НЕ разрушают
     * 
     * 4. ВОЙНА всегда пробивает щит!
     */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList(), null);
    }

    private void handleExplosion(List<Block> blockList, Entity sourceEntity) {
        UUID sourceClanId = getExplosionSourceClan(sourceEntity);

        Iterator<Block> iterator = blockList.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Territory territory = getTerritoryAt(block.getLocation());

            // ========== НИЧЕЙНАЯ ТЕРРИТОРИЯ — всё работает ==========
            if (territory == null) {
                continue;
            }

            UUID ownerClanId = territory.getOwnerClanId();

            // ========== ПРОВЕРЯЕМ ВКЛЮЧЁН ЛИ ЩИТ ==========
            boolean shieldActive = plugin.getTerritoryManager()
                    .isShieldActive(ownerClanId);

            if (!shieldActive) {
                // ЩИТ ВЫКЛЮЧЕН — взрывы работают на территории!
                // Наносим урон территории
                applyTerritoryDamage(territory, sourceClanId, 2.0);
                continue; // Не убираем блок — он будет разрушен
            }

            // ========== ЩИТ ВКЛЮЧЁН ==========

            if (sourceClanId == null) {
                // Неизвестный источник (крипер, мобы) — щит защищает
                iterator.remove();
                continue;
            }

            if (sourceClanId.equals(ownerClanId)) {
                // Свой взрыв на своей территории — щит защищает
                iterator.remove();
                continue;
            }

            // Союзник — щит защищает
            if (plugin.getDiplomacyManager().isAlly(sourceClanId, ownerClanId)) {
                iterator.remove();
                continue;
            }

            // ⚔ ВОЙНА — ЩИТ НЕ СПАСАЕТ ОТ ВОЙНЫ!
            if (plugin.getWarManager().areAtWar(sourceClanId, ownerClanId)) {
                // Враг во время войны — взрывы РАЗРУШАЮТ даже с щитом!
                applyTerritoryDamage(territory, sourceClanId, 2.0);
                continue; // Блок будет разрушен
            }

            // Нейтральный чужой — щит защищает
            iterator.remove();
        }
    }

    /**
     * Нанести урон территории и проверить падение
     */
    private void applyTerritoryDamage(Territory territory,
                                       UUID attackerClanId, double damagePerBlock) {
        if (territory.damage(damagePerBlock)) {
            // HP территории <= 0 — территория пала!
            handleTerritoryFallen(territory, attackerClanId);
        }
        plugin.getStorageManager().saveTerritory(territory);
    }

    /**
     * Определяем какому клану принадлежит источник взрыва.
     * Поддержка: ТНТ, снаряды, дроны (моды), гранаты, ракеты и т.д.
     */
    private UUID getExplosionSourceClan(Entity entity) {
        if (entity == null) return null;

        // TNT — кто поджёг
        if (entity instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                return clan != null ? clan.getClanId() : null;
            }
        }

        // Снаряд (стрела, фаербол, мод-снаряды, гранаты)
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                Clan clan = plugin.getClanManager().getClanByPlayer(player);
                return clan != null ? clan.getClanId() : null;
            }
        }

        // Крипер, визер и т.д. — без клана
        if (entity.getType() == EntityType.CREEPER
                || entity.getType() == EntityType.GHAST
                || entity.getType() == EntityType.WITHER) {
            return null;
        }

        // Для модов — проверяем rider/passenger/vehicle
        return getEntityOwnerClan(entity);
    }

    /**
     * Обработка падения территории (HP = 0)
     */
    private void handleTerritoryFallen(Territory territory, UUID attackerClanId) {
        UUID defenderClanId = territory.getOwnerClanId();
        Clan attacker = plugin.getClanManager().getClanById(attackerClanId);
        Clan defender = plugin.getClanManager().getClanById(defenderClanId);

        String attackerName = attacker != null ? attacker.getName() : "Unknown";
        String defenderName = defender != null ? defender.getName() : "Unknown";

        if (territory.isCore()) {
            // ЯДРО БАЗЫ РАЗРУШЕНО!
            MessageUtil.broadcast(
                    "&8[&6EverWar&8] &4💥 Ядро базы клана &f" + defenderName
                            + " &4уничтожено кланом &f" + attackerName + "&4!");

            // Территория переходит атакующим
            if (attackerClanId != null) {
                plugin.getTerritoryManager().transferTerritory(
                        territory.getChunkKey(), attackerClanId);
                plugin.getWarManager().addCaptureScore(attackerClanId, defenderClanId);
            }
        } else {
            // Обычная территория пала — становится ничейной
            MessageUtil.broadcast(
                    "&8[&6EverWar&8] &c💥 Территория клана &f" + defenderName
                            + " &cразрушена кланом &f" + attackerName + "&c!");

            plugin.getTerritoryManager().removeTerritoryFromCache(territory.getChunkKey());
            plugin.getStorageManager().deleteTerritory(territory.getChunkKey());
        }
    }

    // ==================== ПОРШНИ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Location newLoc = block.getRelative(event.getDirection()).getLocation();
            Territory fromTerritory = getTerritoryAt(block.getLocation());
            Territory toTerritory = getTerritoryAt(newLoc);

            // Проверяем только если хотя бы одна территория со щитом
            if (toTerritory != null
                    && plugin.getTerritoryManager()
                    .isShieldActive(toTerritory.getOwnerClanId())) {
                if (fromTerritory == null
                        || !fromTerritory.getOwnerClanId()
                        .equals(toTerritory.getOwnerClanId())) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (fromTerritory != null
                    && plugin.getTerritoryManager()
                    .isShieldActive(fromTerritory.getOwnerClanId())) {
                if (toTerritory == null
                        || !fromTerritory.getOwnerClanId()
                        .equals(toTerritory.getOwnerClanId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Territory blockTerritory = getTerritoryAt(block.getLocation());
            Territory pistonTerritory = getTerritoryAt(event.getBlock().getLocation());

            if (blockTerritory != null
                    && plugin.getTerritoryManager()
                    .isShieldActive(blockTerritory.getOwnerClanId())) {
                if (pistonTerritory == null
                        || !blockTerritory.getOwnerClanId()
                        .equals(pistonTerritory.getOwnerClanId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // ==================== ЖИДКОСТИ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Territory to = getTerritoryAt(event.getToBlock().getLocation());
        if (to == null) return;

        if (!plugin.getTerritoryManager().isShieldActive(to.getOwnerClanId())) return;

        Territory from = getTerritoryAt(event.getBlock().getLocation());

        if (from == null
                || !from.getOwnerClanId().equals(to.getOwnerClanId())) {
            event.setCancelled(true);
        }
    }

    // ==================== ОГОНЬ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Territory territory = getTerritoryAt(event.getBlock().getLocation());
        if (territory != null
                && plugin.getTerritoryManager()
                .isShieldActive(territory.getOwnerClanId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Territory territory = getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        boolean shieldActive = plugin.getTerritoryManager()
                .isShieldActive(territory.getOwnerClanId());

        if (!shieldActive) return; // Щит выключен — можно всё

        if (event.getPlayer() != null) {
            Player player = event.getPlayer();
            if (player.hasPermission("everwar.admin.bypass")) return;

            UUID ownerClanId = territory.getOwnerClanId();
            Clan playerClan = plugin.getClanManager().getClanByPlayer(player);

            if (playerClan == null) {
                event.setCancelled(true);
                return;
            }

            if (playerClan.getClanId().equals(ownerClanId)) return;

            // Война пробивает щит
            if (plugin.getWarManager().areAtWar(
                    playerClan.getClanId(), ownerClanId)) {
                return; // Разрешаем
            }

            if (plugin.getDiplomacyManager().isAlly(
                    playerClan.getClanId(), ownerClanId)) {
                return; // Союзник — разрешаем
            }

            event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE) {
            Territory territory = getTerritoryAt(event.getBlock().getLocation());
            if (territory != null
                    && plugin.getTerritoryManager()
                    .isShieldActive(territory.getOwnerClanId())) {
                event.setCancelled(true);
            }
        }
    }

    // ==================== МОД-СУЩНОСТИ ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Territory territory = getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        boolean shieldActive = plugin.getTerritoryManager()
                .isShieldActive(territory.getOwnerClanId());

        if (!shieldActive) return; // Щит выключен — всё разрешено

        Entity entity = event.getEntity();
        UUID sourceClanId = getEntityOwnerClan(entity);
        UUID ownerClanId = territory.getOwnerClanId();

        if (sourceClanId == null) {
            event.setCancelled(true);
            return;
        }

        if (sourceClanId.equals(ownerClanId)) return;

        // Война пробивает щит
        if (plugin.getWarManager().areAtWar(sourceClanId, ownerClanId)) return;

        // Союзник
        if (plugin.getDiplomacyManager().isAlly(sourceClanId, ownerClanId)) return;

        event.setCancelled(true);
    }

    // ==================== ОБЩАЯ ЛОГИКА ЗАЩИТЫ ====================

    /**
     * КЛЮЧЕВАЯ ЛОГИКА:
     * 
     * 1. Ничейная территория → ALLOWED
     * 2. Своя территория → ALLOWED
     * 3. Союзная территория → ALLOWED
     * 4. Щит ВЫКЛЮЧЕН → NO_SHIELD (всё разрешено!)
     * 5. Щит ВКЛЮЧЁН + война → WAR_ZONE (разрешено — война пробивает)
     * 6. Щит ВКЛЮЧЁН + нет войны → DENIED (защищено)
     */
    private ProtectionResult checkProtection(Player player, Location location) {
        Territory territory = getTerritoryAt(location);

        // Ничейная территория — всё разрешено
        if (territory == null) return ProtectionResult.ALLOWED;

        UUID ownerClanId = territory.getOwnerClanId();
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player);

        // Игрок без клана
        if (playerClan == null) {
            // Если щит выключен — можно
            if (!plugin.getTerritoryManager().isShieldActive(ownerClanId)) {
                return ProtectionResult.NO_SHIELD;
            }
            return ProtectionResult.DENIED;
        }

        // Своя территория — разрешено
        if (playerClan.getClanId().equals(ownerClanId)) {
            return ProtectionResult.ALLOWED;
        }

        // Союзник — разрешено
        if (plugin.getDiplomacyManager().isAlly(
                playerClan.getClanId(), ownerClanId)) {
            return ProtectionResult.ALLOWED;
        }

        // ЩИТ ВЫКЛЮЧЕН — всё разрешено
        if (!plugin.getTerritoryManager().isShieldActive(ownerClanId)) {
            return ProtectionResult.NO_SHIELD;
        }

        // ЩИТ ВКЛЮЧЁН

        // Война пробивает щит!
        if (plugin.getWarManager().areAtWar(
                playerClan.getClanId(), ownerClanId)) {
            return ProtectionResult.WAR_ZONE;
        }

        // Щит включён, нет войны — защищено
        return ProtectionResult.DENIED;
    }

    private Territory getTerritoryAt(Location location) {
        Chunk chunk = location.getChunk();
        return plugin.getTerritoryManager().getTerritoryByChunk(chunk);
    }

    /**
     * Определить клан-владелец сущности (для модов)
     */
    private UUID getEntityOwnerClan(Entity entity) {
        if (entity instanceof Player player) {
            Clan c = plugin.getClanManager().getClanByPlayer(player);
            return c != null ? c.getClanId() : null;
        }

        if (entity instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player player) {
                Clan c = plugin.getClanManager().getClanByPlayer(player);
                return c != null ? c.getClanId() : null;
            }
        }

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof Player player) {
            Clan c = plugin.getClanManager().getClanByPlayer(player);
            return c != null ? c.getClanId() : null;
        }

        for (Entity p : entity.getPassengers()) {
            if (p instanceof Player player) {
                Clan c = plugin.getClanManager().getClanByPlayer(player);
                return c != null ? c.getClanId() : null;
            }
        }

        // Ближайший игрок для мод-снарядов
        for (Entity nearby : entity.getNearbyEntities(5, 5, 5)) {
            if (nearby instanceof Player player) {
                Clan c = plugin.getClanManager().getClanByPlayer(player);
                if (c != null) return c.getClanId();
            }
        }

        return null;
    }

    private boolean isProtectedInteractable(Material type) {
        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.BREWING_STAND
                || type == Material.ANVIL
                || type == Material.CHIPPED_ANVIL
                || type == Material.DAMAGED_ANVIL
                || type == Material.ENCHANTING_TABLE
                || type == Material.BEACON
                || type == Material.SHULKER_BOX
                || type.name().contains("SHULKER")
                || type.name().contains("DOOR")
                || type.name().contains("GATE")
                || type.name().contains("TRAPDOOR")
                || type == Material.LEVER
                || type.name().contains("BUTTON")
                || type == Material.NOTE_BLOCK
                || type == Material.JUKEBOX
                || type == Material.LECTERN
                || type == Material.CAMPFIRE
                || type == Material.SOUL_CAMPFIRE
                || type == Material.BELL
                || type == Material.RESPAWN_ANCHOR
                || type == Material.COMPARATOR
                || type == Material.REPEATER;
    }

    // ==================== РЕЗУЛЬТАТ ====================

    private enum ProtectionResult {
        ALLOWED,    // Разрешено (свой / союзник)
        DENIED,     // Запрещено (щит включён, чужая территория)
        WAR_ZONE,   // Разрешено (война пробивает щит)
        NO_SHIELD   // Разрешено (щит выключен — реализм)
    }
}