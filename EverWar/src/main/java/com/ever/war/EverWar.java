package com.ever.war;

import com.ever.war.commands.WarCommand;
import com.ever.war.config.ConfigManager;
import com.ever.war.config.LanguageManager;
import com.ever.war.config.MessagesConfig;
import com.ever.war.config.SettingsConfig;
import com.ever.war.hooks.HologramHook;
import com.ever.war.hooks.LuckPermsHook;
import com.ever.war.hooks.PlaceholderHook;
import com.ever.war.hooks.VaultHook;
import com.ever.war.listeners.BlockListener;
import com.ever.war.listeners.CombatListener;
import com.ever.war.listeners.GUIListener;
import com.ever.war.listeners.PlayerListener;
import com.ever.war.listeners.TerritoryListener;
import com.ever.war.managers.ClanManager;
import com.ever.war.managers.CountryManager;
import com.ever.war.managers.DiplomacyManager;
import com.ever.war.managers.PowerManager;
import com.ever.war.managers.SiegeManager;
import com.ever.war.managers.SupplyManager;
import com.ever.war.managers.TerritoryManager;
import com.ever.war.managers.WarManager;
import com.ever.war.storage.SQLiteStorage;
import com.ever.war.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class EverWar extends JavaPlugin {

    // Singleton
    private static EverWar instance;

    // Storage
    private SQLiteStorage sqliteStorage;
    private StorageManager storageManager;

    // Config
    private ConfigManager configManager;
    private MessagesConfig messagesConfig;
    private SettingsConfig settingsConfig;
    private LanguageManager languageManager;

    // Managers
    private ClanManager clanManager;
    private TerritoryManager territoryManager;
    private WarManager warManager;
    private SiegeManager siegeManager;
    private DiplomacyManager diplomacyManager;
    private PowerManager powerManager;
    private SupplyManager supplyManager;
    private CountryManager countryManager;

    // Hooks
    private VaultHook vaultHook;
    private PlaceholderHook placeholderHook;
    private LuckPermsHook luckPermsHook;
    private HologramHook hologramHook;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║        EverWar Loading        ║");
        getLogger().info("╚══════════════════════════════╝");

        // 1. Конфиги
        if (!initConfigs()) {
            getLogger().severe("Ошибка загрузки конфигов! Плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. База данных
        if (!initStorage()) {
            getLogger().severe("Ошибка подключения к базе данных! Плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Менеджеры
        initManagers();

        // 4. Хуки для других плагинов
        initHooks();

        // 5. Команды
        initCommands();

        // 6. Листенеры
        initListeners();

        // 7. Загрузка данных
        loadData();

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║      EverWar успешно запущен  ║");
        getLogger().info("║      Версия: " + getDescription().getVersion() + "              ║");
        getLogger().info("╚══════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        getLogger().info("EverWar: Сохранение данных...");

        // Сохраняем все данные
        if (storageManager != null) {
            storageManager.saveAll();
        }

        // Закрываем соединение с БД
        if (sqliteStorage != null) {
            sqliteStorage.close();
        }

        getLogger().info("EverWar: Плагин выключен.");
    }

    // ==================== ИНИЦИАЛИЗАЦИЯ ====================

    private boolean initConfigs() {
        try {
            // Сохраняем дефолтные конфиги если их нет
            saveDefaultConfig();
            saveResource("messages_ru.yml", false);
            saveResource("messages_en.yml", false);

            configManager = new ConfigManager(this);
            configManager.load();

            languageManager = new LanguageManager(this);
            languageManager.load();

            settingsConfig = new SettingsConfig(this);
            settingsConfig.load();

            messagesConfig = new MessagesConfig(this);
            messagesConfig.load();

            getLogger().info("✓ Конфиги загружены. Язык: " + configManager.getLanguage());
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Ошибка загрузки конфигов:", e);
            return false;
        }
    }

    private boolean initStorage() {
        try {
            sqliteStorage = new SQLiteStorage(this);
            sqliteStorage.connect();
            sqliteStorage.createTables();

            storageManager = new StorageManager(this, sqliteStorage);

            getLogger().info("✓ База данных подключена (SQLite).");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Ошибка подключения к БД:", e);
            return false;
        }
    }

    private void initManagers() {
        clanManager = new ClanManager(this);
        territoryManager = new TerritoryManager(this);
        diplomacyManager = new DiplomacyManager(this);
        warManager = new WarManager(this);
        siegeManager = new SiegeManager(this);
        powerManager = new PowerManager(this);
        supplyManager = new SupplyManager(this);
        countryManager = new CountryManager(this);

        getLogger().info("✓ Менеджеры инициализированы.");
    }

    private void initHooks() {
        // Vault
        vaultHook = new VaultHook(this);
        if (vaultHook.setup()) {
            getLogger().info("✓ Vault подключён.");
        } else {
            getLogger().warning("✗ Vault не найден. Экономика отключена.");
        }

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = new PlaceholderHook(this);
            placeholderHook.register();
            getLogger().info("✓ PlaceholderAPI подключён.");
        } else {
            getLogger().warning("✗ PlaceholderAPI не найден.");
        }

        // LuckPerms
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook(this);
            luckPermsHook.setup();
            getLogger().info("✓ LuckPerms подключён.");
        } else {
            getLogger().warning("✗ LuckPerms не найден.");
        }

        // DecentHolograms
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            hologramHook = new HologramHook(this);
            hologramHook.setup();
            getLogger().info("✓ DecentHolograms подключён.");
        } else {
            getLogger().warning("✗ DecentHolograms не найден.");
        }
    }

    private void initCommands() {
        WarCommand warCommand = new WarCommand(this);
        getCommand("war").setExecutor(warCommand);
        getCommand("war").setTabCompleter(warCommand);
        getLogger().info("✓ Команды зарегистрированы.");
    }

    private void initListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getLogger().info("✓ Листенеры зарегистрированы.");
    }

    private void loadData() {
        storageManager.loadAll();
        getLogger().info("✓ Данные загружены.");
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ====================

    public void reload() {
        // Сохраняем перед перезагрузкой
        storageManager.saveAll();

        // Перезагружаем конфиги
        reloadConfig();
        configManager.load();
        languageManager.load();
        settingsConfig.load();
        messagesConfig.load();

        // Перезагружаем данные
        storageManager.loadAll();

        getLogger().info("EverWar перезагружен.");
    }

    // ==================== GETTERS ====================

    public static EverWar getInstance() {
        return instance;
    }

    public SQLiteStorage getSqliteStorage() {
        return sqliteStorage;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public SettingsConfig getSettingsConfig() {
        return settingsConfig;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public SiegeManager getSiegeManager() {
        return siegeManager;
    }

    public DiplomacyManager getDiplomacyManager() {
        return diplomacyManager;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public SupplyManager getSupplyManager() {
        return supplyManager;
    }

    public CountryManager getCountryManager() {
        return countryManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public HologramHook getHologramHook() {
        return hologramHook;
    }
}