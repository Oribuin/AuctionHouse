package xyz.oribuin.auctionhouse.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.OfflinePlayer;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.util.PluginUtils;

import java.util.List;
import java.util.Optional;

public class AdminCommand extends RoseCommand {

    public AdminCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent, CheckCommand.class, DeleteCommand.class, ExpireCommand.class);
    }

    @RoseExecutable
    public void execute(CommandContext context, @dev.rosewood.rosegarden.command.framework.annotation.Optional RoseSubCommand type) {

        for (RoseSubCommand subCommand : this.getSubCommands().values()) {

            if (subCommand.getRequiredPermission() == null || context.getSender().hasPermission(subCommand.getRequiredPermission())) {
                this.rosePlugin.getManager(LocaleManager.class).sendMessage(context.getSender(), "command-admin-usage", StringPlaceholders.single("command", "/ah admin " + subCommand.getName()));
            }

        }
    }

    @Override
    protected String getDefaultName() {
        return "admin";
    }

    @Override
    public String getDescriptionKey() {
        return "command-admin-description";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.admin";
    }

    public static class CheckCommand extends RoseSubCommand {

        public CheckCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
            super(rosePlugin, parent);
        }

        @RoseExecutable
        public void execute(@Inject CommandContext context, OfflinePlayer player) {
            final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);
            final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

            List<Auction> auctions = auctionManager.getAuctionsBySeller(player.getUniqueId());

            locale.sendMessage(context.getSender(), "command-admin-check-header", StringPlaceholders.single("player", player.getName()));

            for (Auction auction : auctions) {

                final StringPlaceholders.Builder placeholders = StringPlaceholders.builder("id", auction.getId())
                        .addPlaceholder("price", PluginUtils.formatCurrency(auction.getPrice()))
                        .addPlaceholder("sold", auction.isSold() ? "Sold" : "Not Sold");

                if (auction.getItem() != null) {
                    placeholders.addPlaceholder("item", auction.getItem().getType().name());
                    placeholders.addPlaceholder("amount", auction.getItem().getAmount());
                }

                locale.sendMessage(context.getSender(), "command-admin-check-format", placeholders.build());
            }
        }

        @Override
        protected String getDefaultName() {
            return "check";
        }

        @Override
        public String getRequiredPermission() {
            return "auctionhouse.admin.check";
        }
    }

    public static class DeleteCommand extends RoseSubCommand {

        public DeleteCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
            super(rosePlugin, parent);
        }

        @RoseExecutable
        public void execute(@Inject CommandContext context, int auctionId) {
            final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);
            final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

            Optional<Auction> auction = auctionManager.getAuctionById(auctionId);

            if (auction.isEmpty()) {
                locale.sendMessage(context.getSender(), "invalid-auction");
                return;
            }

            // Delete auction
            auctionManager.deleteAuction(auction.get());
            locale.sendMessage(context.getSender(), "command-admin-delete-success", StringPlaceholders.single("id", auctionId));
        }

        @Override
        protected String getDefaultName() {
            return "delete";
        }

        @Override
        public String getRequiredPermission() {
            return "auctionhouse.admin.delete";
        }
    }

    public static class ExpireCommand extends RoseSubCommand {

        public ExpireCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
            super(rosePlugin, parent);
        }

        @RoseExecutable
        public void execute(@Inject CommandContext context, int auctionId) {
            final AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);
            final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

            Optional<Auction> auction = auctionManager.getAuctionById(auctionId);

            if (auction.isEmpty()) {
                locale.sendMessage(context.getSender(), "invalid-auction");
                return;
            }

            // Delete auction
            auctionManager.expireAuction(auction.get());
            locale.sendMessage(context.getSender(), "command-admin-expire-success");
        }


        @Override
        protected String getDefaultName() {
            return "expire";
        }

        @Override
        public String getRequiredPermission() {
            return "auctionhouse.admin.expire";
        }

    }


}
