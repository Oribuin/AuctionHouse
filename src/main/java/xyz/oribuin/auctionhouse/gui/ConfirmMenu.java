package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConfirmMenu extends OriMenu {

    private final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);

    public ConfirmMenu(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public void open(Player player, Auction auction) {
        Gui gui = this.createGUI(player);

        List<Integer> confirmSlots = this.parseList(this.get("confirm-item.slots", List.of("0-3")));
        List<Integer> cancelSlots = this.parseList(this.get("cancel-item.slots", List.of("5-8")));

        final StringPlaceholders placeholders = StringPlaceholders.single("price", auction.getPrice());

        for (int slot : confirmSlots) {
            this.put(gui, slot, "confirm-item", player, placeholders, event -> {
                gui.close(player);
                auctionManager.buyAuction(player, auction.getId());
            });
        }

        for (int slot : cancelSlots) {
            this.put(gui, slot, "cancel-item", player, placeholders, event -> gui.close(player));
        }

        if (this.get("auction-item.enabled", true)) {
            List<Integer> auctionSlots = this.parseList(this.get("auction-item.slots", List.of("4-4")));
            gui.setItem(auctionSlots, new GuiItem(auction.getItem().clone()));
        }

        final ConfigurationSection extra = this.config.getConfigurationSection("extra-items");
        if (extra != null) {
            extra.getKeys(false).forEach(key -> this.put(gui, "extra-items." + key, player, this.getEmptyConsumer()));
        }

        gui.open(player);
    }


    @Override
    public Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{
            this.put("#0", "GUI Settings");
            this.put("gui-settings.title", "Confirm Purchase");
            this.put("gui-settings.rows", 1);

            this.put("#1", "Confirm Item");
            this.put("confirm-item.material", "LIME_STAINED_GLASS_PANE");
            this.put("confirm-item.name", "#00B4DB&lConfirm Purchase");
            this.put("confirm-item.lore", List.of(" &f| &7Click to confirm to", " &f| &7purchase this item for #00B4DB&l%price%&7!"));
            this.put("confirm-item.slots", List.of("0-3"));
            this.put("confirm-item.glow", true);

            this.put("#2", "Cancel Item");
            this.put("cancel-item.material", "RED_STAINED_GLASS_PANE");
            this.put("cancel-item.name", "#00B4DB&lCancel Purchase");
            this.put("cancel-item.lore", List.of(" &f| &7Click to cancel this", " &f| &7buying this item!"));
            this.put("cancel-item.slots", List.of("5-8"));
            this.put("cancel-item.glow", false);

            this.put("#3", "Should the item the player is buying be put in the menu?");
            this.put("auction-item.enabled", true);
            this.put("auction-item.slots", List.of("4-4"));
        }};
    }

    @Override
    public String getMenuName() {
        return "confirm_menu";
    }

    /**
     * Get an empty consumer
     *
     * @return Consumer
     */
    private Consumer<InventoryClickEvent> getEmptyConsumer() {
        return e -> {
            // Empty function
        };
    }

}
