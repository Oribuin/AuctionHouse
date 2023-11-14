package xyz.oribuin.auctionhouse.command.command.admin;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.OfflinePlayer;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;

import java.util.List;

public class CheckCommand extends RoseSubCommand {

    public CheckCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, OfflinePlayer player) {
        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final List<Auction> auctions = manager.getAuctions(player.getUniqueId());

        locale.sendMessage(context.getSender(), "command-admin-check-header", StringPlaceholders.of("player", player.getName()));

        for (Auction auction : auctions) {

            final StringPlaceholders.Builder placeholders = StringPlaceholders.builder("id", auction.getId())
                    .add("price", AuctionUtils.formatCurrency(auction.getPrice()))
                    .add("sold", auction.isSold() ? "Sold" : "Not Sold");

            if (auction.getItem() != null) {
                placeholders.add("item", auction.getItem().getType().name());
                placeholders.add("amount", auction.getItem().getAmount());
            }

            locale.sendMessage(context.getSender(), "command-admin-check-format", placeholders.build());
        }
    }

    @Override
    public String getDefaultName() {
        return "check";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.admin.check";
    }
}