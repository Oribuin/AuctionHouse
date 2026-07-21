package xyz.oribuin.auctionhouse.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellCommand extends BaseRoseCommand {

    private final Map<UUID, Double> confirmMap = new HashMap<>();

    public SellCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Double price) {
        AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        Player player = (Player) context.getSender();

        // Make the player confirm they want to sell their item
        double current = this.confirmMap.getOrDefault(player.getUniqueId(), 0.0);
        if (current != price) {
            locale.sendMessage(player, "command-sell-confirm", StringPlaceholders.of("price", AuctionUtils.formatCurrency(price)));
            this.confirmMap.put(player.getUniqueId(), price);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand().clone();

        this.confirmMap.remove(player.getUniqueId());
        auctionManager.createAuction(player, item, price);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("sell")
                .descriptionKey("command-sell-description")
                .playerOnly(true)
                .permission("auctionhouse.sell")
                .arguments(ArgumentsDefinition.of("price", ArgumentHandlers.DOUBLE))
                .build();
    }

}

