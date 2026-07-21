package xyz.oribuin.auctionhouse.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import xyz.oribuin.auctionhouse.command.impl.admin.CheckCommand;
import xyz.oribuin.auctionhouse.command.impl.admin.DeleteCommand;
import xyz.oribuin.auctionhouse.command.impl.admin.ExpireCommand;
import xyz.oribuin.auctionhouse.command.impl.admin.GetCommand;

public class AdminCommand extends BaseRoseCommand {

    public AdminCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("admin")
                .permission("auctionhouse.admin")
                .descriptionKey("command-admin-description")
                .arguments(this.createArgumentInfo())
                .build();
    }

    private ArgumentsDefinition createArgumentInfo() {
        return ArgumentsDefinition.builder().requiredSub(
                new CheckCommand(this.rosePlugin),
                new DeleteCommand(this.rosePlugin),
                new ExpireCommand(this.rosePlugin),
                new GetCommand(this.rosePlugin)
        );
    }

}
