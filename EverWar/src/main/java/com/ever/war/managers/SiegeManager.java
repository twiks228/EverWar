package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Siege;
import com.ever.war.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SiegeManager {

    private final EverWar plugin;

    private final Map<UUID, Siege> siegesById = new HashMap<>();
    private final Map<String, UUID> siegeByChunk = new HashMap<>();
    private final Map<UUID, List<UUID>> clanSieges = new HashMap<>();

    private int taskId = -1;

    public SiegeManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== КЭШ ====================

    public void addSiegeToCache(Siege siege) {
        siegesById.put(siege.getSiegeId(), siege);
        siegeByChunk.put(siege.getChunkKey(), siege.getSiegeId());
        clanSieges.computeIfAbsent(siege.getAttackerClanId(), k -> new ArrayList<>())
                .add(siege.getSiegeId());
        clanSieges.computeIfAbsent(siege.getDefenderClanId(), k -> new ArrayList<>())
                .add(siege.getSiegeId());
    }

    public void removeSiegeFromCache(UUID siegeId) {
        Siege siege = siegesById.remove(siegeId);
        if (siege != null) {
            siegeByChunk.remove(siege.getChunkKey());
            List<UUID> attList = clanSieges.get(siege.getAttackerClanId());
            if (attList != null) attList.remove(siegeId);
            List<UUID> defList = clanSieges.get(siege.getDefenderClanId());
            if (defList != null) defList.remove(siegeId);
        }
    }

    // ==================== НАЧАЛО ОСАДЫ ====================

    public StartResult startSiege(Player attacker, Chunk targetChunk) {
        Clan attackerClan = plugin.getClanManager().getClanByPlayer(attacker.getUniqueId());
        if (attackerClan == null) return StartResult.NOT_IN_CLAN;

        var member = attackerClan.getMember(attacker.getUniqueId());
        if (member == null || !member.getRole().canDeclareWar()) {
            return StartResult.NO_PERMISSION;
        }

        String chunkKey = Territory.makeKey(
                targetChunk.getWorld().getName(),
                targetChunk.getX(),
                targetChunk.getZ());

        Territory target = plugin.getTerritoryManager().getTerritoryByKey(chunkKey);
        if (target == null) return StartResult.NOT_CLAIMED;

        UUID defenderClanId = target.getOwnerClanId();
        if (defenderClanId.equals(attackerClan.getClanId())) {
            return StartResult.OWN_TERRITORY;
        }

        if (!plugin.getWarManager().areAtWar(
                attackerClan.getClanId(), defenderClanId)) {
            return StartResult.NOT_AT_WAR;
        }

        if (siegeByChunk.containsKey(chunkKey)) {
            return StartResult.ALREADY_SIEGED;
        }

        double cost = plugin.getConfigManager().getSiegeCost();
        if (cost > 0 && plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            if (!plugin.getVaultHook().has(attacker, cost)) {
                return StartResult.NOT_ENOUGH_MONEY;
            }
            plugin.getVaultHook().withdraw(attacker, cost);
        }

        UUID siegeId = UUID.randomUUID();
        int captureTime = plugin.getConfigManager().getSiegeCaptureTime();

        Location loc = attacker.getLocation();
        Siege siege = new Siege(
                siegeId,
                attackerClan.getClanId(),
                defenderClanId,
                chunkKey,
                targetChunk.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                captureTime
        );

        // ✅ ИСПРАВЛЕНО: теперь addEvent — это метод в Siege
        siege.addEvent("Siege started by " + attacker.getName());

        addSiegeToCache(siege);
        plugin.getStorageManager().saveSiege(siege);

        Clan defenderClan = plugin.getClanManager().getClanById(defenderClanId);
        String territoryDisplay = "X:" + targetChunk.getX() + " Z:" + targetChunk.getZ();

        if (plugin.getConfigManager().isBroadcastSiege()) {
            String msg = plugin.getMessagesConfig().get("siege-started",
                    "{territory}", territoryDisplay,
                    "{clan}", attackerClan.getName());
            broadcastColored(msg);
        } else {
            if (defenderClan != null) {
                notifyClan(defenderClan, plugin.getMessagesConfig().get("siege-started",
                        "{territory}", territoryDisplay,
                        "{clan}", attackerClan.getName()));
            }
        }

        startSiegeTimer();
        return StartResult.SUCCESS;
    }

    // ==================== СТОП ОСАДЫ ====================

    public StopResult stopSiege(Player stopper, String ignored) {
        Clan clan = plugin.getClanManager().getClanByPlayer(stopper.getUniqueId());
        if (clan == null) return StopResult.NOT_IN_CLAN;

        Siege siege = getActiveSiegeByAttacker(clan.getClanId());
        if (siege == null) return StopResult.NO_ACTIVE_SIEGE;

        failSiege(siege);
        return StopResult.SUCCESS;
    }

    // ==================== УСПЕХ / ПРОВАЛ ====================

    public void succeedSiege(Siege siege) {
        siege.succeed();
        plugin.getStorageManager().saveSiege(siege);

        plugin.getTerritoryManager().transferTerritory(
                siege.getChunkKey(), siege.getAttackerClanId());

        plugin.getWarManager().addCaptureScore(
                siege.getAttackerClanId(), siege.getDefenderClanId());

        Clan attacker = plugin.getClanManager().getClanById(siege.getAttackerClanId());

        String msg = plugin.getMessagesConfig().get("siege-success",
                "{clan}", attacker != null ? attacker.getName() : "Unknown");
        broadcastColored(msg);

        removeSiegeFromCache(siege.getSiegeId());
        plugin.getStorageManager().deleteSiege(siege.getSiegeId());
    }

    public void failSiege(Siege siege) {
        siege.fail();
        plugin.getStorageManager().saveSiege(siege);

        String msg = plugin.getMessagesConfig().get("siege-failed");
        broadcastColored(msg);

        removeSiegeFromCache(siege.getSiegeId());
        plugin.getStorageManager().deleteSiege(siege.getSiegeId());
    }

    public void endAllClanSieges(UUID clanId) {
        List<UUID> siegeIds = new ArrayList<>(
                clanSieges.getOrDefault(clanId, new ArrayList<>()));
        for (UUID siegeId : siegeIds) {
            Siege siege = siegesById.get(siegeId);
            if (siege != null && siege.isActive()) {
                failSiege(siege);
            }
        }
    }

    // ==================== ТАЙМЕР ====================

    private void startSiegeTimer() {
        if (taskId != -1) return;

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Siege> activeSieges = new ArrayList<>(siegesById.values());

            for (Siege siege : activeSieges) {
                if (!siege.isActive()) continue;

                int radius = plugin.getConfigManager().getSiegeRadius();
                Location siegeLoc = new Location(
                        Bukkit.getWorld(siege.getWorldName()),
                        siege.getSiegeX(),
                        siege.getSiegeY(),
                        siege.getSiegeZ());

                if (siegeLoc.getWorld() == null) continue;

                int attackersInZone = 0;
                int defendersInZone = 0;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().getName().equals(siege.getWorldName())) continue;
                    if (p.getLocation().distance(siegeLoc) > radius) continue;

                    Clan pClan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
                    if (pClan == null) continue;

                    if (pClan.getClanId().equals(siege.getAttackerClanId())) {
                        attackersInZone++;
                    } else if (pClan.getClanId().equals(siege.getDefenderClanId())) {
                        defendersInZone++;
                    }
                }

                boolean done = siege.tick(attackersInZone, defendersInZone);

                // Отправляем action bar игрокам в зоне
                notifySiegeProgress(siege, siegeLoc, radius);

                if (done) {
                    if (siege.getStatus() == Siege.SiegeStatus.SUCCESS) {
                        succeedSiege(siege);
                    } else {
                        failSiege(siege);
                    }
                } else {
                    plugin.getStorageManager().saveSiege(siege);
                }
            }

            if (siegesById.isEmpty()) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            }

        }, 20L, 20L).getTaskId();
    }

    private void notifySiegeProgress(Siege siege, Location siegeLoc, int radius) {
        String progressBar = siege.getProgressBar();
        // ✅ ИСПРАВЛЕНО: используем правильный API для action bar
        String colored = progressBar.replace("&", "\u00A7");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getName().equals(siege.getWorldName())) continue;
            if (p.getLocation().distance(siegeLoc) > radius * 2) continue;

            Clan pClan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
            if (pClan == null) continue;

            if (pClan.getClanId().equals(siege.getAttackerClanId())
                    || pClan.getClanId().equals(siege.getDefenderClanId())) {
                // Spigot Action Bar API
                p.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacy(colored)
                );
            }
        }
    }

    // ==================== ПОИСК ====================

    public Siege getSiegeByChunk(String chunkKey) {
        UUID siegeId = siegeByChunk.get(chunkKey);
        return siegeId != null ? siegesById.get(siegeId) : null;
    }

    public Siege getActiveSiegeByAttacker(UUID attackerClanId) {
        List<UUID> siegeIds = clanSieges.getOrDefault(attackerClanId, new ArrayList<>());
        for (UUID siegeId : siegeIds) {
            Siege s = siegesById.get(siegeId);
            if (s != null && s.isActive()
                    && s.getAttackerClanId().equals(attackerClanId)) {
                return s;
            }
        }
        return null;
    }

    public Collection<Siege> getAllSieges() {
        return siegesById.values();
    }

    // ==================== УТИЛИТЫ ====================

    private void notifyClan(Clan clan, String message) {
        for (var member : clan.getMemberList()) {
            if (member.isOnline()) {
                Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                if (p != null) {
                    p.sendMessage(message.replace("&", "\u00A7"));
                }
            }
        }
    }

    private void broadcastColored(String msg) {
        plugin.getServer().broadcastMessage(msg.replace("&", "\u00A7"));
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    public enum StartResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, NOT_CLAIMED,
        OWN_TERRITORY, NOT_AT_WAR, ALREADY_SIEGED, NOT_ENOUGH_MONEY
    }

    public enum StopResult {
        SUCCESS, NOT_IN_CLAN, NO_ACTIVE_SIEGE
    }
}