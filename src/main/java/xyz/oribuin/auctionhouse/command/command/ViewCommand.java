package xyz.oribuin.auctionhouse.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.gui.ViewMenu;
import xyz.oribuin.auctionhouse.manager.MenuManager;

public class ViewCommand extends RoseCommand {

    public ViewCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, OfflinePlayer seller) {
        final MenuManager manager = this.rosePlugin.getManager(MenuManager.class);
        final Player contextPlayer = (Player) context.getSender();

        manager.get(ViewMenu.class).open(contextPlayer, seller);
    }

    @Override
    protected String getDefaultName() {
        return "view";
    }

    @Override
    public String getDescriptionKey() {
        return "command-view-description";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.view";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
