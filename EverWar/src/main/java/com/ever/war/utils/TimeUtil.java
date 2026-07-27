package com.ever.war.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Секунды в читаемый формат (1ч 30м 15с)
     */
    public static String formatSeconds(long seconds) {
        if (seconds <= 0) return "0с";

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("с");

        return sb.toString().trim();
    }

    /**
     * Секунды в читаемый формат (EN)
     */
    public static String formatSecondsEn(long seconds) {
        if (seconds <= 0) return "0s";

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }

    /**
     * Автоопределение языка
     */
    public static String formatTime(long seconds, String lang) {
        return lang.equalsIgnoreCase("en")
                ? formatSecondsEn(seconds)
                : formatSeconds(seconds);
    }

    /**
     * Unix timestamp в дату
     */
    public static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
        return dateTime.format(DATE_FORMAT);
    }

    /**
     * Сколько прошло с timestamp (русский)
     */
    public static String timeAgo(long timestamp) {
        long now = Instant.now().getEpochSecond();
        long diff = now - timestamp;

        if (diff < 60) return diff + " сек. назад";
        if (diff < 3600) return (diff / 60) + " мин. назад";
        if (diff < 86400) return (diff / 3600) + " ч. назад";
        return (diff / 86400) + " дн. назад";
    }

    /**
     * Сколько прошло (EN)
     */
    public static String timeAgoEn(long timestamp) {
        long now = Instant.now().getEpochSecond();
        long diff = now - timestamp;

        if (diff < 60) return diff + "s ago";
        if (diff < 3600) return (diff / 60) + "m ago";
        if (diff < 86400) return (diff / 3600) + "h ago";
        return (diff / 86400) + "d ago";
    }

    /**
     * Авто-язык
     */
    public static String timeAgo(long timestamp, String lang) {
        return lang.equalsIgnoreCase("en")
                ? timeAgoEn(timestamp)
                : timeAgo(timestamp);
    }

    /**
     * Текущий unix timestamp
     */
    public static long now() {
        return Instant.now().getEpochSecond();
    }

    /**
     * Короткий формат MM:SS
     */
    public static String formatMinSec(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}