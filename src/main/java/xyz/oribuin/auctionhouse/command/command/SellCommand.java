package xyz.oribuin.auctionhouse.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
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

public class SellCommand extends RoseCommand {

    private final Map<UUID, Double> confirmMap = new HashMap<>();

    public SellCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, Double price) {
        AuctionManager auctionManager = this.rosePlugin.getManager(AuctionManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        Player player = (Player) context.getSender();

        if (price == null) {
            locale.sendMessage(player, "command-sell-invalid-price");
            return;
        }

        // Make the player confirm they want to sell their item
        if (!this.confirmMap.containsKey(player.getUniqueId()) || this.confirmMap.getOrDefault(player.getUniqueId(), 0.0) != price.doubleValue()) {
            locale.sendMessage(player, "command-sell-confirm", StringPlaceholders.of("price", AuctionUtils.formatCurrency(price)));
            this.confirmMap.put(player.getUniqueId(), price);
            return;
        }

        final ItemStack item = player.getInventory().getItemInMainHand().clone();
        this.confirmMap.remove(player.getUniqueId());
        auctionManager.createAuction(player, item, price);
    }

    @Override
    protected String getDefaultName() {
        return "sell";
    }

    @Override
    public String getDescriptionKey() {
        return "command-sell-description";
    }

    @Override
    public String getRequiredPermission() {
        return "auctionhouse.sell";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}

