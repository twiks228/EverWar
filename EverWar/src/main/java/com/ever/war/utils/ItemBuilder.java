package com.ever.war.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    /**
     * Установить название
     */
    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        return this;
    }

    /**
     * Установить описание (lore)
     */
    public ItemBuilder lore(String... lines) {
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);
        }
        return this;
    }

    /**
     * Установить описание из списка
     */
    public ItemBuilder lore(List<String> lines) {
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);
        }
        return this;
    }

    /**
     * Добавить строку в описание
     */
    public ItemBuilder addLore(String line) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ColorUtil.colorize(line));
            meta.setLore(lore);
        }
        return this;
    }

    /**
     * Добавить несколько строк в описание
     */
    public ItemBuilder addLore(String... lines) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(ColorUtil.colorize(line));
            }
            meta.setLore(lore);
        }
        return this;
    }

    /**
     * Установить количество
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * Добавить зачарование
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    /**
     * Добавить свечение (зачарование без текста)
     */
    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * Скрыть атрибуты
     */
    public ItemBuilder hideAttributes() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        return this;
    }

    /**
     * Скрыть все флаги
     */
    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * Добавить флаг
     */
    public ItemBuilder addFlag(ItemFlag flag) {
        if (meta != null) {
            meta.addItemFlags(flag);
        }
        return this;
    }

    /**
     * Сделать неразрушаемым
     */
    public ItemBuilder unbreakable() {
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        return this;
    }

    /**
     * Установить Custom Model Data
     */
    public ItemBuilder customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    /**
     * Создать голову игрока
     */
    public static ItemBuilder playerHead(OfflinePlayer player) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        if (builder.meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
        }
        return builder;
    }

    /**
     * Создать декоративное стекло
     */
    public static ItemStack filler(Material glass) {
        return new ItemBuilder(glass)
                .name(" ")
                .hideFlags()
                .build();
    }

    /**
     * Стандартный филлер (серое стекло)
     */
    public static ItemStack filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * Собрать ItemStack
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}