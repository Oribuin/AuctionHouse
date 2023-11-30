package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.auction.OfflineProfits;
import xyz.oribuin.auctionhouse.event.AuctionCreateEvent;
import xyz.oribuin.auctionhouse.event.AuctionSoldEvent;
import xyz.oribuin.auctionhouse.hook.VaultProvider;
import xyz.oribuin.auctionhouse.manager.ConfigurationManager.Settings;
import xyz.oribuin.auctionhouse.manager.LogManager.LogMessage;
import xyz.oribuin.auctionhouse.util.AuctionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AuctionManager extends Manager {

    private final Map<UUID, Long> listingCooldown = new HashMap<>();
    private DataManager data;
    private LogManager logManager;

    public AuctionManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.logManager = this.rosePlugin.getManager(LogManager.class);
        this.data = this.rosePlugin.getManager(DataManager.class);
        this.data.loadAuctions();

        Bukkit.getOnlinePlayers().forEach(player -> this.data.loadUserAuctions(player.getUniqueId()));
    }

    @Override
    public void disable() {
        // Nothing to do here
    }

    /**
     * Create a new auction with the given information
     *
     * @param player The player creating the auction
     * @param item   The item being auctioned
     * @param price  The price of the item
     */
    public void createAuction(Player player, ItemStack item, double price) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final int maxAuctions = this.getMaximumAuctions(player);
        final int currentAuctions = this.getActive(player.getUniqueId()).size();

        // Check if the player has permission to create an auction

        if (currentAuctions >= maxAuctions) {
            final StringPlaceholders placeholders = StringPlaceholders.builder()
                    .add("max", maxAuctions)
                    .add("current", currentAuctions)
                    .build();

            locale.sendMessage(player, "command-sell-max-reached", placeholders);
            return;
        }

        final Long listTime = this.listingCooldown.get(player.getUniqueId());
        final double cooldownMillis = Settings.LIST_COOLDOWN.getDouble() * 1000.0;

        // check if the player is on cooldown
        if (listTime != null && listTime + cooldownMillis > System.currentTimeMillis()) {
            // format the time remaining to 1 decimal place
            final String timeLeft = String.format("%.1f", (listTime + cooldownMillis - System.currentTimeMillis()) / 1000.0);

            locale.sendMessage(player, "command-sell-cooldown", StringPlaceholders.of("time", timeLeft));
            return;
        }

        // Check if the player has enough money to create an auction
        double listPrice = Settings.LIST_PRICE.getDouble();
        double playerBalance = VaultProvider.get().balance(player);

        if (listPrice != 0 && listPrice > playerBalance) {
            locale.sendMessage(player, "invalid-funds", StringPlaceholders.builder().add("price", listPrice).build());
            return;
        }

        // Check if the price is in correct range
        final double minPrice = Settings.LIST_MIN.getDouble();
        final double maxPrice = Settings.LIST_MAX.getDouble();

        if (price < minPrice || price > maxPrice) {
            final StringPlaceholders placeholders = StringPlaceholders.builder()
                    .add("min", minPrice)
                    .add("max", maxPrice)
                    .add("price", price)
                    .build();

            locale.sendMessage(player, "command-sell-invalid-price", placeholders);
            return;
        }

        if (item.getType().isAir()) {
            locale.sendMessage(player, "command-sell-disabled-item");
            return;
        }

        // Check if the item's material is allowed to be listed
        if (Settings.DISABLED_MATERIALS.getStringList().contains(item.getType().name())) {
            locale.sendMessage(player, "command-sell-disabled-item");
            return;
        }

        // Remove the item and check if it has been removed
        if (!player.getInventory().removeItem(item).isEmpty()) {
            locale.sendMessage(player, "command-sell-no-item");
            return;
        }

        this.listingCooldown.put(player.getUniqueId(), System.currentTimeMillis());

        this.data.createAuction(player.getUniqueId(), item, price, auction -> {
            AuctionCreateEvent event = new AuctionCreateEvent(player, auction);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;

            this.logManager.addLogMessage(LogMessage.AUCTION_CREATED, auction);

            if (listPrice > 0) {
                VaultProvider.get().take(player, listPrice);
            }

            locale.sendMessage(player, "command-sell-success", StringPlaceholders.of("price", AuctionUtils.formatCurrency(price)));
        });
    }

    /**
     * Allow a player to buy an auction with the given information
     *
     * @param player    The player buying the auction
     * @param auctionId The id of the auction being bought
     */
    public void buyAuction(Player player, int auctionId) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final Auction auction = this.getAuction(auctionId);
        if (auction == null) {
            locale.sendMessage(player, "command-buy-auction-gone");
            return;
        }

        // Stop the player from buying their own auction
        if (auction.getSeller() == player.getUniqueId()) {
            locale.sendMessage(player, "command-buy-own-auction");
            return;
        }


        // Make sure the auction is has not been sold or expired
        if (auction.isSold() || auction.isExpired()) {
            locale.sendMessage(player, "command-buy-auction-gone");
            return;
        }

        // Check if the player has enough money to buy an auction
        double buyPrice = auction.getPrice();
        buyPrice = buyPrice - (buyPrice * Settings.LIST_TAX.getDouble());

        double playerBalance = VaultProvider.get().balance(player);

        if (auction.getPrice() > playerBalance) {
            locale.sendMessage(player, "invalid-funds", StringPlaceholders.builder().add("price", AuctionUtils.formatCurrency(buyPrice)).build());
            return;
        }

        // Remove the item and check if it has been removed
        ItemStack item = auction.getItem();
        if (player.getInventory().firstEmpty() == -1) {
            locale.sendMessage(player, "command-buy-no-space");
            return;
        }

        auction.setSoldTime(System.currentTimeMillis());
        auction.setSold(true);
        auction.setSoldPrice(buyPrice);
        auction.setBuyer(player.getUniqueId());

        double finalBuyPrice = buyPrice;

        // Give the player the money when the auction is saved
        CompletableFuture.runAsync(() -> this.data.saveAuction(auction)).thenRun(() -> {
            OfflinePlayer seller = auction.getSellerPlayer();

            player.getInventory().addItem(item);

            final VaultProvider provider = VaultProvider.get();
            provider.take(player, auction.getPrice());
            provider.give(seller, finalBuyPrice);

            final StringPlaceholders placeholders = StringPlaceholders.builder()
                    .add("price", AuctionUtils.formatCurrency(auction.getPrice()))
                    .add("seller", seller.getName())
                    .add("buyer", player.getName())
                    .build();


            Player sellerPlayer = seller.getPlayer();
            if (sellerPlayer != null) {
                locale.sendMessage(sellerPlayer, "auction-sold", placeholders);
            }

            locale.sendMessage(player, "command-buy-success", placeholders);
            this.logManager.addLogMessage(LogMessage.AUCTION_SOLD, auction);

            AuctionSoldEvent event = new AuctionSoldEvent(player, auction);
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    /**
     * Expire an auction
     *
     * @param auction The auction to expire
     */
    public void expire(Auction auction) {
        if (auction.isExpired()) {
            return;
        }

        this.logManager.addLogMessage(LogMessage.AUCTION_EXPIRED, auction);

        auction.setExpired(true);
        auction.setSold(false);
        auction.setExpiredTime(System.currentTimeMillis());
        this.data.saveAuction(auction);
    }

    /**
     * Delete an auction from the database
     *
     * @param auction  The auction to delete
     * @param callback The result of the deletion
     */
    public void delete(Auction auction, Consumer<Auction> callback) {
        auction.setExpired(true);
        auction.setSold(true);
        this.data.deleteAuction(auction, callback.andThen(
                x -> this.logManager.addLogMessage(LogMessage.AUCTION_DELETED, auction)));
    }

    /**
     * Delete an auction from the database
     *
     * @param auction The auction to delete
     */
    public void delete(Auction auction) {
        auction.setExpired(true);
        auction.setSold(true);

        this.delete(auction, callback -> this.logManager.addLogMessage(LogMessage.AUCTION_DELETED, auction));
    }

    /**
     * Send the player their offline profits
     *
     * @param player The player to load
     */
    public void showProfit(Player player) {
        final OfflineProfits profits = this.data.getOfflineProfitsCache().getOrDefault(player.getUniqueId(), new OfflineProfits(0, 0));
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        // Don't show the player their offline profits if they have none
        if (profits.profit() <= 0) {
            return;
        }

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("amount", AuctionUtils.formatCurrency(profits.profit()))
                .add("total", profits.sold())
                .build();

        locale.sendMessage(player, "offline-profits", placeholders);
        this.resetProfit(player.getUniqueId());
    }

    /**
     * Add a player's offline profit
     *
     * @param player  The player to add
     * @param auction The auction they sold
     */
    public void addProfit(UUID player, Auction auction) {
        final OfflineProfits offlineProfits = this.data.getOfflineProfitsCache().getOrDefault(player, new OfflineProfits(0, 0));
        double profits = offlineProfits.profit() + auction.getSoldPrice();
        int sold = offlineProfits.sold() + 1;

        this.data.saveProfits(player, new OfflineProfits(profits, sold));
    }

    /**
     * Reset a player's offline profits
     *
     * @param player The player to reset
     */
    public void resetProfit(UUID player) {
        this.data.saveProfits(player, new OfflineProfits(0, 0));
    }

    /**
     * Get auction by id
     *
     * @param id the id of the auction
     * @return the auction
     */
    @Nullable
    public Auction getAuction(int id) {
        return this.data.getAuctionCache().get(id);
    }

    /**
     * Get all auctions that are sold by a player
     *
     * @param uuid the uuid of the player
     * @return a list of auctions
     */
    public List<Auction> getAuctions(UUID uuid) {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> auction.getSeller().equals(uuid))
                .collect(Collectors.toList());
    }

    /**
     * Get all active auctions that are not expired or sold
     *
     * @return a list of auctions
     */
    public List<Auction> getActive() {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> !auction.isSold() && !auction.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Get all auctions that have not expired or sold yet from a seller
     *
     * @param seller the uuid of the player
     * @return a list of auctions
     */
    public List<Auction> getActive(UUID seller) {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> auction.getSeller().equals(seller) && !auction.isSold() && !auction.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Get all auctions that are expired
     *
     * @param seller the uuid of the player
     * @return a list of expired auctions
     */
    public List<Auction> getExpired(UUID seller) {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> auction.getSeller().equals(seller) && auction.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Get all auctions that are sold by a player
     *
     * @param seller the uuid of the player
     * @return a list of auctions
     */
    public List<Auction> getSold(UUID seller) {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> auction.getSeller().equals(seller) && auction.isSold())
                .collect(Collectors.toList());
    }

    /**
     * Get all offline players selling an item, no duplicates
     *
     * @return a list of offline players
     */
    public List<OfflinePlayer> getActiveSellers() {
        return this.data.getAuctionCache().values()
                .stream()
                .filter(auction -> !auction.isSold() && !auction.isExpired())
                .map(Auction::getSeller)
                .distinct()
                .map(Bukkit::getOfflinePlayer)
                .collect(Collectors.toList());
    }

    /**
     * Get the maximum amount of auctions a player can have open
     *
     * @param player the player to check
     * @return the maximum amount of auctions a player can have open
     */
    public int getMaximumAuctions(Player player) {
        int amount = 1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            final String target = info.getPermission().toLowerCase();

            if (target.startsWith("auctionhouse.limit.") && info.getValue()) {
                try {
                    amount = Math.max(amount, Integer.parseInt(target.substring(target.lastIndexOf('.') + 1)));
                } catch (NumberFormatException ignored) {
                    // Ignore
                }
            }
        }

        return amount;
    }

}
