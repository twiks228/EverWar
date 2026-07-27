package com.ever.war.storage;

import com.ever.war.EverWar;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class DataMigrator {

    private final EverWar plugin;
    private final SQLiteStorage db;

    private static final int CURRENT_VERSION = 1;

    public DataMigrator(EverWar plugin, SQLiteStorage db) {
        this.plugin = plugin;
        this.db = db;
    }

    /**
     * Проверить и применить миграции
     */
    public void migrate() {
        int version = getVersion();
        plugin.getLogger().info("Текущая версия БД: " + version);

        if (version < CURRENT_VERSION) {
            plugin.getLogger().info("Начинаем миграцию БД до версии " + CURRENT_VERSION + "...");

            try {
                db.beginTransaction();

                if (version < 1) {
                    migrateToV1();
                }
                // if (version < 2) { migrateToV2(); }

                setVersion(CURRENT_VERSION);
                db.commit();

                plugin.getLogger().info("Миграция БД завершена. Версия: " + CURRENT_VERSION);
            } catch (SQLException e) {
                db.rollback();
                plugin.getLogger().log(Level.SEVERE, "Ошибка миграции БД:", e);
            }
        }
    }

    private int getVersion() {
        try {
            db.getValidConnection().createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS db_version (version INTEGER DEFAULT 0)"
            );

            ResultSet rs = db.query("SELECT version FROM db_version LIMIT 1");
            if (rs.next()) {
                return rs.getInt("version");
            } else {
                db.execute("INSERT INTO db_version (version) VALUES (0)");
                return 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private void setVersion(int version) {
        db.execute("UPDATE db_version SET version = ?", version);
    }

    private void migrateToV1() {
        plugin.getLogger().info("Миграция v0 -> v1");
        // Базовые таблицы уже создаются в createTables()
        // Здесь можно добавить миграции для будущих версий
    }
}