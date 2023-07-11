package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.auction.AuctionSort;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;
import xyz.oribuin.auctionhouse.util.ItemBuilder;
import xyz.oribuin.auctionhouse.util.PluginUtils;
import xyz.oribuin.auctionhouse.util.SortOption;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainAuctionMenu extends OriMenu {

    private final MenuManager menuManager = this.rosePlugin.getManager(MenuManager.class);
    private final Map<UUID, SortOption> sortMap = new HashMap<>();
    private AuctionSort defaultSort;

    public MainAuctionMenu(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public void open(Player player) {
        this.defaultSort = AuctionSort.match(this.get("sort-auctions.default", null)).orElse(AuctionSort.TIME_ASCENDING);
        boolean shouldScroll = this.get("gui-settings.should-scroll", false);
        ScrollType scrollType = ScrollType.valueOf(this.get("gui-settings.scroll-type", "HORIZONTAL"));
        final PaginatedGui gui = shouldScroll ? this.createScrollingGui(player, scrollType) : this.createPagedGUI(player);

        final ItemStack item = PluginUtils.getItemStack(this.config, "border-item", player, StringPlaceholders.empty());
        List<Integer> borderSlots = this.parseList(this.get("gui-settings.border-slots", List.of("35-54")));
        gui.setItem(borderSlots, new GuiItem(item));

        this.sortMap.putIfAbsent(player.getUniqueId(), new SortOption(this.defaultSort));
        this.setSort(gui, player);
        this.put(gui, "next-page", player, event -> {
            gui.next();
            gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui)));
        });

        this.put(gui, "refresh-menu", player, event -> this.setAuctions(gui, player));
        this.put(gui, "previous-page", player, event -> {
            gui.previous();
            gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui)));
        });

        this.put(gui, "auction-sellers", player, event -> this.menuManager.get(SellerMenu.class).open(player));

        this.put(gui, "sold-auctions", player, event -> this.menuManager.get(SoldAuctionsMenu.class).open(player));
        this.put(gui, "expired-auctions", player, event -> this.menuManager.get(ExpiredAuctionsMenu.class).open(player));
        this.put(gui, "my-auctions", player, event -> this.menuManager.get(PersonalAuctionsMenu.class).open(player));

        this.setAuctions(gui, player);

        gui.open(player);
        this.sync(() -> gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui))));
    }

    /**
     * Set the auctions for the gui
     *
     * @param gui    Gui
     * @param player Player
     */
    private void setAuctions(PaginatedGui gui, Player player) {

        final AuctionSort sort = this.sortMap.get(player.getUniqueId()).getSort();
        final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);

        List<String> configLore = player.hasPermission("auctionhouse.admin")
                ? this.get("admin-auction-lore", List.of("Missing option admin-auction-lore in /menus/main_menu.yml"))
                : this.get("auction-lore", List.of("Missing option auction-lore in /menus/main_menu.yml"));

        boolean loreBefore = this.get("lore-before", false);
        gui.clearPageItems(true);

        final List<Auction> auctions = new ArrayList<>(auctionManager.getActiveActions());
        if (sort.getComparator() != null) {
            auctions.sort(sort.getComparator());
        }

        this.async(() -> {
            auctions.forEach(value -> {

                if (auctionManager.isAuctionExpired(value)) {
                    auctionManager.expireAuction(value);
                    return;
                }

                ItemStack baseItem = value.getItem().clone();
                final ItemMeta meta = baseItem.getItemMeta();
                if (meta == null) {
                    return;
                }

                List<String> lore = new ArrayList<>();

                if (loreBefore) {
                    lore.addAll(configLore);
                }

                if (meta.getLore() != null) {
                    lore.addAll(meta.getLore());
                }

                if (!loreBefore) {
                    lore.addAll(configLore);
                }

                final String timeLeft = PluginUtils.formatTime(auctionManager.getTimeLeft(value));
                final String formattedTime = timeLeft.equals("0") ? "Expired" : timeLeft;

                final StringPlaceholders auctionPls = StringPlaceholders.builder()
                        .addPlaceholder("price", PluginUtils.formatCurrency(value.getPrice()))
                        .addPlaceholder("seller", Bukkit.getOfflinePlayer(value.getSeller()).getName())
                        .addPlaceholder("time", formattedTime)
                        .build();


                lore = lore.stream().map(s -> PluginUtils.format(player, s, auctionPls)).collect(Collectors.toList());
                baseItem = new ItemBuilder(baseItem)
                        .setLore(lore)
                        .create();

                ItemStack finalBaseItem = baseItem;
                gui.addItem(new GuiItem(finalBaseItem, event -> {
                    if (event.isShiftClick() && player.hasPermission("auctionhouse.admin")) {
                        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
                        if (event.isLeftClick())
                            manager.expireAuction(value);

                        else if (event.isRightClick())
                            manager.deleteAuction(value, result -> {});

                        this.sync(() -> this.setAuctions(gui, player));
                        return;
                    }

                    if (player.getUniqueId() == value.getSeller()) {
                        gui.close(player);
                        this.rosePlugin.getManager(LocaleManager.class).sendMessage(player, "command-buy-own-auction");
                        return;
                    }

                    this.menuManager.get(ConfirmMenu.class).open(player, value);
                }));
            });

            gui.update();
            // opening a gui cannot be async iirc
            this.sync(() -> gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui))));
        });
    }

    /**
     * Set the sort option for the gui
     *
     * @param gui    The Gui
     * @param player The Player
     */
    private void setSort(PaginatedGui gui, Player player) {
        SortOption option = this.sortMap.getOrDefault(player.getUniqueId(), new SortOption(this.defaultSort));
        Consumer<InventoryClickEvent> consumer = event -> {

            if (!option.getIterator().hasNext()) {
                option.setIterator(Arrays.stream(AuctionSort.values()).iterator());
            }

            option.setSort(option.getIterator().next());
            this.sortMap.put(player.getUniqueId(), option);
            this.setSort(gui, player);
            this.setAuctions(gui, player);
        };

        this.put(gui, "sort-auctions", player, StringPlaceholders.single("sort", option.getSort().getDisplayName()), consumer);
    }

    @Override
    public Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{
            this.put("#0", "GUI Settings");
            this.put("gui-settings.title", "Auction House (%page%/%total%)");
            this.put("gui-settings.rows", 6);
            this.put("gui-settings.page-slots", List.of("9-44"));
            this.put("gui-settings.border-slots", List.of("0-8", "45-53"));
            this.put("gui-settings.should-scroll", false);
            this.put("gui-settings.scroll-type", "HORIZONTAL");

            this.put("#1", "Auction Lore Settings");
            this.put("auction-lore", List.of(
                    " &8------ #00B4DB&lSelling Item &8------",
                    " &f| &7Price: &f%price%",
                    " &f| &7Time: &f%time%",
                    " &f| &7Seller: &f%seller%",
                    " &f|",
                    " &f| &7Click to purchase"
            ));

            this.put("admin-auction-lore", List.of(
                    " &8------ #00B4DB&lSelling Item &8------",
                    " &f| &7Price: &f%price%",
                    " &f| &7Time: &f%time%",
                    " &f| &7Seller: &f%seller%",
                    " &f|",
                    " &f| &7Click to purchase",
                    " &f| &7Shift-Left Click to expire",
                    " &f| &7Shift-Right Click to cancel"
            ));

            this.put("#3", "Should the lore be before or after the item's lore?");
            this.put("lore-before", false);

            this.put("#4", "Border Settings");
            this.put("border-item.material", "BLACK_STAINED_GLASS_PANE");
            this.put("border-item.name", " ");

            this.put("#5", "Next Page");
            this.put("next-page.material", "PAPER");
            this.put("next-page.name", "#00B4DB&lNext Page");
            this.put("next-page.lore", List.of(" &f| &7Click to go to", " &f| &7the next page."));
            this.put("next-page.glow", true);
            this.put("next-page.slot", 50);

            this.put("#6", "Previous Page");
            this.put("previous-page.material", "PAPER");
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
            this.put("previous-page.lore", List.of(" &f| &7Click to go to", " &f| &7the previous page."));
            this.put("previous-page.glow", true);
            this.put("previous-page.slot", 48);

            this.put("#7", "Sold Auctions Menu");
            this.put("sold-auctions.enabled", true);
            this.put("sold-auctions.material", "GOLD_INGOT");
            this.put("sold-auctions.name", "#00B4DB&lSold Auctions");
            this.put("sold-auctions.lore", List.of(" &f| &7Click to go to", " &f| &7the sold auctions menu."));
            this.put("sold-auctions.glow", true);
            this.put("sold-auctions.slot", 2);

            this.put("#8", "Expired Auctions Menu");
            this.put("expired-auctions.enabled", true);
            this.put("expired-auctions.material", "CLOCK");
            this.put("expired-auctions.name", "#00B4DB&lExpired Auctions");
            this.put("expired-auctions.lore", List.of(" &f| &7Click to go to", " &f| &7the expired auctions menu."));
            this.put("expired-auctions.glow", true);
            this.put("expired-auctions.slot", 6);

            this.put("#9", "Refresh Menu");
            this.put("refresh-menu.enabled", true);
            this.put("refresh-menu.material", "SUNFLOWER");
            this.put("refresh-menu.name", "#00B4DB&lRefresh Menu");
            this.put("refresh-menu.lore", List.of(" &f| &7Click to refresh the menu."));
            this.put("refresh-menu.glow", true);
            this.put("refresh-menu.slot", 49);

            this.put("#10", "My Auctions");
            this.put("my-auctions.enabled", true);
            this.put("my-auctions.material", "BOOK");
            this.put("my-auctions.name", "#00B4DB&lMy Auctions");
            this.put("my-auctions.lore", List.of(" &f| &7Click to go to", " &f| &7your auctions."));
            this.put("my-auctions.glow", true);
            this.put("my-auctions.slot", 4);

            this.put("#11", "Sort Auctions");
            this.put("#12", "Sort Defaults: PRICE_ASCENDING, PRICE_DESCENDING, TIME_ASCENDING, TIME_DESCENDING");
            this.put("sort-auctions.enabled", true);
            this.put("sort-auctions.default", "TIME_ASCENDING");
            this.put("sort-auctions.material", "COMPARATOR");
            this.put("sort-auctions.name", "#00B4DB&lSort Auctions &8| &f%sort%");
            this.put("sort-auctions.lore", List.of(" &f| &7Click to sort", " &f| &7your auctions."));
            this.put("sort-auctions.glow", true);
            this.put("sort-auctions.slot", 46);

            this.put("#13", "Auction Sellers");
            this.put("auction-sellers.enabled", true);
            this.put("auction-sellers.material", "SPRUCE_SIGN");
            this.put("auction-sellers.name", "#00B4DB&lAuction Sellers");
            this.put("auction-sellers.lore", List.of(" &f| &7Click to go to", " &f| &7the auction sellers menu."));
            this.put("auction-sellers.glow", true);
            this.put("auction-sellers.slot", 52);

        }};
    }

    @Override
    public String getMenuName() {
        return "main_menu";
    }

    @Override
    public List<Integer> getPageSlots() {
        return this.parseList(this.get("gui-settings.page-slots", List.of("0-44")));
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
