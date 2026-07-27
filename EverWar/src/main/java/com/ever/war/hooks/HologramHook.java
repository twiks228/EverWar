package com.ever.war.hooks;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HologramHook {

    private final EverWar plugin;
    private boolean enabled;

    // chunkKey -> hologramId (для управления)
    private final Map<String, String> holograms = new HashMap<>();

    public HologramHook(EverWar plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            enabled = true;
            return true;
        }
        enabled = false;
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Создать голограмму территории (ядра базы)
     */
    public void createTerritoryHologram(Territory territory, Clan clan) {
        if (!isEnabled()) return;
        if (!territory.isCore()) return;

        try {
            String hologramId = "everwar_" + territory.getChunkKey().replace(":", "_");
            String lang = plugin.getConfigManager().getLanguage();

            // Координаты центра чанка
            World world = Bukkit.getWorld(territory.getWorldName());
            if (world == null) return;

            int centerX = (territory.getChunkX() << 4) + 8;
            int centerZ = (territory.getChunkZ() << 4) + 8;
            int y = world.getHighestBlockYAt(centerX, centerZ) + 3;

            Location loc = new Location(world, centerX + 0.5, y, centerZ + 0.5);

            // Линии голограммы
            String line1 = clan.getFormattedName();
            String line2;
            String line3;

            if (lang.equalsIgnoreCase("en")) {
                line2 = "&7Base Core";
                line3 = "&7HP: " + territory.getHpDisplay();
            } else {
                line2 = "&7Ядро базы";
                line3 = "&7HP: " + territory.getHpDisplay();
            }

            // Выполняем команду DecentHolograms
            String cmd = String.format(
                    "dh create %s \"%s\" \"%s\" \"%s\"",
                    hologramId,
                    colorize(line1),
                    colorize(line2),
                    colorize(line3)
            );

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "dh create " + hologramId);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "dh line add " + hologramId + " " + colorize(line1));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "dh line add " + hologramId + " " + colorize(line2));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "dh line add " + hologramId + " " + colorize(line3));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        String.format("dh move %s %s %d %d %d",
                                hologramId, territory.getWorldName(),
                                centerX, y, centerZ));
            });

            holograms.put(territory.getChunkKey(), hologramId);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка создания голограммы:", e);
        }
    }

    /**
     * Обновить голограмму территории
     */
    public void updateHologram(Territory territory, Clan clan) {
        if (!isEnabled()) return;

        String hologramId = holograms.get(territory.getChunkKey());
        if (hologramId == null) {
            if (territory.isCore()) {
                createTerritoryHologram(territory, clan);
            }
            return;
        }

        String lang = plugin.getConfigManager().getLanguage();
        String line1 = clan.getFormattedName();
        String line3 = "&7HP: " + territory.getHpDisplay();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "dh line set " + hologramId + " 1 " + colorize(line1));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "dh line set " + hologramId + " 3 " + colorize(line3));
        });
    }

    /**
     * Удалить голограмму
     */
    public void removeHologram(String chunkKey) {
        if (!isEnabled()) return;

        String hologramId = holograms.remove(chunkKey);
        if (hologramId != null) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "dh delete " + hologramId));
        }
    }

    /**
     * Удалить все голограммы EverWar
     */
    public void removeAllHolograms() {
        if (!isEnabled()) return;

        for (String hologramId : holograms.values()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "dh delete " + hologramId);
        }
        holograms.clear();
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }
}