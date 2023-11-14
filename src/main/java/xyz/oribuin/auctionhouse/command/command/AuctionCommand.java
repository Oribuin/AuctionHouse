package xyz.oribuin.auctionhouse.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.command.BaseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.gui.MainAuctionsMenu;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;

public class AuctionCommand extends BaseCommand {

    public AuctionCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        if (!(context.getSender() instanceof Player player)) {
            locale.sendMessage(context.getSender(), "not-player");
            return;
        }

        this.rosePlugin.getManager(MenuManager.class).get(MainAuctionsMenu.class).open(player);
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.use";
    }
}
