package com.ever.war.gui;

import com.ever.war.EverWar;
import com.ever.war.models.Clan;
import com.ever.war.models.Supply;
import com.ever.war.utils.ItemBuilder;
import com.ever.war.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SupplyGUI {

    public static void open(Player player) {
        EverWar plugin = EverWar.getInstance();
        String lang = plugin.getConfigManager().getLanguage();
        boolean en = lang.equals("en");

        Clan clan = plugin.getClanManager().getClanByPlayer(player);
        if (clan == null) {
            MessageUtil.sendMessage(player, "not-in-clan");
            return;
        }

        Supply supply = plugin.getSupplyManager().getSupply(clan.getClanId());

        String title = en ? "§0§l📦 Supply" : "§0§l📦 Снабжение";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        inv.setItem(4, new ItemBuilder(Material.CHEST)
                .name("&e&l" + (en ? "Clan Supply" : "Клановый склад"))
                .lore("",
                        en ? "&7Resources for war" : "&7Ресурсы для войны",
                        "",
                        (en ? "&7Need for war: " : "&7Для войны нужно: ")
                                + "&f" + plugin.getConfigManager().getFoodPerWar()
                                + (en ? " food, " : " еды, ")
                                + plugin.getConfigManager().getMaterialsPerWar()
                                + (en ? " materials" : " материалов"))
                .build());

        // Еда
        int foodReq = plugin.getConfigManager().getFoodPerWar();
        boolean foodOk = supply.getFood() >= foodReq;
        inv.setItem(11, new ItemBuilder(Material.BREAD)
                .name("&6" + (en ? "Food" : "Еда") + " &7— &f" + supply.getFood())
                .lore("",
                        (en ? "&7For war: " : "&7Для войны: ")
                                + (foodOk ? "&a✓ " : "&c✗ ")
                                + "&f" + supply.getFood() + "/" + foodReq,
                        "",
                        en ? "&eDeposit food items to add" : "&eПоложите еду в инвентарь и кликните")
                .amount(Math.max(1, Math.min(64, supply.getFood())))
                .build());

        // Материалы
        int matReq = plugin.getConfigManager().getMaterialsPerWar();
        boolean matOk = supply.getMaterials() >= matReq;
        inv.setItem(13, new ItemBuilder(Material.IRON_INGOT)
                .name("&7" + (en ? "Materials" : "Материалы") + " &7— &f" + supply.getMaterials())
                .lore("",
                        (en ? "&7For war: " : "&7Для войны: ")
                                + (matOk ? "&a✓ " : "&c✗ ")
                                + "&f" + supply.getMaterials() + "/" + matReq,
                        "",
                        en ? "&eDeposit materials to add" : "&eПоложите материалы и кликните")
                .amount(Math.max(1, Math.min(64, supply.getMaterials())))
                .build());

        // Топливо
        inv.setItem(15, new ItemBuilder(Material.COAL)
                .name("&8" + (en ? "Fuel" : "Топливо") + " &7— &f" + supply.getFuel())
                .lore("",
                        en ? "&7Used for vehicles & machines" : "&7Для транспорта и машин",
                        "",
                        en ? "&eDeposit fuel to add" : "&eПоложите топливо и кликните")
                .amount(Math.max(1, Math.min(64, supply.getFuel())))
                .build());

        // Готовность к войне
        boolean warReady = supply.hasEnoughForWar(foodReq, matReq);
        inv.setItem(22, new ItemBuilder(warReady ? Material.GREEN_WOOL : Material.RED_WOOL)
                .name(warReady
                        ? "&a✓ " + (en ? "Ready for war!" : "Готовы к войне!")
                        : "&c✗ " + (en ? "Not ready for war" : "Не готовы к войне"))
                .lore("",
                        warReady
                                ? (en ? "&aAll supplies are sufficient" : "&aВсех ресурсов достаточно")
                                : (en ? "&cGather more resources!" : "&cСоберите больше ресурсов!"))
                .build());

        inv.setItem(26, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name("&c↩ " + (en ? "Back" : "Назад"))
                .build());

        player.openInventory(inv);
        MessageUtil.soundClick(player);
    }

    public static void handleClick(Player player, int slot) {
        if (slot == 26) {
            player.closeInventory();
            ClanMenuGUI.open(player);
        }
    }
}