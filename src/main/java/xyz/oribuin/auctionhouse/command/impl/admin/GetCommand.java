package xyz.oribuin.auctionhouse.command.impl.admin;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;

public class GetCommand extends BaseRoseCommand {

    public GetCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, int auctionId) {
        AuctionManager manager = this.rosePlugin.getManager(AuctionManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        Player player = (Player) context.getSender();
        Auction auction = manager.getAuction(auctionId);

        if (auction == null) {
            locale.sendMessage(context.getSender(), "invalid-auction");
            return;
        }

        player.getInventory().addItem(auction.getItem().clone());
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("get")
                .arguments(ArgumentsDefinition.of("auctionId", ArgumentHandlers.INTEGER))
                .permission("auctionhouse.admin")
                .playerOnly(true)
                .build();
    }

}