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

public class ExpiredAuctionsMenu extends PluginMenu {

    private final Map<UUID, Integer> lastPage = new HashMap<>(); // TODO

    public ExpiredAuctionsMenu(RosePlugin rosePlugin) {
        super(rosePlugin, "expired-auctions");
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
                .path("main-menu")
                .player(player)
                .action(event -> menuManager.get(MainAuctionsMenu.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("my-auctions")
                .player(player)
                .action(event -> menuManager.get(PersonalAuctionsMenu.class).open(player))
                .place(gui);

        this.setAuctions(gui, player);

        gui.open(player);
    }

    @SuppressWarnings("deprecation")
    private void setAuctions(final PaginatedGui gui, final Player player) {
        gui.clearPageItems();

        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final List<String> auctionLore = new ArrayList<>(this.config.getStringList("auction-settings.lore"));

        this.async(() -> {
            for (Auction auction : manager.getExpired(player.getUniqueId())) {
                ItemStack itemStack = auction.getItem().clone();
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta == null) return;

                final List<String> itemLore = new ArrayList<>(itemMeta.getLore() != null ? itemMeta.getLore() : List.of());
                final StringPlaceholders placeholders = StringPlaceholders.of(
                        "price", AuctionUtils.formatCurrency(auction.getPrice()),
                        "expired", AuctionUtils.getDate(auction.getExpiredTime())
                );

                auctionLore.forEach(line -> itemLore.add(locale.format(player, line, placeholders)));
                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);

                gui.addItem(new GuiItem(itemStack, event -> {
                    if (event.getWhoClicked().getInventory().firstEmpty() == -1)
                        return;

                    // Delete the auction
                    manager.delete(auction, x -> {
                        event.getWhoClicked().getInventory().addItem(auction.getItem());
                        this.setAuctions(gui, player);
                    });
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
