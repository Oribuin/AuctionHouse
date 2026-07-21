package xyz.oribuin.auctionhouse.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.HelpCommand;
import dev.rosewood.rosegarden.command.ReloadCommand;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.auctionhouse.command.impl.AdminCommand;
import xyz.oribuin.auctionhouse.command.impl.SellCommand;
import xyz.oribuin.auctionhouse.command.impl.ViewCommand;
import xyz.oribuin.auctionhouse.gui.MainAuctionsMenu;
import xyz.oribuin.auctionhouse.manager.MenuManager;

public class AuctionCommand extends BaseRoseCommand {

    public AuctionCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        MenuManager manager = this.rosePlugin.getManager(MenuManager.class);
        Player player = (Player) context.getSender();

        manager.get(MainAuctionsMenu.class).open(player);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("auctionhouse")
                .aliases("ah")
                .playerOnly(true)
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder().optionalSub(
                new AdminCommand(this.rosePlugin),
                new SellCommand(this.rosePlugin),
                new ViewCommand(this.rosePlugin),
                new ReloadCommand(this.rosePlugin),
                new HelpCommand(this.rosePlugin, this)
        );
    }
}
