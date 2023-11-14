package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.auctionhouse.gui.api.MenuItem;
import xyz.oribuin.auctionhouse.gui.api.PluginMenu;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;
import xyz.oribuin.auctionhouse.util.ItemBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SellerMenu extends PluginMenu {

    private final Map<UUID, Integer> lastPage = new HashMap<>(); // TODO

    public SellerMenu(RosePlugin rosePlugin) {
        super(rosePlugin, "seller-menu");
    }

    public void open(Player player) {
        final PaginatedGui gui = this.createPagedGUI(player);
        final CommentedConfigurationSection section = this.config.getConfigurationSection("extra-items");
        if (section != null) {
            section.getKeys(false).forEach(key -> MenuItem.create(section)
                    .path(key)
                    .player(player)
                    .place(gui)
            );
        }

        // Add Page Buttons
        this.addPageButtons(gui, player);

        MenuItem.create(this.config)
                .path("refresh-menu")
                .player(player)
                .action(event -> this.setSellers(gui, player))
                .place(gui);

        // Add gui navigation buttons
        final MenuManager menuManager = this.rosePlugin.getManager(MenuManager.class);
        MenuItem.create(this.config)
                .path("sold-auctions")
                .player(player)
                .action(event -> menuManager.get(SoldAuctionsMenu.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("expired-auctions")
                .player(player)
                .action(event -> menuManager.get(ExpiredAuctionsMenu.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("main-menu")
                .player(player)
                .action(event -> menuManager.get(MainAuctionsMenu.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("my-auctions")
                .player(player)
                .action(event -> menuManager.get(PersonalAuctionsMenu.class).open(player))
                .place(gui);

        this.setSellers(gui, player);
        gui.open(player);
    }

    /**
     * Load all the active sellers into the gui and add them
     *
     * @param gui    the gui
     * @param player the player
     */
    private void setSellers(final PaginatedGui gui, final Player player) {
        gui.clearPageItems();

        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);

        this.async(() -> {
            final List<OfflinePlayer> sellers = new ArrayList<>(manager.getActiveSellers());
            sellers.sort(Comparator.comparing(OfflinePlayer::getName));

            for (OfflinePlayer seller : sellers) {
                ItemStack itemStack = AuctionUtils.deserialize(
                        this.config,
                        player,
                        "seller-item",
                        StringPlaceholders.of("seller", seller.getName())
                );

                if (itemStack == null) {
                    continue;
                }

                itemStack = new ItemBuilder(itemStack)
                        .owner(seller)
                        .build();

                gui.addItem(new GuiItem(itemStack, event -> {
                    final MenuManager menuManager = this.rosePlugin.getManager(MenuManager.class);
                    menuManager.get(ViewMenu.class).open(player, seller);
                }));

            }

            gui.update();
            this.updateTitle(gui);
        });
    }

    /**
     * Add all the navigational items to the GUI
     *
     * @param gui    The GUI to add the items to
     * @param player The player to add the items for
     */
    private void addPageButtons(PaginatedGui gui, Player player) {
        MenuItem.create(this.config)
                .path("previous-page")
                .placeholders(this.getPagePlaceholders(gui))
                .player(player)
                .action(event -> {
                    if (gui.previous()) {
                        this.updateTitle(gui);
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("next-page")
                .placeholders(this.getPagePlaceholders(gui))
                .player(player)
                .action(event -> {
                    if (gui.next()) {
                        this.updateTitle(gui);
                    }
                })
                .place(gui);

        gui.update();
    }

}
