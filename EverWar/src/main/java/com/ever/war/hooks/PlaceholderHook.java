package com.ever.war.hooks;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.models.Country;
import com.ever.war.models.Supply;
import com.ever.war.models.War;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {

    private final EverWar plugin;

    public PlaceholderHook(EverWar plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "everwar";
    }

    @Override
    public @NotNull String getAuthor() {
        return "EverTeam";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Не удалять при перезагрузке PlaceholderAPI
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String lang = plugin.getConfigManager().getLanguage();
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

        switch (params.toLowerCase()) {

            // ========== КЛАН ==========

            case "clan":
            case "clan_name":
                return clan != null ? clan.getName() : (lang.equals("en") ? "None" : "Нет");

            case "clan_tag":
                return clan != null ? clan.getTag() : "";

            case "clan_formatted":
                return clan != null ? clan.getFormattedTag() : "";

            case "clan_color":
                return clan != null ? clan.getColor() : "&7";

            case "clan_leader":
                if (clan == null) return "";
                ClanMember leader = clan.getMember(clan.getLeaderUUID());
                return leader != null ? leader.getPlayerName() : "Unknown";

            case "clan_members":
                return clan != null ? String.valueOf(clan.getMemberCount()) : "0";

            case "clan_members_online":
                return clan != null ? String.valueOf(clan.getOnlineMembers().size()) : "0";

            case "clan_territories":
                if (clan == null) return "0";
                return String.valueOf(
                        plugin.getTerritoryManager().getClanTerritoryCount(clan.getClanId()));

            case "clan_description":
                return clan != null ? clan.getDescription() : "";

            case "clan_kills":
                return clan != null ? String.valueOf(clan.getTotalKills()) : "0";

            case "clan_deaths":
                return clan != null ? String.valueOf(clan.getTotalDeaths()) : "0";

            case "clan_wars_won":
                return clan != null ? String.valueOf(clan.getWarsWon()) : "0";

            case "clan_wars_lost":
                return clan != null ? String.valueOf(clan.getWarsLost()) : "0";

            // ========== РОЛЬ ==========

            case "role":
            case "clan_role":
                if (clan == null) return "";
                ClanMember member = clan.getMember(player.getUniqueId());
                return member != null ? member.getRole().getName(lang) : "";

            case "role_icon":
                if (clan == null) return "";
                ClanMember memberIcon = clan.getMember(player.getUniqueId());
                return memberIcon != null ? memberIcon.getRole().getIcon() : "";

            case "role_color":
                if (clan == null) return "&7";
                ClanMember memberColor = clan.getMember(player.getUniqueId());
                return memberColor != null ? memberColor.getRole().getChatColor() : "&7";

            // ========== МОЩЬ ==========

            case "power":
            case "player_power":
                return String.format("%.0f",
                        plugin.getPowerManager().getPlayerPower(player.getUniqueId()));

            case "clan_power":
                if (clan == null) return "0";
                return String.format("%.0f", clan.getTotalPower());

            // ========== СТАТИСТИКА ИГРОКА ==========

            case "kills":
                if (clan == null) return "0";
                ClanMember memberKills = clan.getMember(player.getUniqueId());
                return memberKills != null ? String.valueOf(memberKills.getKills()) : "0";

            case "deaths":
                if (clan == null) return "0";
                ClanMember memberDeaths = clan.getMember(player.getUniqueId());
                return memberDeaths != null ? String.valueOf(memberDeaths.getDeaths()) : "0";

            case "kdr":
                if (clan == null) return "0";
                ClanMember memberKdr = clan.getMember(player.getUniqueId());
                return memberKdr != null ? String.valueOf(memberKdr.getKillDeathRatio()) : "0";

            // ========== ВОЙНА ==========

            case "at_war":
                if (clan == null) return (lang.equals("en") ? "No" : "Нет");
                List<War> wars = plugin.getWarManager().getClanWars(clan.getClanId());
                return wars.isEmpty()
                        ? (lang.equals("en") ? "No" : "Нет")
                        : (lang.equals("en") ? "Yes" : "Да");

            case "war_count":
                if (clan == null) return "0";
                return String.valueOf(
                        plugin.getWarManager().getClanWars(clan.getClanId()).size());

            // ========== СНАБЖЕНИЕ ==========

            case "supply_food":
                if (clan == null) return "0";
                Supply supplyFood = plugin.getSupplyManager().getSupply(clan.getClanId());
                return String.valueOf(supplyFood.getFood());

            case "supply_materials":
                if (clan == null) return "0";
                Supply supplyMat = plugin.getSupplyManager().getSupply(clan.getClanId());
                return String.valueOf(supplyMat.getMaterials());

            case "supply_fuel":
                if (clan == null) return "0";
                Supply supplyFuel = plugin.getSupplyManager().getSupply(clan.getClanId());
                return String.valueOf(supplyFuel.getFuel());

            // ========== СТРАНА ==========

            case "country":
            case "country_name":
                if (clan == null) return (lang.equals("en") ? "None" : "Нет");
                Country country = plugin.getCountryManager().getCountryByClan(clan.getClanId());
                return country != null ? country.getName() : (lang.equals("en") ? "None" : "Нет");

            case "country_tag":
                if (clan == null) return "";
                Country countryTag = plugin.getCountryManager().getCountryByClan(clan.getClanId());
                return countryTag != null ? countryTag.getTag() : "";

            case "country_clans":
                if (clan == null) return "0";
                Country countryClans = plugin.getCountryManager().getCountryByClan(clan.getClanId());
                return countryClans != null ? String.valueOf(countryClans.getClanCount()) : "0";

            // ========== ТЕРРИТОРИЯ ==========

            case "territory":
            case "territory_owner":
                var chunk = player.getLocation().getChunk();
                var territory = plugin.getTerritoryManager().getTerritoryByChunk(chunk);
                if (territory == null) return (lang.equals("en") ? "Wilderness" : "Дикая местность");
                Clan ownerClan = plugin.getClanManager().getClanById(territory.getOwnerClanId());
                return ownerClan != null ? ownerClan.getName() : "Unknown";

            case "territory_tag":
                var chunk2 = player.getLocation().getChunk();
                var territory2 = plugin.getTerritoryManager().getTerritoryByChunk(chunk2);
                if (territory2 == null) return "";
                Clan ownerClan2 = plugin.getClanManager().getClanById(territory2.getOwnerClanId());
                return ownerClan2 != null ? ownerClan2.getTag() : "";

            // ========== РЕЙТИНГ ==========

            case "rank":
                if (clan == null) return "-";
                List<Clan> topList = plugin.getClanManager().getTopClans(100);
                for (int i = 0; i < topList.size(); i++) {
                    if (topList.get(i).getClanId().equals(clan.getClanId())) {
                        return String.valueOf(i + 1);
                    }
                }
                return "-";

            // ========== В КЛАНЕ ЛИ ==========

            case "has_clan":
                return clan != null
                        ? (lang.equals("en") ? "Yes" : "Да")
                        : (lang.equals("en") ? "No" : "Нет");

            default:
                return null;
        }
    }
}