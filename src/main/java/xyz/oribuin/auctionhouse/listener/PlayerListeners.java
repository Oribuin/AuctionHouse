package xyz.oribuin.auctionhouse.listener;

import dev.rosewood.rosegarden.RosePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.oribuin.auctionhouse.event.AuctionSoldEvent;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.DataManager;

public class PlayerListeners implements Listener {

    private final RosePlugin rosePlugin;
    private final AuctionManager manager;

    public PlayerListeners(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
        this.manager = this.rosePlugin.getManager(AuctionManager.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {

        // load the users auctions (we're loading the sold or expired auctions here)
        this.rosePlugin.getManager(DataManager.class).loadUserAuctions(event.getPlayer().getUniqueId());

        this.rosePlugin.getServer().getScheduler().runTaskLaterAsynchronously(this.rosePlugin,
                () -> this.manager.showProfit(event.getPlayer()), 100);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAuctionSold(AuctionSoldEvent event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getAuction().getSeller());
        if (player.isOnline()) {
            return;
        }

        this.manager.addProfit(event.getAuction().getSeller(), event.getAuction());
    }

}
