package xyz.oribuin.auctionhouse.command.command.admin;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;

public class GetCommand extends RoseSubCommand {

    public GetCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, int auctionId) {
        final AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final Player player = (Player) context.getSender();
        final Auction auction = manager.getAuction(auctionId);

        if (auction == null) {
            locale.sendMessage(context.getSender(), "invalid-auction");
            return;
        }

        final Gui gui = Gui.gui()
                .type(GuiType.HOPPER)
                .disableItemSwap()
                .disableItemSwap()
                .disableItemPlace()
                .create();

        gui.setItem(2, new GuiItem(auction.getItem().clone()));
        gui.open(player);
    }

    @Override
    public String getDefaultName() {
        return "get";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.admin.get";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

}