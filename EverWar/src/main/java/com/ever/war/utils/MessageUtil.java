package com.ever.war.utils;

import com.ever.war.EverWar;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MessageUtil {

    /**
     * Отправить цветное сообщение игроку
     */
    public static void send(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage(ColorUtil.colorize(message));
        }
    }

    /**
     * Отправить сообщение из конфига
     */
    public static void sendMessage(Player player, String key) {
        EverWar plugin = EverWar.getInstance();
        String msg = plugin.getLanguageManager().get(key);
        send(player, msg);
    }

    /**
     * Отправить сообщение из конфига с плейсхолдерами
     */
    public static void sendMessage(Player player, String key, String... placeholders) {
        EverWar plugin = EverWar.getInstance();
        String msg = plugin.getLanguageManager().get(key, placeholders);
        send(player, msg);
    }

    /**
     * Отправить Action Bar
     */
    public static void sendActionBar(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacy(ColorUtil.colorize(message))
            );
        }
    }

    /**
     * Отправить Title
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        if (player != null && player.isOnline()) {
            player.sendTitle(
                    ColorUtil.colorize(title),
                    ColorUtil.colorize(subtitle),
                    fadeIn, stay, fadeOut
            );
        }
    }

    /**
     * Отправить кликабельное сообщение
     */
    public static void sendClickable(Player player, String message,
                                     String hoverText, String command) {
        if (player == null || !player.isOnline()) return;

        TextComponent component = new TextComponent(
                TextComponent.fromLegacy(ColorUtil.colorize(message)));

        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(ColorUtil.colorize(hoverText))
            ));
        }

        if (command != null && !command.isEmpty()) {
            component.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, command));
        }

        player.spigot().sendMessage(component);
    }

    /**
     * Отправить кликабельное предложение команды
     */
    public static void sendSuggest(Player player, String message,
                                   String hoverText, String suggestion) {
        if (player == null || !player.isOnline()) return;

        TextComponent component = new TextComponent(
                TextComponent.fromLegacy(ColorUtil.colorize(message)));

        if (hoverText != null) {
            component.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(ColorUtil.colorize(hoverText))
            ));
        }

        if (suggestion != null) {
            component.setClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND, suggestion));
        }

        player.spigot().sendMessage(component);
    }

    /**
     * Широковещание на весь сервер
     */
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(ColorUtil.colorize(message));
    }

    /**
     * Широковещание из конфига
     */
    public static void broadcastMessage(String key, String... placeholders) {
        EverWar plugin = EverWar.getInstance();
        String msg = plugin.getLanguageManager().get(key, placeholders);
        broadcast(msg);
    }

    /**
     * Проиграть звук игроку
     */
    public static void playSound(Player player, Sound sound) {
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    /**
     * Проиграть звук с громкостью и тоном
     */
    public static void playSound(Player player, Sound sound,
                                 float volume, float pitch) {
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Звук успеха
     */
    public static void soundSuccess(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /**
     * Звук ошибки
     */
    public static void soundError(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    /**
     * Звук клика (GUI)
     */
    public static void soundClick(Player player) {
        playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Звук войны
     */
    public static void soundWar(Player player) {
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }
}