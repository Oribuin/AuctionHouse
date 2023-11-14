package xyz.oribuin.auctionhouse.command.command.admin;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;

public class ExpireCommand extends RoseSubCommand {

    public ExpireCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, int auctionId) {
        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final Auction auction = manager.getAuction(auctionId);

        if (auction == null) {
            locale.sendMessage(context.getSender(), "invalid-auction");
            return;
        }

        // Delete auction
        manager.expire(auction);
        locale.sendMessage(context.getSender(), "command-admin-expire-success");
    }


    @Override
    public String getDefaultName() {
        return "expire";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.admin.expire";
    }

}