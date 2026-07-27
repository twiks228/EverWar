package com.ever.war.managers;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Country;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CountryManager {

    private final EverWar plugin;

    // countryId -> Country
    private final Map<UUID, Country> countriesById = new HashMap<>();

    // name.lower -> countryId
    private final Map<String, UUID> countriesByName = new HashMap<>();

    // tag.lower -> countryId
    private final Map<String, UUID> countriesByTag = new HashMap<>();

    // clanId -> countryId
    private final Map<UUID, UUID> clanCountryMap = new HashMap<>();

    public CountryManager(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== КЭШ ====================

    public void addCountryToCache(Country country) {
        countriesById.put(country.getCountryId(), country);
        countriesByName.put(country.getName().toLowerCase(), country.getCountryId());
        countriesByTag.put(country.getTag().toLowerCase(), country.getCountryId());
        for (UUID clanId : country.getClanIds()) {
            clanCountryMap.put(clanId, country.getCountryId());
        }
    }

    public void removeCountryFromCache(UUID countryId) {
        Country country = countriesById.remove(countryId);
        if (country != null) {
            countriesByName.remove(country.getName().toLowerCase());
            countriesByTag.remove(country.getTag().toLowerCase());
            for (UUID clanId : country.getClanIds()) {
                clanCountryMap.remove(clanId);
            }
        }
    }

    // ==================== СОЗДАНИЕ / УДАЛЕНИЕ ====================

    public CreateResult createCountry(Player leader, String name, String tag) {
        Clan clan = plugin.getClanManager().getClanByPlayer(leader.getUniqueId());
        if (clan == null) return CreateResult.NOT_IN_CLAN;

        if (!clan.isLeader(leader.getUniqueId())) return CreateResult.NO_PERMISSION;

        if (clanCountryMap.containsKey(clan.getClanId())) return CreateResult.ALREADY_IN_COUNTRY;

        if (name.length() < 3 || name.length() > 32) return CreateResult.INVALID_NAME;
        if (tag.length() < 2 || tag.length() > 5) return CreateResult.INVALID_TAG;

        if (countriesByName.containsKey(name.toLowerCase())) return CreateResult.NAME_TAKEN;
        if (countriesByTag.containsKey(tag.toLowerCase())) return CreateResult.TAG_TAKEN;

        UUID countryId = UUID.randomUUID();
        Country country = new Country(countryId, name, tag,
                clan.getClanId(), clan.getName());

        addCountryToCache(country);
        plugin.getStorageManager().saveCountry(country);

        return CreateResult.SUCCESS;
    }

    public DeleteResult deleteCountry(Player leader) {
        Clan clan = plugin.getClanManager().getClanByPlayer(leader.getUniqueId());
        if (clan == null) return DeleteResult.NOT_IN_CLAN;

        UUID countryId = clanCountryMap.get(clan.getClanId());
        if (countryId == null) return DeleteResult.NOT_IN_COUNTRY;

        Country country = countriesById.get(countryId);
        if (country == null) return DeleteResult.COUNTRY_NOT_FOUND;

        if (!country.isLeaderClan(clan.getClanId())) return DeleteResult.NO_PERMISSION;

        removeCountryFromCache(countryId);
        plugin.getStorageManager().deleteCountry(countryId);

        return DeleteResult.SUCCESS;
    }

    // ==================== ВСТУПЛЕНИЕ / ВЫХОД ====================

    public InviteResult inviteClan(Player leader, String targetClanName) {
        Clan leaderClan = plugin.getClanManager().getClanByPlayer(leader.getUniqueId());
        if (leaderClan == null) return InviteResult.NOT_IN_CLAN;

        UUID countryId = clanCountryMap.get(leaderClan.getClanId());
        if (countryId == null) return InviteResult.NOT_IN_COUNTRY;

        Country country = countriesById.get(countryId);
        if (!country.isLeaderClan(leaderClan.getClanId())) return InviteResult.NO_PERMISSION;

        Clan target = plugin.getClanManager().getClanByName(targetClanName);
        if (target == null) return InviteResult.TARGET_NOT_FOUND;

        if (clanCountryMap.containsKey(target.getClanId())) return InviteResult.ALREADY_IN_COUNTRY;

        country.invite(target.getClanId());

        notifyClan(target, plugin.getMessagesConfig().get("country-invited",
                "{country}", country.getName()));

        return InviteResult.SUCCESS;
    }

    public JoinResult joinCountry(Player leader, String countryName) {
        Clan clan = plugin.getClanManager().getClanByPlayer(leader.getUniqueId());
        if (clan == null) return JoinResult.NOT_IN_CLAN;

        if (!clan.isLeader(leader.getUniqueId())) return JoinResult.NO_PERMISSION;

        if (clanCountryMap.containsKey(clan.getClanId())) return JoinResult.ALREADY_IN_COUNTRY;

        UUID countryId = countriesByName.get(countryName.toLowerCase());
        if (countryId == null) return JoinResult.COUNTRY_NOT_FOUND;

        Country country = countriesById.get(countryId);
        if (!country.hasInvite(clan.getClanId())) return JoinResult.NO_INVITE;

        country.addClan(clan.getClanId());
        clanCountryMap.put(clan.getClanId(), countryId);

        plugin.getStorageManager().saveCountry(country);

        return JoinResult.SUCCESS;
    }

    public LeaveResult leaveClan(Player leader) {
        Clan clan = plugin.getClanManager().getClanByPlayer(leader.getUniqueId());
        if (clan == null) return LeaveResult.NOT_IN_CLAN;

        UUID countryId = clanCountryMap.get(clan.getClanId());
        if (countryId == null) return LeaveResult.NOT_IN_COUNTRY;

        Country country = countriesById.get(countryId);
        if (country.isLeaderClan(clan.getClanId())) return LeaveResult.IS_LEADER;

        country.removeClan(clan.getClanId());
        clanCountryMap.remove(clan.getClanId());

        plugin.getStorageManager().saveCountry(country);

        return LeaveResult.SUCCESS;
    }

    // ==================== ПОИСК ====================

    public Country getCountryById(UUID countryId) {
        return countriesById.get(countryId);
    }

    public Country getCountryByName(String name) {
        UUID id = countriesByName.get(name.toLowerCase());
        return id != null ? countriesById.get(id) : null;
    }

    public Country getCountryByClan(UUID clanId) {
        UUID countryId = clanCountryMap.get(clanId);
        return countryId != null ? countriesById.get(countryId) : null;
    }

    public Collection<Country> getAllCountries() {
        return countriesById.values();
    }

    public int getCountryCount() {
        return countriesById.size();
    }

    public List<Country> getTopCountries(int limit) {
        List<Country> sorted = new ArrayList<>(countriesById.values());
        sorted.sort(Comparator.comparingInt(Country::getClanCount).reversed());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    // ==================== УТИЛИТЫ ====================

    private void notifyClan(Clan clan, String message) {
        for (var member : clan.getMemberList()) {
            if (member.isOnline()) {
                var p = plugin.getServer().getPlayer(member.getPlayerUUID());
                if (p != null) p.sendMessage(message.replace("&", "\u00A7"));
            }
        }
    }

    // ==================== РЕЗУЛЬТАТЫ ====================

    public enum CreateResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, ALREADY_IN_COUNTRY,
        INVALID_NAME, INVALID_TAG, NAME_TAKEN, TAG_TAKEN
    }

    public enum DeleteResult {
        SUCCESS, NOT_IN_CLAN, NOT_IN_COUNTRY, COUNTRY_NOT_FOUND, NO_PERMISSION
    }

    public enum InviteResult {
        SUCCESS, NOT_IN_CLAN, NOT_IN_COUNTRY, NO_PERMISSION,
        TARGET_NOT_FOUND, ALREADY_IN_COUNTRY
    }

    public enum JoinResult {
        SUCCESS, NOT_IN_CLAN, NO_PERMISSION, ALREADY_IN_COUNTRY,
        COUNTRY_NOT_FOUND, NO_INVITE
    }

    public enum LeaveResult {
        SUCCESS, NOT_IN_CLAN, NOT_IN_COUNTRY, IS_LEADER
    }
}