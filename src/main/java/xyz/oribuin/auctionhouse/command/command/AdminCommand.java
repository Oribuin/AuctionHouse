package xyz.oribuin.auctionhouse.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.auctionhouse.command.command.admin.CheckCommand;
import xyz.oribuin.auctionhouse.command.command.admin.DeleteCommand;
import xyz.oribuin.auctionhouse.command.command.admin.ExpireCommand;
import xyz.oribuin.auctionhouse.command.command.admin.GetCommand;
import xyz.oribuin.auctionhouse.manager.LocaleManager;

public class AdminCommand extends RoseCommand {

    public AdminCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent,
                CheckCommand.class,
                DeleteCommand.class,
                ExpireCommand.class,
                GetCommand.class
        );
    }

    @RoseExecutable
    public void execute(CommandContext context, @dev.rosewood.rosegarden.command.framework.annotation.Optional RoseSubCommand type) {

        for (RoseSubCommand subCommand : this.getSubCommands().values()) {

            if (subCommand.getRequiredPermission() == null || context.getSender().hasPermission(subCommand.getRequiredPermission())) {
                this.rosePlugin.getManager(LocaleManager.class).sendMessage(
                        context.getSender(),
                        "command-admin-usage",
                        StringPlaceholders.of("command", "/ah admin " + subCommand.getName())
                );
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


}
