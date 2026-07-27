package com.ever.war.hooks;

import com.ever.war.EverWar;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class LuckPermsHook {

    private final EverWar plugin;
    private LuckPerms luckPerms;
    private boolean enabled;

    // Префикс групп EverWar в LuckPerms
    private static final String GROUP_PREFIX = "everwar_";

    public LuckPermsHook(EverWar plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public boolean setup() {
        try {
            luckPerms = LuckPermsProvider.get();
            enabled = true;
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "LuckPerms API не найден:", e);
            enabled = false;
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled && luckPerms != null;
    }

    /**
     * Установить группу клана игроку.
     * Убирает все предыдущие клановые группы.
     */
    public void setGroup(Player player, String group) {
        if (!isEnabled()) return;

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            // Удаляем все предыдущие клановые группы
            user.data().clear(node ->
                    node instanceof InheritanceNode inheritance
                            && inheritance.getGroupName().startsWith(GROUP_PREFIX)
            );

            // Добавляем новую
            String fullGroup = GROUP_PREFIX + group;
            InheritanceNode node = InheritanceNode.builder(fullGroup).build();
            user.data().add(node);

            luckPerms.getUserManager().saveUser(user);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Ошибка установки LuckPerms группы для " + player.getName(), e);
        }
    }

    /**
     * Установить группу по UUID (для оффлайн игроков)
     */
    public void setGroup(UUID playerUUID, String group) {
        if (!isEnabled()) return;

        luckPerms.getUserManager().loadUser(playerUUID).thenAcceptAsync(user -> {
            if (user == null) return;

            user.data().clear(node ->
                    node instanceof InheritanceNode inheritance
                            && inheritance.getGroupName().startsWith(GROUP_PREFIX)
            );

            String fullGroup = GROUP_PREFIX + group;
            InheritanceNode node = InheritanceNode.builder(fullGroup).build();
            user.data().add(node);

            luckPerms.getUserManager().saveUser(user);
        });
    }

    /**
     * Удалить все клановые группы игрока
     */
    public void removeAllClanGroups(Player player) {
        removeAllClanGroups(player.getUniqueId());
    }

    public void removeAllClanGroups(UUID playerUUID) {
        if (!isEnabled()) return;

        luckPerms.getUserManager().loadUser(playerUUID).thenAcceptAsync(user -> {
            if (user == null) return;

            user.data().clear(node ->
                    node instanceof InheritanceNode inheritance
                            && inheritance.getGroupName().startsWith(GROUP_PREFIX)
            );

            luckPerms.getUserManager().saveUser(user);
        });
    }

    /**
     * Проверить есть ли у игрока клановая группа
     */
    public boolean hasClanGroup(Player player) {
        if (!isEnabled()) return false;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;

        return user.getNodes().stream()
                .anyMatch(node ->
                        node instanceof InheritanceNode inheritance
                                && inheritance.getGroupName().startsWith(GROUP_PREFIX)
                );
    }

    /**
     * Получить текущую клановую группу
     */
    public String getClanGroup(Player player) {
        if (!isEnabled()) return null;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return null;

        return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode inheritance
                        && inheritance.getGroupName().startsWith(GROUP_PREFIX))
                .map(node -> ((InheritanceNode) node).getGroupName())
                .findFirst()
                .orElse(null);
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}