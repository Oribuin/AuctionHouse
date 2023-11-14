package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.auction.SortType;
import xyz.oribuin.auctionhouse.gui.api.MenuItem;
import xyz.oribuin.auctionhouse.gui.api.PluginMenu;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainAuctionsMenu extends PluginMenu {

    private final Map<UUID, Integer> lastPage = new HashMap<>(); // TODO
    private final Map<UUID, SortType> lastSort = new HashMap<>();

    public MainAuctionsMenu(RosePlugin rosePlugin) {
        super(rosePlugin, "main-menu");
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
                .action(event -> this.setAuctions(gui, player))
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
                .path("my-auctions")
                .player(player)
                .action(event -> menuManager.get(PersonalAuctionsMenu.class).open(player))
                .place(gui);

        this.addSort(gui, player);
        this.setAuctions(gui, player);
        gui.open(player);
    }

    /**
     * Load all the auctions into the gui and add them
     *
     * @param gui    the gui
     * @param player the player
     */
    @SuppressWarnings("deprecation")
    private void setAuctions(final PaginatedGui gui, final Player player) {
        gui.clearPageItems();

        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final List<String> auctionLore = new ArrayList<>(
                player.hasPermission("auctionhouse.admin")
                        ? this.config.getStringList("auction-settings.admin-lore")
                        : this.config.getStringList("auction-settings.lore")
        );

        this.async(() -> {
            final List<Auction> auctions = new ArrayList<>(manager.getActive());
            final SortType sortType = this.lastSort.getOrDefault(player.getUniqueId(), SortType.TIME_ASCENDING);

            auctions.sort(sortType.getComparator());

            for (Auction auction : auctions) {
                if (auction.isExpired()) {
                    manager.expire(auction);
                    return;
                }

                ItemStack itemStack = auction.getItem().clone();
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta == null) return;

                final List<String> itemLore = new ArrayList<>(itemMeta.getLore() != null ? itemMeta.getLore() : List.of());
                final String timeLeft = AuctionUtils.formatTime(auction.getTimeLeft());

                final StringPlaceholders placeholders = StringPlaceholders.builder()
                        .add("price", AuctionUtils.formatCurrency(auction.getPrice()))
                        .add("seller", auction.getSellerPlayer().getName())
                        .add("time", timeLeft.equals("0") ? "Expired" : timeLeft)
                        .build();

                auctionLore.forEach(line -> itemLore.add(locale.format(player, line, placeholders)));
                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);

                gui.addItem(new GuiItem(itemStack, event -> {

                    // Admin commands
                    if (event.isShiftClick() && player.hasPermission("auctionhouse.admin")) {
                        if (event.isLeftClick()) {
                            manager.expire(auction);
                        } else if (event.isRightClick()) {
                            manager.delete(auction);

                            // Give the player the item back
                            player.getInventory().addItem(auction.getItem());
                        }

                        // Update the GUI
                        this.setAuctions(gui, player);
                        return;
                    }

                    // Stop the player from buying it if they are the seller
                    if (player.getUniqueId() == auction.getSeller()) {
                        gui.close(player);
                        locale.sendMessage(player, "command-buy-own-auction");
                        return;
                    }

                    gui.close(player);

                    // Buy the auction
                    this.rosePlugin.getManager(MenuManager.class)
                            .get(ConfirmMenu.class)
                            .open(player, auction);
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
                        this.lastPage.put(player.getUniqueId(), gui.getCurrentPageNum());
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
                        this.lastPage.put(player.getUniqueId(), gui.getCurrentPageNum());
                    }
                })
                .place(gui);

        gui.update();
    }

    /**
     * Add the option to cycle through the different sort types
     *
     * @param gui    The GUI
     * @param player The player
     */
    private void addSort(PaginatedGui gui, Player player) {
        final SortType sort = this.lastSort.getOrDefault(player.getUniqueId(), SortType.TIME_ASCENDING);

        MenuItem.create(this.config)
                .path("sort-item")
                .placeholders(StringPlaceholders.of("sort", sort.getDisplayName()))
                .player(player)
                .action(event -> {
                    SortType newSort = sort;

                    switch (sort) {
                        case TIME_ASCENDING -> newSort = SortType.TIME_DESCENDING;
                        case TIME_DESCENDING -> newSort = SortType.PRICE_ASCENDING;
                        case PRICE_ASCENDING -> newSort = SortType.PRICE_DESCENDING;
                        case PRICE_DESCENDING -> newSort = SortType.TIME_ASCENDING;
                    }

                    this.lastSort.put(player.getUniqueId(), newSort);
                    this.addSort(gui, player);
                    this.setAuctions(gui, player);
                })
                .place(gui);

        gui.update();
    }

}
