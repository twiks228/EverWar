package com.ever.war.storage;

import com.ever.war.EverWar;
import com.ever.war.models.Alliance;
import com.ever.war.models.Clan;
import com.ever.war.models.ClanMember;
import com.ever.war.models.ClanRole;
import com.ever.war.models.Country;
import com.ever.war.models.Siege;
import com.ever.war.models.Supply;
import com.ever.war.models.Territory;
import com.ever.war.models.War;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class StorageManager {

    private final EverWar plugin;
    private final SQLiteStorage db;

    public StorageManager(EverWar plugin, SQLiteStorage db) {
        this.plugin = plugin;
        this.db = db;
    }

    // ==================== ЗАГРУЗКА ====================

    public void loadAll() {
        plugin.getLogger().info("Загрузка данных из базы...");
        loadClans();
        loadTerritories();
        loadWars();
        loadDiplomacy();
        loadSieges();
        loadSupplies();
        loadCountries();
        plugin.getLogger().info("Данные загружены.");
    }

    public void saveAll() {
        plugin.getLogger().info("Сохранение данных...");
        for (Clan clan : plugin.getClanManager().getAllClans()) {
            saveClan(clan);
            for (ClanMember member : clan.getMemberList()) {
                saveClanMember(clan.getClanId(), member);
            }
        }
        for (Territory territory : plugin.getTerritoryManager().getAllTerritories()) {
            saveTerritory(territory);
        }
        for (War war : plugin.getWarManager().getAllWars()) {
            saveWar(war);
        }
        for (Supply supply : plugin.getSupplyManager().getAllSupplies()) {
            saveSupply(supply);
        }
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            saveCountry(country);
        }
        plugin.getLogger().info("Данные сохранены.");
    }

    // ==================== КЛАНЫ ====================

    public void loadClans() {
        try (ResultSet rs = db.query("SELECT * FROM clans")) {
            while (rs.next()) {
                UUID clanId = UUID.fromString(rs.getString("clan_id"));
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                UUID leaderUUID = UUID.fromString(rs.getString("leader_uuid"));
                String description = rs.getString("description");
                String color = rs.getString("color");
                long createdAt = rs.getLong("created_at");
                boolean isOpen = rs.getInt("is_open") == 1;
                boolean friendlyFire = rs.getInt("friendly_fire") == 1;
                boolean publicInfo = rs.getInt("public_info") == 1;
                int totalKills = rs.getInt("total_kills");
                int totalDeaths = rs.getInt("total_deaths");
                int warsWon = rs.getInt("wars_won");
                int warsLost = rs.getInt("wars_lost");
                int territoriesCaptured = rs.getInt("territories_captured");

                Clan clan = new Clan(clanId, name, tag, leaderUUID, description, color,
                        createdAt, isOpen, friendlyFire, publicInfo,
                        totalKills, totalDeaths, warsWon, warsLost, territoriesCaptured);

                // Новые поля — с защитой от отсутствия столбцов
                try {
                    boolean allowAttackAllies = rs.getInt("allow_attack_allies") == 1;
                    clan.setAllowAttackAllies(allowAttackAllies);
                } catch (SQLException ignored) {}

                try {
                    boolean deserter = rs.getInt("deserter") == 1;
                    long deserterUntil = rs.getLong("deserter_until");
                    clan.setDeserter(deserter, deserterUntil);
                } catch (SQLException ignored) {}

                plugin.getClanManager().addClanToCache(clan);
            }

            loadClanMembers();

            plugin.getLogger().info("Загружено кланов: "
                    + plugin.getClanManager().getClanCount());

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки кланов:", e);
        }
    }

    private void loadClanMembers() {
        try (ResultSet rs = db.query("SELECT * FROM clan_members")) {
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                UUID clanId = UUID.fromString(rs.getString("clan_id"));
                String playerName = rs.getString("player_name");
                ClanRole role = ClanRole.fromString(rs.getString("role"));
                long joinedAt = rs.getLong("joined_at");
                long lastSeen = rs.getLong("last_seen");
                int kills = rs.getInt("kills");
                int deaths = rs.getInt("deaths");
                double power = rs.getDouble("power");

                Clan clan = plugin.getClanManager().getClanById(clanId);
                if (clan != null) {
                    ClanMember member = new ClanMember(playerUUID, playerName, role,
                            joinedAt, lastSeen, kills, deaths, power);
                    clan.getMembers().put(playerUUID, member);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки участников:", e);
        }
    }

    public void saveClan(Clan clan) {
        db.execute("""
            INSERT OR REPLACE INTO clans
            (clan_id, name, tag, leader_uuid, description, color, created_at,
             is_open, friendly_fire, public_info, total_kills, total_deaths,
             wars_won, wars_lost, territories_captured,
             allow_attack_allies, deserter, deserter_until)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                clan.getClanId().toString(),
                clan.getName(),
                clan.getTag(),
                clan.getLeaderUUID().toString(),
                clan.getDescription(),
                clan.getColor(),
                clan.getCreatedAt(),
                clan.isOpen() ? 1 : 0,
                clan.isFriendlyFire() ? 1 : 0,
                clan.isPublicInfo() ? 1 : 0,
                clan.getTotalKills(),
                clan.getTotalDeaths(),
                clan.getWarsWon(),
                clan.getWarsLost(),
                clan.getTerritoriesCaptured(),
                clan.isAllowAttackAllies() ? 1 : 0,
                clan.isDeserter() ? 1 : 0,
                clan.getDeserterUntil()
        );
    }

    public void saveClanMember(UUID clanId, ClanMember member) {
        db.execute("""
            INSERT OR REPLACE INTO clan_members
            (player_uuid, clan_id, player_name, role, joined_at, last_seen, kills, deaths, power)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                member.getPlayerUUID().toString(),
                clanId.toString(),
                member.getPlayerName(),
                member.getRole().getId(),
                member.getJoinedAt(),
                member.getLastSeen(),
                member.getKills(),
                member.getDeaths(),
                member.getPower()
        );
    }

    public void deleteClan(UUID clanId) {
        db.execute("DELETE FROM clans WHERE clan_id = ?", clanId.toString());
    }

    public void deleteClanMember(UUID playerUUID) {
        db.execute("DELETE FROM clan_members WHERE player_uuid = ?",
                playerUUID.toString());
    }

    // ==================== ТЕРРИТОРИИ ====================

    public void loadTerritories() {
        try (ResultSet rs = db.query("SELECT * FROM territories")) {
            while (rs.next()) {
                String chunkKey = rs.getString("chunk_key");
                UUID clanId = UUID.fromString(rs.getString("clan_id"));
                String worldName = rs.getString("world_name");
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                boolean isCore = rs.getInt("is_core") == 1;
                long claimedAt = rs.getLong("claimed_at");
                int defenseLevel = rs.getInt("defense_level");
                double hp = rs.getDouble("hp");
                double maxHp = rs.getDouble("max_hp");

                Territory territory = new Territory(chunkKey, clanId, worldName,
                        chunkX, chunkZ, isCore, claimedAt, defenseLevel, hp, maxHp);

                plugin.getTerritoryManager().addTerritoryToCache(territory);
            }
            plugin.getLogger().info("Загружено территорий: " +
                    plugin.getTerritoryManager().getTerritoryCount());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки территорий:", e);
        }
    }

    public void saveTerritory(Territory territory) {
        db.execute("""
            INSERT OR REPLACE INTO territories
            (chunk_key, clan_id, world_name, chunk_x, chunk_z, is_core,
             claimed_at, defense_level, hp, max_hp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                territory.getChunkKey(),
                territory.getOwnerClanId().toString(),
                territory.getWorldName(),
                territory.getChunkX(),
                territory.getChunkZ(),
                territory.isCore() ? 1 : 0,
                territory.getClaimedAt(),
                territory.getDefenseLevel(),
                territory.getHp(),
                territory.getMaxHp()
        );
    }

    public void deleteTerritory(String chunkKey) {
        db.execute("DELETE FROM territories WHERE chunk_key = ?", chunkKey);
    }

    // ==================== ВОЙНЫ ====================

    public void loadWars() {
        try (ResultSet rs = db.query("SELECT * FROM wars WHERE status != 'ENDED'")) {
            while (rs.next()) {
                UUID warId = UUID.fromString(rs.getString("war_id"));
                UUID attackerId = UUID.fromString(rs.getString("attacker_clan_id"));
                UUID defenderId = UUID.fromString(rs.getString("defender_clan_id"));
                War.WarStatus status = War.WarStatus.valueOf(rs.getString("status"));
                long declaredAt = rs.getLong("declared_at");
                long startedAt = rs.getLong("started_at");
                long endedAt = rs.getLong("ended_at");
                int attackerScore = rs.getInt("attacker_score");
                int defenderScore = rs.getInt("defender_score");
                String winnerStr = rs.getString("winner_clan_id");
                UUID winner = winnerStr != null ? UUID.fromString(winnerStr) : null;
                long prepEnd = rs.getLong("preparation_end_time");

                War war = new War(warId, attackerId, defenderId, status, declaredAt,
                        startedAt, endedAt, attackerScore, defenderScore, winner, prepEnd);

                plugin.getWarManager().addWarToCache(war);
            }
            plugin.getLogger().info("Загружено войн: " + plugin.getWarManager().getWarCount());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки войн:", e);
        }
    }

    public void saveWar(War war) {
        db.execute("""
            INSERT OR REPLACE INTO wars
            (war_id, attacker_clan_id, defender_clan_id, status, declared_at,
             started_at, ended_at, attacker_score, defender_score, winner_clan_id, preparation_end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                war.getWarId().toString(),
                war.getAttackerClanId().toString(),
                war.getDefenderClanId().toString(),
                war.getStatus().name(),
                war.getDeclaredAt(),
                war.getStartedAt(),
                war.getEndedAt(),
                war.getAttackerScore(),
                war.getDefenderScore(),
                war.getWinnerClanId() != null ? war.getWinnerClanId().toString() : null,
                war.getPreparationEndTime()
        );
    }

    // ==================== ДИПЛОМАТИЯ ====================

    public void loadDiplomacy() {
        try (ResultSet rs = db.query("SELECT * FROM diplomacy")) {
            while (rs.next()) {
                UUID clanA = UUID.fromString(rs.getString("clan_a"));
                UUID clanB = UUID.fromString(rs.getString("clan_b"));
                String relation = rs.getString("relation");
                long createdAt = rs.getLong("created_at");

                Alliance alliance = new Alliance(clanA, clanB,
                        Alliance.Relation.valueOf(relation), createdAt);

                plugin.getDiplomacyManager().addAllianceToCache(alliance);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки дипломатии:", e);
        }
    }

    public void saveDiplomacy(Alliance alliance) {
        db.execute("""
            INSERT OR REPLACE INTO diplomacy (clan_a, clan_b, relation, created_at)
            VALUES (?, ?, ?, ?)
            """,
                alliance.getClanA().toString(),
                alliance.getClanB().toString(),
                alliance.getRelation().name(),
                alliance.getCreatedAt()
        );
    }

    public void deleteDiplomacy(UUID clanA, UUID clanB) {
        db.execute("""
            DELETE FROM diplomacy
            WHERE (clan_a = ? AND clan_b = ?) OR (clan_a = ? AND clan_b = ?)
            """,
                clanA.toString(), clanB.toString(),
                clanB.toString(), clanA.toString()
        );
    }

    // ==================== ОСАДЫ ====================

    public void loadSieges() {
        try (ResultSet rs = db.query("SELECT * FROM sieges WHERE status = 'ACTIVE'")) {
            while (rs.next()) {
                UUID siegeId = UUID.fromString(rs.getString("siege_id"));
                UUID attackerId = UUID.fromString(rs.getString("attacker_clan_id"));
                UUID defenderId = UUID.fromString(rs.getString("defender_clan_id"));
                String chunkKey = rs.getString("chunk_key");
                String worldName = rs.getString("world_name");
                double x = rs.getDouble("siege_x");
                double y = rs.getDouble("siege_y");
                double z = rs.getDouble("siege_z");
                long startedAt = rs.getLong("started_at");
                int captureTime = rs.getInt("capture_time");
                double progress = rs.getDouble("progress");

                Siege siege = new Siege(siegeId, attackerId, defenderId,
                        chunkKey, worldName, x, y, z, startedAt, captureTime, progress);

                plugin.getSiegeManager().addSiegeToCache(siege);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки осад:", e);
        }
    }

    public void saveSiege(Siege siege) {
        db.execute("""
            INSERT OR REPLACE INTO sieges
            (siege_id, attacker_clan_id, defender_clan_id, chunk_key, world_name,
             siege_x, siege_y, siege_z, started_at, capture_time, progress, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                siege.getSiegeId().toString(),
                siege.getAttackerClanId().toString(),
                siege.getDefenderClanId().toString(),
                siege.getChunkKey(),
                siege.getWorldName(),
                siege.getSiegeX(),
                siege.getSiegeY(),
                siege.getSiegeZ(),
                siege.getStartedAt(),
                siege.getCaptureTime(),
                siege.getProgress(),
                siege.getStatus().name()
        );
    }

    public void deleteSiege(UUID siegeId) {
        db.execute("DELETE FROM sieges WHERE siege_id = ?", siegeId.toString());
    }

    // ==================== СНАБЖЕНИЕ ====================

    public void loadSupplies() {
        try (ResultSet rs = db.query("SELECT * FROM supply")) {
            while (rs.next()) {
                UUID clanId = UUID.fromString(rs.getString("clan_id"));
                int food = rs.getInt("food");
                int materials = rs.getInt("materials");
                int fuel = rs.getInt("fuel");
                long lastUpdated = rs.getLong("last_updated");

                Supply supply = new Supply(clanId, food, materials, fuel, lastUpdated);
                plugin.getSupplyManager().addSupplyToCache(supply);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки снабжения:", e);
        }
    }

    public void saveSupply(Supply supply) {
        db.execute("""
            INSERT OR REPLACE INTO supply (clan_id, food, materials, fuel, last_updated)
            VALUES (?, ?, ?, ?, ?)
            """,
                supply.getClanId().toString(),
                supply.getFood(),
                supply.getMaterials(),
                supply.getFuel(),
                supply.getLastUpdated()
        );
    }

    // ==================== СТРАНЫ ====================

    public void loadCountries() {
        try (ResultSet rs = db.query("SELECT * FROM countries")) {
            while (rs.next()) {
                UUID countryId = UUID.fromString(rs.getString("country_id"));
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                UUID leaderClanId = UUID.fromString(rs.getString("leader_clan_id"));
                String description = rs.getString("description");
                String color = rs.getString("color");
                long createdAt = rs.getLong("created_at");
                String capitalKey = rs.getString("capital_chunk_key");

                Country country = new Country(countryId, name, tag, leaderClanId,
                        description, color, createdAt, capitalKey);

                plugin.getCountryManager().addCountryToCache(country);
            }

            try (ResultSet rs2 = db.query("SELECT * FROM country_clans")) {
                while (rs2.next()) {
                    UUID clanId = UUID.fromString(rs2.getString("clan_id"));
                    UUID countryId = UUID.fromString(rs2.getString("country_id"));
                    Country country = plugin.getCountryManager().getCountryById(countryId);
                    if (country != null) {
                        country.addClan(clanId);
                    }
                }
            }

            plugin.getLogger().info("Загружено стран: "
                    + plugin.getCountryManager().getCountryCount());

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки стран:", e);
        }
    }

    public void saveCountry(Country country) {
        db.execute("""
            INSERT OR REPLACE INTO countries
            (country_id, name, tag, leader_clan_id, description, color, created_at, capital_chunk_key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
                country.getCountryId().toString(),
                country.getName(),
                country.getTag(),
                country.getLeaderClanId().toString(),
                country.getDescription(),
                country.getColor(),
                country.getCreatedAt(),
                country.getCapitalChunkKey()
        );

        db.execute("DELETE FROM country_clans WHERE country_id = ?",
                country.getCountryId().toString());

        for (UUID clanId : country.getClanIds()) {
            db.execute("""
                INSERT OR IGNORE INTO country_clans (clan_id, country_id, joined_at)
                VALUES (?, ?, ?)
                """,
                    clanId.toString(),
                    country.getCountryId().toString(),
                    Instant.now().getEpochSecond()
            );
        }
    }

    public void deleteCountry(UUID countryId) {
        db.execute("DELETE FROM countries WHERE country_id = ?", countryId.toString());
    }

    // ==================== ИГРОКИ ====================

    public void savePlayerData(UUID uuid, String name, String language) {
        long now = Instant.now().getEpochSecond();
        db.execute("""
            INSERT INTO players (player_uuid, player_name, language, first_join, last_join)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
            player_name = excluded.player_name,
            language = excluded.language,
            last_join = excluded.last_join
            """,
                uuid.toString(), name, language, now, now
        );
    }

    public String getPlayerLanguage(UUID uuid) {
        try (ResultSet rs = db.query(
                "SELECT language FROM players WHERE player_uuid = ?", uuid.toString())) {
            if (rs.next()) {
                return rs.getString("language");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка получения языка:", e);
        }
        return plugin.getConfigManager().getLanguage();
    }
}