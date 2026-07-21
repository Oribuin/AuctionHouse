package xyz.oribuin.auctionhouse.command.impl.admin;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.OfflinePlayer;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;

import java.util.List;

public class CheckCommand extends BaseRoseCommand {

    public CheckCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, OfflinePlayer player) {
        AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        List<Auction> auctions = manager.getAuctions(player.getUniqueId());

        locale.sendMessage(context.getSender(), "command-admin-check-header", StringPlaceholders.of("player", player.getName()));

        for (Auction auction : auctions) {

            StringPlaceholders.Builder placeholders = StringPlaceholders.builder("id", auction.getId())
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
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("check")
                .arguments(ArgumentsDefinition.of("player", ArgumentHandlers.OFFLINE_PLAYER))
                .permission("auctionhouse.admin")
                .build();
    }

}