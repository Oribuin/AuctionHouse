package xyz.oribuin.auctionhouse.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.gui.ViewMenu;
import xyz.oribuin.auctionhouse.manager.MenuManager;

public class ViewCommand extends BaseRoseCommand {

    public ViewCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, OfflinePlayer seller) {
        MenuManager manager = this.rosePlugin.getManager(MenuManager.class);
        Player contextPlayer = (Player) context.getSender();

        manager.get(ViewMenu.class).open(contextPlayer, seller);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("view")
                .descriptionKey("command-view-description")
                .permission("auctionhouse.view")
                .playerOnly(true)
                .arguments(ArgumentsDefinition.of("seller", ArgumentHandlers.OFFLINE_PLAYER))
                .build();
    }

}
