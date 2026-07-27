package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import com.ever.war.utils.MessageUtil;
import com.ever.war.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerritoryManager {

    private final EverWar plugin;

    // chunkKey -> Territory
    private final Map<String, Territory> territoriesByKey = new HashMap<>();

    // clanId -> список chunkKey
    private final Map<UUID, List<String>> clanTerritories = new HashMap<>();

    // ==================== SHIELD СИСТЕМА ====================

    /**
     * Щит территории клана.
     * clanId -> время когда щит истекает (unix timestamp)
     * Если значение = -1 → щит постоянный (permanent, нужно разрешение админа)
     * Если значение = 0 или нет записи → щит выключен
     * Если значение > текущее время → щит временный и активен
     */
    private final Map<UUID, Long> clanShields = new HashMap<>();

    // Максимальное время щита без разрешения админа (секунды)
    private static final int MAX_SHIELD_DURATION = 15 * 60; // 15 минут

    // Таймер проверки истечения щитов
    private int shieldTaskId = -1;

    public TerritoryManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== SHIELD МЕТОДЫ ====================

    /**
     * Проверить — активен ли щит у клана
     */
    public boolean isShieldActive(UUID clanId) {
        Long expiry = clanShields.get(clanId);
        if (expiry == null || expiry == 0) return false; // Выключен
        if (expiry == -1) return true; // Постоянный
        if (Instant.now().getEpochSecond() < expiry) return true; // Ещё действует
        // Истёк — убираем
        clanShields.remove(clanId);
        notifyClanShieldExpired(clanId);
        return false;
    }

    /**
     * Включить щит на время
     * @param clanId ID клана
     * @param durationSeconds время в секундах (макс 15 мин без админа)
     * @param byAdmin если true — без ограничения по времени
     * @return результат
     */
    public ShieldResult enableShield(UUID clanId, int durationSeconds, boolean byAdmin) {
        if (!byAdmin && durationSeconds > MAX_SHIELD_DURATION) {
            return ShieldResult.DURATION_TOO_LONG;
        }

        if (durationSeconds <= 0 && !byAdmin) {
            return ShieldResult.INVALID_DURATION;
        }

        long expiry = Instant.now().getEpochSecond() + durationSeconds;
        clanShields.put(clanId, expiry);

        // Запускаем таймер проверки если нужно
        startShieldTimer();

        // Оповещаем клан
        Clan clan = plugin.getClanManager().getClanById(clanId);
        if (clan != null) {
            String lang = plugin.getConfigManager().getLanguage();
            String time = TimeUtil.formatTime(durationSeconds, lang);

            for (var member : clan.getMemberList()) {
                if (member.isOnline()) {
                    Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                    if (p != null) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &a🛡 Щит территории &aвключён &7на &f" + time);
                        MessageUtil.sendTitle(p,
                                "&a🛡 ЩИТ ВКЛЮЧЁН",
                                "&7Территория защищена на " + time,
                                10, 60, 10);
                    }
                }
            }
        }

        return ShieldResult.SUCCESS;
    }

    /**
     * Включить постоянный щит (только админ)
     */
    public ShieldResult enablePermanentShield(UUID clanId) {
        clanShields.put(clanId, -1L);

        Clan clan = plugin.getClanManager().getClanById(clanId);
        if (clan != null) {
            for (var member : clan.getMemberList()) {
                if (member.isOnline()) {
                    Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                    if (p != null) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &a🛡 Щит территории &aвключён &7навсегда (админ)");
                    }
                }
            }
        }

        return ShieldResult.SUCCESS;
    }

    /**
     * Выключить щит
     */
    public ShieldResult disableShield(UUID clanId) {
        clanShields.remove(clanId);

        Clan clan = plugin.getClanManager().getClanById(clanId);
        if (clan != null) {
            for (var member : clan.getMemberList()) {
                if (member.isOnline()) {
                    Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                    if (p != null) {
                        MessageUtil.send(p,
                                "&8[&6EverWar&8] &c🛡 Щит территории &cвыключен! &7Территория уязвима.");
                        MessageUtil.sendTitle(p,
                                "&c🛡 ЩИТ ВЫКЛЮЧЕН",
                                "&7Территория открыта для атак!",
                                10, 60, 10);
                    }
                }
            }
        }

        return ShieldResult.SUCCESS;
    }

    /**
     * Получить оставшееся время щита
     */
    public long getShieldRemainingSeconds(UUID clanId) {
        Long expiry = clanShields.get(clanId);
        if (expiry == null || expiry == 0) return 0;
        if (expiry == -1) return -1; // Постоянный
        long remaining = expiry - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /**
     * Получить статус щита для дисплея
     */
    public String getShieldStatus(UUID clanId, String lang) {
        Long expiry = clanShields.get(clanId);

        if (expiry == null || expiry == 0) {
            return lang.equals("en")
                    ? "&c🛡 Shield: &cOFF &7(Territory is vulnerable!)"
                    : "&c🛡 Щит: &cВЫКЛ &7(Территория уязвима!)";
        }

        if (expiry == -1) {
            return lang.equals("en")
                    ? "&a🛡 Shield: &aPERMANENT &7(Admin)"
                    : "&a🛡 Щит: &aПОСТОЯННЫЙ &7(Админ)";
        }

        long remaining = expiry - Instant.now().getEpochSecond();
        if (remaining <= 0) {
            clanShields.remove(clanId);
            return lang.equals("en")
                    ? "&c🛡 Shield: &cOFF"
                    : "&c🛡 Щит: &cВЫКЛ";
        }

        String time = TimeUtil.formatTime(remaining, lang);
        return lang.equals("en")
                ? "&a🛡 Shield: &aON &7(" + time + " left)"
                : "&a🛡 Щит: &aВКЛ &7(осталось " + time + ")";
    }

    /**
     * Оповещение что щит истёк
     */
    private void notifyClanShieldExpired(UUID clanId) {
        Clan clan = plugin.getClanManager().getClanById(clanId);
        if (clan == null) return;

        for (var member : clan.getMemberList()) {
            if (member.isOnline()) {
                Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p,
                            "&8[&6EverWar&8] &c⚠ Щит территории &cистёк! &7Территория уязвима для атак!");
                    MessageUtil.sendTitle(p,
                            "&c⚠ ЩИТ ИСТЁК",
                            "&7Территория открыта для атак!",
                            10, 80, 10);
                    MessageUtil.soundWar(p);
                }
            }
        }
    }

    /**
     * Таймер проверки истечения щитов
     */
    private void startShieldTimer() {
        if (shieldTaskId != -1) return;

        shieldTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = Instant.now().getEpochSecond();
            List<UUID> expired = new ArrayList<>();

            for (var entry : clanShields.entrySet()) {
                long val = entry.getValue();
                if (val > 0 && val <= now) {
                    expired.add(entry.getKey());
                }
            }

            for (UUID clanId : expired) {
                clanShields.remove(clanId);
                notifyClanShieldExpired(clanId);
            }

            // Если нет щитов — останавливаем таймер
            boolean hasActive = clanShields.values().stream()
                    .anyMatch(v -> v != 0);
            if (!hasActive) {
                Bukkit.getScheduler().cancelTask(shieldTaskId);
                shieldTaskId = -1;
            }

        }, 20L * 10, 20L * 10).getTaskId(); // каждые 10 секунд
    }

    // ==================== РЕЗУЛЬТАТЫ SHIELD ====================

    public enum ShieldResult {
        SUCCESS,
        DURATION_TOO_LONG,   // Больше 15 минут без админа
        INVALID_DURATION,    // Некорректная длительность
        NO_PERMISSION        // Нет прав
    }

    // ==================== КЭШ ТЕРРИТОРИЙ ====================

    public void addTerritoryToCache(Territory territory) {
        territoriesByKey.put(territory.getChunkKey(), territory);
        clanTerritories
                .computeIfAbsent(territory.getOwnerClanId(), k -> new ArrayList<>())
                .add(territory.getChunkKey());
    }

    public void removeTerritoryFromCache(String chunkKey) {
        Territory territory = territoriesByKey.remove(chunkKey);
        if (territory != null) {
            List<String> list = clanTerritories.get(territory.getOwnerClanId());
            if (list != null) {
                list.remove(chunkKey);
                if (list.isEmpty()) {
                    clanTerritories.remove(territory.getOwnerClanId());
                }
            }
        }
    }

    // ==================== ЗАХВАТ / ОСВОБОЖДЕНИЕ ====================

    public ClaimResult claimChunk(Player player, Chunk chunk) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return ClaimResult.NOT_IN_CLAN;

        var member = clan.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canClaimTerritory()) {
            return ClaimResult.NO_PERMISSION;
        }

        String chunkKey = Territory.makeKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ());

        if (territoriesByKey.containsKey(chunkKey)) {
            Territory existing = territoriesByKey.get(chunkKey);
            if (existing.getOwnerClanId().equals(clan.getClanId())) {
                return ClaimResult.ALREADY_CLAIMED_OWN;
            }
            return ClaimResult.ALREADY_CLAIMED_OTHER;
        }

        int maxChunks = plugin.getConfigManager().getMaxChunks();
        int chunksPerPlayer = plugin.getConfigManager().getChunksPerPlayer();
        int memberCount = clan.getMemberCount();
        int allowedChunks = Math.min(maxChunks, memberCount * chunksPerPlayer);

        List<String> currentTerritories = clanTerritories.getOrDefault(
                clan.getClanId(), new ArrayList<>());

        if (currentTerritories.size() >= allowedChunks) {
            return ClaimResult.MAX_REACHED;
        }

        double cost = plugin.getConfigManager().getClaimCost();
        if (cost > 0 && plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                return ClaimResult.NOT_ENOUGH_MONEY;
            }
            plugin.getVaultHook().withdraw(player, cost);
        }

        Territory territory = new Territory(
                chunkKey,
                clan.getClanId(),
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );

        addTerritoryToCache(territory);
        plugin.getStorageManager().saveTerritory(territory);

        clan.addTerritoryCaptured();
        plugin.getStorageManager().saveClan(clan);

        if (plugin.getHologramHook() != null && territory.isCore()) {
            plugin.getHologramHook().createTerritoryHologram(territory, clan);
        }

        return ClaimResult.SUCCESS;
    }

    public UnclaimResult unclaimChunk(Player player, Chunk chunk) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return UnclaimResult.NOT_IN_CLAN;

        var member = clan.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canClaimTerritory()) {
            return UnclaimResult.NO_PERMISSION;
        }

        String chunkKey = Territory.makeKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ());

        Territory territory = territoriesByKey.get(chunkKey);
        if (territory == null) return UnclaimResult.NOT_CLAIMED;

        if (!territory.getOwnerClanId().equals(clan.getClanId())) {
            return UnclaimResult.NOT_YOUR_TERRITORY;
        }

        if (territory.isCore()) return UnclaimResult.IS_CORE;

        removeTerritoryFromCache(chunkKey);
        plugin.getStorageManager().deleteTerritory(chunkKey);

        return UnclaimResult.SUCCESS;
    }

    // ==================== ЯДРО БАЗЫ ====================

    public SetCoreResult setCore(Player player, Chunk chunk) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return SetCoreResult.NOT_IN_CLAN;

        if (!clan.isLeader(player.getUniqueId())) return SetCoreResult.NO_PERMISSION;

        String chunkKey = Territory.makeKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ());

        Territory territory = territoriesByKey.get(chunkKey);
        if (territory == null) return SetCoreResult.NOT_CLAIMED;
        if (!territory.getOwnerClanId().equals(clan.getClanId())) {
            return SetCoreResult.NOT_YOUR_TERRITORY;
        }

        for (String key : clanTerritories.getOrDefault(clan.getClanId(), new ArrayList<>())) {
            Territory t = territoriesByKey.get(key);
            if (t != null && t.isCore()) {
                t.setCore(false);
                plugin.getStorageManager().saveTerritory(t);
                if (plugin.getHologramHook() != null) {
                    plugin.getHologramHook().removeHologram(key);
                }
            }
        }

        territory.setCore(true);
        plugin.getStorageManager().saveTerritory(territory);

        if (plugin.getHologramHook() != null) {
            plugin.getHologramHook().createTerritoryHologram(territory, clan);
        }

        return SetCoreResult.SUCCESS;
    }

    // ==================== ПЕРЕДАЧА / УДАЛЕНИЕ ====================

    public void transferTerritory(String chunkKey, UUID newOwnerClanId) {
        Territory territory = territoriesByKey.get(chunkKey);
        if (territory == null) return;

        UUID oldOwner = territory.getOwnerClanId();
        List<String> oldList = clanTerritories.get(oldOwner);
        if (oldList != null) {
            oldList.remove(chunkKey);
            if (oldList.isEmpty()) clanTerritories.remove(oldOwner);
        }

        territory.setOwnerClanId(newOwnerClanId);
        territory.setCore(false);
        territory.setHp(territory.getMaxHp());

        clanTerritories.computeIfAbsent(newOwnerClanId, k -> new ArrayList<>())
                .add(chunkKey);

        plugin.getStorageManager().saveTerritory(territory);

        Clan newClan = plugin.getClanManager().getClanById(newOwnerClanId);
        if (plugin.getHologramHook() != null && newClan != null) {
            plugin.getHologramHook().updateHologram(territory, newClan);
        }
    }

    public void removeAllClanTerritories(UUID clanId) {
        List<String> keys = new ArrayList<>(
                clanTerritories.getOrDefault(clanId, new ArrayList<>()));

        for (String key : keys) {
            territoriesByKey.remove(key);
            plugin.getStorageManager().deleteTerritory(key);
            if (plugin.getHologramHook() != null) {
                plugin.getHologramHook().removeHologram(key);
            }
        }

        clanTerritories.remove(clanId);
        clanShields.remove(clanId);
    }

    // ==================== КАРТА ====================

    public List<String> generateMap(Player player) {
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        Chunk center = player.getLocation().getChunk();
        String worldName = player.getWorld().getName();
        String lang = plugin.getConfigManager().getLanguage();

        List<String> lines = new ArrayList<>();

        String header = lang.equalsIgnoreCase("en")
                ? "&8&l━━━━━ &6Territory Map &8&l━━━━━"
                : "&8&l━━━━━ &6Карта территорий &8&l━━━━━";
        lines.add(header);

        if (lang.equalsIgnoreCase("en")) {
            lines.add("&aYou &bAlly &cEnemy &7Other &8Wild &e✦You");
        } else {
            lines.add("&aВы &bСоюзники &cВраги &7Другие &8Дикая &e✦Вы");
        }

        int radius = 5;
        for (int dz = -radius; dz <= radius; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = center.getX() + dx;
                int cz = center.getZ() + dz;
                String key = Territory.makeKey(worldName, cx, cz);
                Territory territory = territoriesByKey.get(key);

                if (dx == 0 && dz == 0) {
                    row.append("&e✦");
                } else if (territory == null) {
                    row.append("&8░");
                } else {
                    UUID ownerClanId = territory.getOwnerClanId();
                    boolean shieldOn = isShieldActive(ownerClanId);
                    String shieldMark = shieldOn ? "▣" : "▪";

                    if (playerClan != null && ownerClanId.equals(playerClan.getClanId())) {
                        row.append(territory.isCore() ? "&a+" : "&a" + shieldMark);
                    } else if (playerClan != null && plugin.getDiplomacyManager()
                            .isAlly(playerClan.getClanId(), ownerClanId)) {
                        row.append("&b" + shieldMark);
                    } else if (playerClan != null && plugin.getDiplomacyManager()
                            .isEnemy(playerClan.getClanId(), ownerClanId)) {
                        row.append("&c" + shieldMark);
                    } else {
                        row.append("&7" + shieldMark);
                    }
                }
            }
            lines.add(row.toString());
        }

        lines.add("&7X:" + center.getX() + " Z:" + center.getZ());

        // Показываем статус щита если в клане
        if (playerClan != null) {
            lines.add(getShieldStatus(playerClan.getClanId(), lang));
        }

        lines.add("&8&l━━━━━━━━━━━━━━━━━━━━━━");

        return lines;
    }

    // ==================== ПОИСК ====================

    public Territory getTerritoryByChunk(Chunk chunk) {
        String key = Territory.makeKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ());
        return territoriesByKey.get(key);
    }

    public Territory getTerritoryByKey(String key) {
        return territoriesByKey.get(key);
    }

    public List<Territory> getClanTerritories(UUID clanId) {
        List<String> keys = clanTerritories.getOrDefault(clanId, new ArrayList<>());
        List<Territory> result = new ArrayList<>();
        for (String key : keys) {
            Territory t = territoriesByKey.get(key);
            if (t != null) result.add(t);
        }
        return result;
    }

    public Territory getClanCore(UUID clanId) {
        for (Territory t : getClanTerritories(clanId)) {
            if (t.isCore()) return t;
        }
        return null;
    }

    public int getClanTerritoryCount(UUID clanId) {
        return clanTerritories.getOrDefault(clanId, new ArrayList<>()).size();
    }

    public Collection<Territory> getAllTerritories() {
        return territoriesByKey.values();
    }

    public int getTerritoryCount() {
        return territoriesByKey.size();
    }

    public boolean isClaimed(Chunk chunk) {
        String key = Territory.makeKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ());
        return territoriesByKey.containsKey(key);
    }

    public boolean isClanTerritory(Chunk chunk, UUID clanId) {
        Territory t = getTerritoryByChunk(chunk);
        return t != null && t.getOwnerClanId().equals(clanId);
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    public enum ClaimResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, ALREADY_CLAIMED_OWN,
        ALREADY_CLAIMED_OTHER, MAX_REACHED, NOT_ENOUGH_MONEY
    }

    public enum UnclaimResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, NOT_CLAIMED,
        NOT_YOUR_TERRITORY, IS_CORE
    }

    public enum SetCoreResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, NOT_CLAIMED, NOT_YOUR_TERRITORY
    }
}