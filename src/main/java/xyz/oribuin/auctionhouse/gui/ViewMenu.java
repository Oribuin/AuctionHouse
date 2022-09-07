package xyz.oribuin.auctionhouse.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;
import xyz.oribuin.auctionhouse.util.ItemBuilder;
import xyz.oribuin.auctionhouse.util.PluginUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ViewMenu extends OriMenu {

    private final MenuManager menuManager = this.rosePlugin.getManager(MenuManager.class);

    public ViewMenu(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public void open(Player player, OfflinePlayer seller) {
        boolean shouldScroll = this.get("gui-settings.should-scroll", false);
        ScrollType scrollType = ScrollType.valueOf(this.get("gui-settings.scroll-type", "HORIZONTAL"));
        final PaginatedGui gui = shouldScroll ? this.createScrollingGui(player, scrollType) : this.createPagedGUI(player);

        final ItemStack item = PluginUtils.getItemStack(this.config, "border-item", player, StringPlaceholders.empty());
        List<Integer> borderSlots = this.parseList(this.get("gui-settings.border-slots", List.of("35-54")));
        gui.setItem(borderSlots, new GuiItem(item));

        this.put(gui, "next-page", player, event -> {
            gui.next();
            gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui, seller)));
        });

        this.put(gui, "refresh-menu", player, event -> this.setAuctions(gui, player, seller));
        this.put(gui, "previous-page", player, event -> {
            gui.previous();
            gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui, seller)));
        });


        this.setAuctions(gui, player, seller);

        gui.open(player);
        gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui, seller)));
    }


    /**
     * Set the auctions for the gui
     *
     * @param gui    Gui
     * @param player Player
     */
    public void setAuctions(PaginatedGui gui, Player player, OfflinePlayer seller) {

        final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);

        List<String> configLore = player.hasPermission("auctionhouse.admin")
                ? this.get("admin-auction-lore", List.of("Missing option admin-auction-lore in /menus/view_menu.yml"))
                : this.get("auction-lore", List.of("Missing option auction-lore in /menus/view_menu.yml"));

        boolean loreBefore = this.get("lore-before", false);
        gui.clearPageItems(true);

        this.async(() -> {

            auctionManager.getActiveAuctionsBySeller(seller.getUniqueId()).forEach(value -> {

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

                gui.addItem(new GuiItem(baseItem, event -> {

                    if (event.isShiftClick() && player.hasPermission("auctionhouse.admin")) {
                        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
                        if (event.isLeftClick())
                            manager.expireAuction(value);

                        else if (event.isRightClick())
                            manager.deleteAuction(value);

                        this.sync(() -> this.setAuctions(gui, player, seller));
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
            this.sync(() -> gui.updateTitle(this.formatString(player, this.get("gui-settings.title", "gui-settings.title"), this.getPagePlaceholders(gui, seller))));
        });
    }

    @Override
    public Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{
            this.put("#0", "GUI Settings");
            this.put("gui-settings.title", "%player%'s Auctions (%page%/%total%)");
            this.put("gui-settings.rows", 4);
            this.put("gui-settings.page-slots", List.of("9-26"));
            this.put("gui-settings.border-slots", List.of("0-8", "27-35"));
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
            this.put("next-page.slot", 32);

            this.put("#6", "Previous Page");
            this.put("previous-page.material", "PAPER");
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
            this.put("previous-page.lore", List.of(" &f| &7Click to go to", " &f| &7the previous page."));
            this.put("previous-page.glow", true);
            this.put("previous-page.slot", 30);

            this.put("#9", "Refresh Menu");
            this.put("refresh-menu.enabled", true);
            this.put("refresh-menu.material", "SUNFLOWER");
            this.put("refresh-menu.name", "#00B4DB&lRefresh Menu");
            this.put("refresh-menu.lore", List.of(" &f| &7Click to refresh the menu."));
            this.put("refresh-menu.glow", true);
            this.put("refresh-menu.slot", 31);
        }};
    }

    @Override
    public String getMenuName() {
        return "view_menu";
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

    public StringPlaceholders getPagePlaceholders(PaginatedGui gui, OfflinePlayer player) {
        return StringPlaceholders.builder()
                .addPlaceholder("page", gui.getCurrentPageNum())
                .addPlaceholder("total", Math.max(gui.getPagesNum(), 1))
                .addPlaceholder("next", gui.getNextPageNum())
                .addPlaceholder("previous", gui.getPrevPageNum())
                .addPlaceholder("player", player.getName())
                .build();
    }
}
