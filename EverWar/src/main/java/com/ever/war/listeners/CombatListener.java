package com.ever.war.listeners;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.utils.MessageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

public class CombatListener implements Listener {

    private final EverWar plugin;

    public CombatListener(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== PVP ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null) return;

        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        Clan attackerClan = plugin.getClanManager().getClanByPlayer(attacker);
        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim);

        // Оба без клана — PVP разрешён
        if (attackerClan == null && victimClan == null) return;
        if (attackerClan == null || victimClan == null) return;

        // ✅ ДЕЗЕРТИР: атакующий-дезертир бьёт ВСЕХ включая союзников и клан
        if (attackerClan.isDeserter()) {
            MessageUtil.sendActionBar(attacker,
                    "&4🚨 РЕЖИМ ДЕЗЕРТИРА — атакуй всех!");
            return;
        }

        // ✅ ДЕЗЕРТИР-ЖЕРТВА: любого дезертира может атаковать кто угодно
        if (victimClan.isDeserter()) {
            MessageUtil.sendActionBar(attacker,
                    "&e⚔ Дезертир! Атакуй без пощады!");
            return;
        }

        // Тот же клан
        if (attackerClan.getClanId().equals(victimClan.getClanId())) {
            if (!attackerClan.isFriendlyFire()
                    && !plugin.getConfigManager().isFriendlyFire()) {
                event.setCancelled(true);
                MessageUtil.sendActionBar(attacker,
                        "&c❌ Атака своих отключена! /war → Настройки");
                return;
            }
            MessageUtil.sendActionBar(attacker,
                    "&e⚠ Атакуешь своего!");
            return;
        }

        // ✅ СОЮЗНИКИ: проверка настройки "разрешить атаку союзников"
        if (plugin.getDiplomacyManager().isAlly(
                attackerClan.getClanId(), victimClan.getClanId())) {

            if (attackerClan.isAllowAttackAllies()) {
                // Разрешено — атака = предательство
                MessageUtil.sendActionBar(attacker,
                        "&e⚠ ПРЕДАТЕЛЬСТВО! Вы атакуете союзника!");
                return;
            }

            event.setCancelled(true);
            MessageUtil.sendActionBar(attacker,
                    "&b❌ Нельзя атаковать союзников! /war → Настройки");
            return;
        }

        // Остальные (враги, нейтралы) — PVP разрешён
    }

    // ==================== СМЕРТЬ ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            // Смерть без убийцы
            Clan victimClan = plugin.getClanManager().getClanByPlayer(victim);
            if (victimClan != null) {
                var member = victimClan.getMember(victim.getUniqueId());
                if (member != null) {
                    member.addDeath();
                    victimClan.addDeath();
                    victimClan.recalculatePower();
                    plugin.getStorageManager().saveClanMember(
                            victimClan.getClanId(), member);
                    plugin.getStorageManager().saveClan(victimClan);
                }
            }
            return;
        }

        // Убийство игроком
        plugin.getPowerManager().onKill(killer, victim);

        Clan killerClan = plugin.getClanManager().getClanByPlayer(killer);
        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim);

        if (killerClan != null && victimClan != null
                && !killerClan.getClanId().equals(victimClan.getClanId())) {

            String lang = plugin.getConfigManager().getLanguage();
            boolean en = lang.equals("en");

            // Убийство дезертира — награда
            if (victimClan.isDeserter()) {
                MessageUtil.broadcast(
                        "&8[&6EverWar&8] &a✓ &f" + killer.getName()
                                + " &aубил дезертира &f" + victim.getName()
                                + " &7[" + victimClan.getTag() + "]");
                // Награда — +мощь
                plugin.getPowerManager().addPower(killer.getUniqueId(), 10);
                return;
            }

            // Убийство союзника (предательство)
            if (plugin.getDiplomacyManager().isAlly(
                    killerClan.getClanId(), victimClan.getClanId())) {
                MessageUtil.broadcast(
                        "&8[&6EverWar&8] &c⚠ &lПРЕДАТЕЛЬСТВО! &f"
                                + killer.getName() + " &7[" + killerClan.getTag() + "] "
                                + (en ? "killed ally" : "убил союзника") + " &f"
                                + victim.getName() + " &7[" + victimClan.getTag() + "]");
                return;
            }

            // Убийство во время войны
            if (plugin.getWarManager().areAtWar(
                    killerClan.getClanId(), victimClan.getClanId())) {

                String msg = "&8[&6EverWar&8] &c⚔ &f" + killer.getName()
                        + " &7[" + killerClan.getTag() + "] "
                        + (en ? "killed" : "убил") + " &f"
                        + victim.getName() + " &7[" + victimClan.getTag() + "]"
                        + " &7(+" + plugin.getConfigManager().getKillPoints()
                        + (en ? " war points)" : " очков войны)");

                notifyWarClans(killerClan, victimClan, msg);
            }
        }

        // Осада
        String chunkKey = com.ever.war.models.Territory.makeKey(
                victim.getWorld().getName(),
                victim.getLocation().getChunk().getX(),
                victim.getLocation().getChunk().getZ());

        var siege = plugin.getSiegeManager().getSiegeByChunk(chunkKey);

        if (siege != null && siege.isActive()) {
            if (victimClan != null
                    && victimClan.getClanId().equals(siege.getDefenderClanId())) {
                siege.setProgress(Math.min(100.0, siege.getProgress() + 5.0));
                plugin.getStorageManager().saveSiege(siege);
            }
            if (killerClan != null
                    && victimClan != null
                    && killerClan.getClanId().equals(siege.getDefenderClanId())
                    && victimClan.getClanId().equals(siege.getAttackerClanId())) {
                siege.setProgress(Math.max(0.0, siege.getProgress() - 5.0));
                plugin.getStorageManager().saveSiege(siege);
            }
        }
    }

    // ==================== УТИЛИТЫ ====================

    private Player getPlayerAttacker(Entity entity) {
        if (entity instanceof Player player) return player;

        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof Player player) return player;

        for (Entity p : entity.getPassengers()) {
            if (p instanceof Player player) return player;
        }

        return null;
    }

    private void notifyWarClans(Clan clanA, Clan clanB, String message) {
        notifyClan(clanA, message);
        notifyClan(clanB, message);
    }

    private void notifyClan(Clan clan, String message) {
        for (var member : clan.getMemberList()) {
            if (member.isOnline()) {
                Player p = plugin.getServer().getPlayer(member.getPlayerUUID());
                if (p != null) {
                    MessageUtil.send(p, message);
                }
            }
        }
    }
}