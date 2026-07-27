package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PowerManager {

    private final EverWar plugin;

    public PowerManager(EverWar plugin) {
        this.plugin = plugin;
    }

    public void onKill(Player killer, Player victim) {
        Clan killerClan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());

        double killPower = plugin.getConfigManager().getKillPower();
        double deathPower = plugin.getConfigManager().getDeathPower();
        double maxPower = plugin.getConfigManager().getMaxPower();

        // Убийца получает мощь
        if (killerClan != null) {
            ClanMember killerMember = killerClan.getMember(killer.getUniqueId());
            if (killerMember != null) {
                killerMember.addKill();
                double newPower = Math.min(maxPower, killerMember.getPower() + killPower);
                killerMember.setPower(newPower);
                killerClan.addKill();
                killerClan.recalculatePower();
                plugin.getStorageManager().saveClanMember(killerClan.getClanId(), killerMember);
                plugin.getStorageManager().saveClan(killerClan);
            }
        }

        // Жертва теряет мощь
        if (victimClan != null) {
            ClanMember victimMember = victimClan.getMember(victim.getUniqueId());
            if (victimMember != null) {
                victimMember.addDeath();
                double newPower = Math.max(0, victimMember.getPower() - deathPower);
                victimMember.setPower(newPower);
                victimClan.addDeath();
                victimClan.recalculatePower();
                plugin.getStorageManager().saveClanMember(victimClan.getClanId(), victimMember);
                plugin.getStorageManager().saveClan(victimClan);
            }
        }

        // Очки войны
        if (killerClan != null && victimClan != null) {
            plugin.getWarManager().addKillScore(
                    killerClan.getClanId(), victimClan.getClanId());
        }
    }

    public void addPower(UUID playerUUID, double amount) {
        Clan clan = plugin.getClanManager().getClanByPlayer(playerUUID);
        if (clan == null) return;

        ClanMember member = clan.getMember(playerUUID);
        if (member == null) return;

        double maxPower = plugin.getConfigManager().getMaxPower();
        member.setPower(Math.min(maxPower, member.getPower() + amount));
        clan.recalculatePower();

        plugin.getStorageManager().saveClanMember(clan.getClanId(), member);
        plugin.getStorageManager().saveClan(clan);
    }

    public void removePower(UUID playerUUID, double amount) {
        Clan clan = plugin.getClanManager().getClanByPlayer(playerUUID);
        if (clan == null) return;

        ClanMember member = clan.getMember(playerUUID);
        if (member == null) return;

        member.setPower(Math.max(0, member.getPower() - amount));
        clan.recalculatePower();

        plugin.getStorageManager().saveClanMember(clan.getClanId(), member);
        plugin.getStorageManager().saveClan(clan);
    }

    public double getPlayerPower(UUID playerUUID) {
        Clan clan = plugin.getClanManager().getClanByPlayer(playerUUID);
        if (clan == null) return 0;
        ClanMember member = clan.getMember(playerUUID);
        return member != null ? member.getPower() : 0;
    }

    public double getClanPower(UUID clanId) {
        Clan clan = plugin.getClanManager().getClanById(clanId);
        if (clan == null) return 0;
        clan.recalculatePower();
        return clan.getTotalPower();
    }
}