package com.ever.war.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    // Паттерн для HEX цветов: &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Конвертировать & коды и HEX в цвета Minecraft
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        // Обработка HEX цветов
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                ChatColor color = ChatColor.of("#" + hex);
                matcher.appendReplacement(sb, color.toString());
            } catch (Exception e) {
                // Если невалидный HEX — оставляем как есть
            }
        }
        matcher.appendTail(sb);

        // Обработка стандартных & кодов
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /**
     * Убрать все цветовые коды из строки
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    /**
     * Градиент текст (от цвета A к цвету B)
     */
    public static String gradient(String text, String hexStart, String hexEnd) {
        if (text == null || text.isEmpty()) return "";

        int[] startRGB = hexToRGB(hexStart);
        int[] endRGB = hexToRGB(hexEnd);

        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / Math.max(1, length - 1);
            int r = (int) (startRGB[0] + (endRGB[0] - startRGB[0]) * ratio);
            int g = (int) (startRGB[1] + (endRGB[1] - startRGB[1]) * ratio);
            int b = (int) (startRGB[2] + (endRGB[2] - startRGB[2]) * ratio);

            String hex = String.format("#%02x%02x%02x", r, g, b);
            try {
                result.append(ChatColor.of(hex));
            } catch (Exception e) {
                // fallback
            }
            result.append(text.charAt(i));
        }

        return result.toString();
    }

    /**
     * Конвертировать HEX в RGB массив
     */
    private static int[] hexToRGB(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    /**
     * Создать полоску прогресса
     */
    public static String progressBar(double current, double max,
                                     int length, String filled, String empty,
                                     String filledColor, String emptyColor) {
        double percent = current / max;
        int filledCount = (int) (percent * length);
        int emptyCount = length - filledCount;

        StringBuilder bar = new StringBuilder();
        bar.append(filledColor);
        for (int i = 0; i < filledCount; i++) bar.append(filled);
        bar.append(emptyColor);
        for (int i = 0; i < emptyCount; i++) bar.append(empty);

        return colorize(bar.toString());
    }

    /**
     * Стандартная полоска здоровья
     */
    public static String healthBar(double current, double max) {
        return progressBar(current, max, 10, "█", "░", "&a", "&7");
    }

    /**
     * Цвет по проценту (зелёный -> жёлтый -> красный)
     */
    public static String getPercentColor(double percent) {
        if (percent > 75) return "&a";
        if (percent > 50) return "&e";
        if (percent > 25) return "&6";
        return "&c";
    }
}