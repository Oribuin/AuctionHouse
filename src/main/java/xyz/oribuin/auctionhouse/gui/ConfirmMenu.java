package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.gui.api.MenuItem;
import xyz.oribuin.auctionhouse.gui.api.PluginMenu;
import xyz.oribuin.auctionhouse.manager.AuctionManager;

public class ConfirmMenu extends PluginMenu {

    public ConfirmMenu(RosePlugin rosePlugin) {
        super(rosePlugin, "confirm-menu");
    }

    public void open(@NotNull Player player, @NotNull Auction auction) {
        final Gui gui = this.createGUI(player);
        final StringPlaceholders placeholders = StringPlaceholders.of("price", auction.getPrice());
        final CommentedConfigurationSection extras = this.config.getConfigurationSection("extra-items");
        if (extras != null) {
            extras.getKeys(false).forEach(key -> MenuItem.create(extras)
                    .path(key)
                    .placeholders(placeholders)
                    .player(player)
                    .place(gui)
            );
        }

        // Buy the auction
        MenuItem.create(this.config)
                .path("confirm-item")
                .player(player)
                .placeholders(placeholders)
                .action(event -> {
                    gui.close(player);
                    this.rosePlugin.getManager(AuctionManager.class).buyAuction(player, auction.getId());
                })
                .place(gui);

        // Cancel the auction
        MenuItem.create(this.config)
                .path("cancel-item")
                .player(player)
                .placeholders(placeholders)
                .action(event -> gui.close(player))
                .place(gui);

        // Auction Item?
        MenuItem.create(this.config)
                .path("auction-item")
                .condition(menuItem -> menuItem.getConfig().getBoolean("auction-item.enabled"))
                .placeholders(placeholders)
                .item(auction.getItem())
                .place(gui);

        gui.open(player);
    }

}
