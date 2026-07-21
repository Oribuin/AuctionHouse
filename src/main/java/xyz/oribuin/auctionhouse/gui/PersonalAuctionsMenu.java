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

public class PersonalAuctionsMenu extends PluginMenu {

    private final Map<UUID, Integer> lastPage = new HashMap<>(); // TODO

    public PersonalAuctionsMenu(RosePlugin rosePlugin) {
        super(rosePlugin, "my-auctions");
    }

    public void open(Player player) {
        PaginatedGui gui = this.createPagedGUI(player);
        CommentedConfigurationSection section = this.config.getConfigurationSection("extra-items");
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
                .action(event -> this.setAuctions(gui, (Player) event.getWhoClicked()))
                .place(gui);

        // Add gui navigation buttons
        MenuManager menuManager = this.rosePlugin.getManager(MenuManager.class);
        MenuItem.create(this.config)
                .path("sold-auctions")
                .player(player)
                .action(event -> menuManager.get(SoldAuctionsMenu.class).open((Player) event.getWhoClicked()))
                .place(gui);

        MenuItem.create(this.config)
                .path("expired-auctions")
                .player(player)
                .action(event -> menuManager.get(ExpiredAuctionsMenu.class).open((Player) event.getWhoClicked()))
                .place(gui);

        MenuItem.create(this.config)
                .path("main-menu")
                .player(player)
                .action(event -> menuManager.get(MainAuctionsMenu.class).open((Player) event.getWhoClicked()))
                .place(gui);

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

        AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        List<String> auctionLore = new ArrayList<>(this.config.getStringList("auction-settings.lore"));

        this.async(() -> {
            List<Auction> auctions = new ArrayList<>(manager.getActive(player.getUniqueId()));

            for (Auction auction : auctions) {
                if (auction.isExpired()) {
                    manager.expire(auction);
                    return;
                }

                ItemStack itemStack = auction.getItem().clone();
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta == null) return;

                List<String> itemLore = new ArrayList<>(itemMeta.getLore() != null ? itemMeta.getLore() : List.of());
                String timeLeft = AuctionUtils.formatTime(auction.getTimeLeft());

                StringPlaceholders placeholders = StringPlaceholders.builder()
                        .add("price", AuctionUtils.formatCurrency(auction.getPrice()))
                        .add("seller", "You")
                        .add("time", timeLeft.equals("0") ? "Expired" : timeLeft)
                        .build();

                auctionLore.forEach(line -> itemLore.add(locale.format(player, line, placeholders)));
                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);

                gui.addItem(new GuiItem(itemStack, event -> {
                    if (auction.isExpired())
                        return;

                    manager.expire(auction);
                    this.setAuctions(gui, (Player) event.getWhoClicked());
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
