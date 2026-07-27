package com.ever.war.commands;

import com.ever.war.EverWar;
import com.ever.war.gui.SupplyGUI;
import com.ever.war.models.Clan;
import com.ever.war.models.Supply;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SupplyCommand {

    private final EverWar plugin;

    public SupplyCommand(EverWar plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            SupplyGUI.open(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status", "статус", "info", "инфо" -> handleStatus(player);
            case "add", "добавить", "deposit", "положить" -> handleDeposit(player, args);
            case "gui", "меню" -> SupplyGUI.open(player);
            case "help", "помощь" -> sendHelp(player);
            default -> sendHelp(player);
        }
    }

    // ==================== СТАТУС ====================

    private void handleStatus(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        Supply supply = plugin.getSupplyManager().getSupply(clan.getClanId());
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        int foodReq = plugin.getConfigManager().getFoodPerWar();
        int matReq = plugin.getConfigManager().getMaterialsPerWar();

        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "      &e📦 " +
                (en ? "Clan Supply" : "Клановый склад"));
        MessageUtil.send(player, "&7" + clan.getName() + " &7[" + clan.getTag() + "]");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.send(player, "");

        // Еда
        String foodStatus = supply.getFood() >= foodReq ? "&a✓" : "&c✗";
        MessageUtil.send(player, "&6🌾 " + (en ? "Food: " : "Еда: ")
                + "&f" + supply.getFood() + " " + foodStatus
                + " &7(нужно " + foodReq + ")");

        // Материалы
        String matStatus = supply.getMaterials() >= matReq ? "&a✓" : "&c✗";
        MessageUtil.send(player, "&7⚙ " + (en ? "Materials: " : "Материалы: ")
                + "&f" + supply.getMaterials() + " " + matStatus
                + " &7(нужно " + matReq + ")");

        // Топливо
        MessageUtil.send(player, "&8⚡ " + (en ? "Fuel: " : "Топливо: ")
                + "&f" + supply.getFuel());

        MessageUtil.send(player, "");

        boolean warReady = supply.hasEnoughForWar(foodReq, matReq);
        if (warReady) {
            MessageUtil.send(player, "&a&l✓ " + (en ? "READY FOR WAR!" : "ГОТОВ К ВОЙНЕ!"));
        } else {
            MessageUtil.send(player, "&c&l✗ " + (en ? "NOT READY FOR WAR" : "НЕ ГОТОВ К ВОЙНЕ"));
            MessageUtil.send(player, "&7Соберите ресурсы для объявления войны!");
        }

        MessageUtil.send(player, "");
        MessageUtil.sendClickable(player,
                "&e📦 [Открыть GUI склада]",
                "&aОткрыть меню склада",
                "/war supply gui");
        MessageUtil.send(player, "&7Пополнить: &f/war supply add <food|materials|fuel>");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== ПОПОЛНЕНИЕ ====================

    private void handleDeposit(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        var member = clan.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canManageSupply()) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cНедостаточно прав. Нужна роль Офицер+");
            MessageUtil.soundError(player);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &c/war supply add <food|materials|fuel> [кол-во]");
            MessageUtil.send(player, "&7Возьмите ресурс в руку и напишите команду.");
            MessageUtil.send(player, "&7Пример: &f/war supply add food");
            MessageUtil.send(player, "&7С количеством: &f/war supply add food 32");
            return;
        }

        String type = args[1].toLowerCase();
        int amount = -1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) amount = 1;
            } catch (NumberFormatException ignored) {
                MessageUtil.send(player, "&8[&6EverWar&8] &cНекорректное число.");
                return;
            }
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cВозьмите ресурс в руку!");
            MessageUtil.soundError(player);
            return;
        }

        int taken = amount == -1 ? held.getAmount() : Math.min(amount, held.getAmount());
        Material mat = held.getType();
        boolean valid = false;
        String category = "";

        switch (type) {
            case "food", "еда" -> {
                if (isFood(mat)) {
                    plugin.getSupplyManager().addFood(clan.getClanId(), taken);
                    valid = true;
                    category = "еды";
                }
            }
            case "materials", "материалы", "material", "мат" -> {
                if (isMaterial(mat)) {
                    plugin.getSupplyManager().addMaterials(clan.getClanId(), taken);
                    valid = true;
                    category = "материалов";
                }
            }
            case "fuel", "топливо" -> {
                if (isFuel(mat)) {
                    plugin.getSupplyManager().addFuel(clan.getClanId(), taken);
                    valid = true;
                    category = "топлива";
                }
            }
        }

        if (!valid) {
            MessageUtil.send(player,
                    "&8[&6EverWar&8] &cПредмет &f" + mat.name()
                            + " &cне подходит как &f" + type);
            MessageUtil.send(player, "");
            MessageUtil.send(player, "&7Подсказка:");
            MessageUtil.send(player, "  &7food: &fлюбая еда (хлеб, мясо, овощи)");
            MessageUtil.send(player, "  &7materials: &fжелезо, дерево, камень, кирпичи");
            MessageUtil.send(player, "  &7fuel: &fуголь, брикеты, дерево, лава");
            MessageUtil.soundError(player);
            return;
        }

        // Убираем из инвентаря
        held.setAmount(held.getAmount() - taken);
        if (held.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }

        MessageUtil.send(player,
                "&8[&6EverWar&8] &a✓ Добавлено &f" + taken + " " + category + " &aна склад клана.");
        MessageUtil.soundSuccess(player);

        // Показываем новый статус
        Supply supply = plugin.getSupplyManager().getSupply(clan.getClanId());
        MessageUtil.send(player,
                "&7Теперь на складе: &6🌾" + supply.getFood()
                        + " &7⚙" + supply.getMaterials()
                        + " &8⚡" + supply.getFuel());
    }

    // ==================== ПРОВЕРКИ ТИПОВ ====================

    private boolean isFood(Material m) {
        return m.isEdible();
    }

    private boolean isMaterial(Material m) {
        String n = m.name();
        return n.contains("INGOT")
                || n.contains("PLANKS")
                || n.contains("STONE")
                || n.contains("BRICK")
                || n.contains("IRON")
                || n.contains("LOG")
                || n.contains("COPPER")
                || n.contains("GOLD_")
                || n.contains("DIAMOND")
                || n.contains("NETHERITE")
                || n.contains("WOOL")
                || n.equals("CLAY_BALL")
                || n.equals("FLINT")
                || n.equals("STICK");
    }

    private boolean isFuel(Material m) {
        return m == Material.COAL
                || m == Material.CHARCOAL
                || m == Material.COAL_BLOCK
                || m == Material.LAVA_BUCKET
                || m == Material.STICK
                || m == Material.BLAZE_ROD
                || m.name().contains("LOG")
                || m.name().contains("PLANKS")
                || m.name().contains("SAPLING");
    }

    // ==================== ПОМОЩЬ ====================

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&8&l━━━━━ &e📦 Снабжение &8&l━━━━━");
        MessageUtil.send(player, "");
        MessageUtil.sendClickable(player,
                "&e/war supply &7— Открыть GUI склада",
                "&aОткрыть",
                "/war supply gui");
        MessageUtil.sendClickable(player,
                "&e/war supply status &7— Статус в чате",
                "&aПоказать",
                "/war supply status");
        MessageUtil.send(player,
                "&e/war supply add <тип> [кол-во] &7— Пополнить");
        MessageUtil.send(player, "");
        MessageUtil.send(player, "&7Типы: &ffood&7, &fmaterials&7, &ffuel");
        MessageUtil.send(player, "&7Возьмите ресурс в руку и напишите команду");
        MessageUtil.send(player, "&8&l━━━━━━━━━━━━━━━━━━━━");
    }
}