package com.ever.war.storage;

import com.ever.war.EverWar;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLiteStorage {

    private final EverWar plugin;
    private Connection connection;
    private File dbFile;

    public SQLiteStorage(EverWar plugin) {
        this.plugin = plugin;
    }

    // ==================== ПОДКЛЮЧЕНИЕ ====================

    public void connect() throws SQLException {
        dbFile = new File(plugin.getDataFolder(), "everwar.db");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC драйвер не найден!", e);
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        plugin.getLogger().info("SQLite подключён: " + dbFile.getAbsolutePath());
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite соединение закрыто.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка закрытия SQLite:", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public Connection getValidConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    // ==================== СОЗДАНИЕ ТАБЛИЦ ====================

    public void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Таблица кланов
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS clans (
                    clan_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    tag TEXT NOT NULL UNIQUE,
                    leader_uuid TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    color TEXT DEFAULT '&6',
                    created_at INTEGER NOT NULL,
                    is_open INTEGER DEFAULT 0,
                    friendly_fire INTEGER DEFAULT 0,
                    public_info INTEGER DEFAULT 1,
                    total_kills INTEGER DEFAULT 0,
                    total_deaths INTEGER DEFAULT 0,
                    wars_won INTEGER DEFAULT 0,
                    wars_lost INTEGER DEFAULT 0,
                    territories_captured INTEGER DEFAULT 0,
                    allow_attack_allies INTEGER DEFAULT 0,
                    deserter INTEGER DEFAULT 0,
                    deserter_until INTEGER DEFAULT 0
                )
                """);

            // Миграции для существующих БД (безопасное добавление столбцов)
            addColumnIfNotExists(stmt, "clans", "allow_attack_allies", "INTEGER DEFAULT 0");
            addColumnIfNotExists(stmt, "clans", "deserter", "INTEGER DEFAULT 0");
            addColumnIfNotExists(stmt, "clans", "deserter_until", "INTEGER DEFAULT 0");

            // Таблица участников
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS clan_members (
                    player_uuid TEXT PRIMARY KEY,
                    clan_id TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'RECRUIT',
                    joined_at INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    power REAL DEFAULT 100.0,
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                )
                """);

            // Таблица территорий
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS territories (
                    chunk_key TEXT PRIMARY KEY,
                    clan_id TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    is_core INTEGER DEFAULT 0,
                    claimed_at INTEGER NOT NULL,
                    defense_level INTEGER DEFAULT 0,
                    hp REAL DEFAULT 1000.0,
                    max_hp REAL DEFAULT 1000.0,
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                )
                """);

            // Таблица войн
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS wars (
                    war_id TEXT PRIMARY KEY,
                    attacker_clan_id TEXT NOT NULL,
                    defender_clan_id TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PREPARATION',
                    declared_at INTEGER NOT NULL,
                    started_at INTEGER DEFAULT 0,
                    ended_at INTEGER DEFAULT 0,
                    attacker_score INTEGER DEFAULT 0,
                    defender_score INTEGER DEFAULT 0,
                    winner_clan_id TEXT DEFAULT NULL,
                    preparation_end_time INTEGER NOT NULL
                )
                """);

            // Таблица дипломатии
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS diplomacy (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clan_a TEXT NOT NULL,
                    clan_b TEXT NOT NULL,
                    relation TEXT NOT NULL DEFAULT 'NEUTRAL',
                    created_at INTEGER NOT NULL,
                    UNIQUE(clan_a, clan_b)
                )
                """);

            // Таблица осад
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sieges (
                    siege_id TEXT PRIMARY KEY,
                    attacker_clan_id TEXT NOT NULL,
                    defender_clan_id TEXT NOT NULL,
                    chunk_key TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    siege_x REAL NOT NULL,
                    siege_y REAL NOT NULL,
                    siege_z REAL NOT NULL,
                    started_at INTEGER NOT NULL,
                    capture_time INTEGER NOT NULL,
                    progress REAL DEFAULT 0.0,
                    status TEXT NOT NULL DEFAULT 'ACTIVE'
                )
                """);

            // Таблица снабжения
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS supply (
                    clan_id TEXT PRIMARY KEY,
                    food INTEGER DEFAULT 0,
                    materials INTEGER DEFAULT 0,
                    fuel INTEGER DEFAULT 0,
                    last_updated INTEGER NOT NULL,
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE
                )
                """);

            // Таблица стран
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS countries (
                    country_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    tag TEXT NOT NULL UNIQUE,
                    leader_clan_id TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    color TEXT DEFAULT '&b',
                    created_at INTEGER NOT NULL,
                    capital_chunk_key TEXT DEFAULT NULL
                )
                """);

            // Таблица кланов в стране
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS country_clans (
                    clan_id TEXT PRIMARY KEY,
                    country_id TEXT NOT NULL,
                    joined_at INTEGER NOT NULL,
                    FOREIGN KEY (clan_id) REFERENCES clans(clan_id) ON DELETE CASCADE,
                    FOREIGN KEY (country_id) REFERENCES countries(country_id) ON DELETE CASCADE
                )
                """);

            // Таблица игроков
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    language TEXT DEFAULT 'ru',
                    first_join INTEGER NOT NULL,
                    last_join INTEGER NOT NULL
                )
                """);

            plugin.getLogger().info("✓ Все таблицы SQLite созданы/проверены.");
        }
    }

    /**
     * Безопасное добавление столбца — не падает если уже есть
     */
    private void addColumnIfNotExists(Statement stmt, String table,
                                       String column, String definition) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN "
                    + column + " " + definition);
            plugin.getLogger().info("✓ Добавлен столбец " + column + " в " + table);
        } catch (SQLException ignored) {
            // Столбец уже существует — это нормально
        }
    }

    // ==================== УТИЛИТЫ ====================

    public void execute(String sql, Object... params) {
        try (PreparedStatement stmt = getValidConnection().prepareStatement(sql)) {
            setParams(stmt, params);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL ошибка: " + sql, e);
        }
    }

    public ResultSet query(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = getValidConnection().prepareStatement(sql);
        setParams(stmt, params);
        return stmt.executeQuery();
    }

    public boolean exists(String sql, Object... params) {
        try (PreparedStatement stmt = getValidConnection().prepareStatement(sql)) {
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL exists ошибка:", e);
            return false;
        }
    }

    private void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                stmt.setNull(i + 1, java.sql.Types.NULL);
            } else if (p instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (p instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (p instanceof Long l) {
                stmt.setLong(i + 1, l);
            } else if (p instanceof Double d) {
                stmt.setDouble(i + 1, d);
            } else if (p instanceof Boolean b) {
                stmt.setInt(i + 1, b ? 1 : 0);
            } else {
                stmt.setString(i + 1, p.toString());
            }
        }
    }

    public void beginTransaction() throws SQLException {
        getValidConnection().setAutoCommit(false);
    }

    public void commit() throws SQLException {
        getValidConnection().commit();
        getValidConnection().setAutoCommit(true);
    }

    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка rollback:", e);
        }
    }
}