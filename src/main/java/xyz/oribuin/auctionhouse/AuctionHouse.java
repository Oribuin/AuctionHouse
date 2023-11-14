package xyz.oribuin.auctionhouse;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.plugin.PluginManager;
import xyz.oribuin.auctionhouse.hook.VaultProvider;
import xyz.oribuin.auctionhouse.listener.PlayerListeners;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.CommandManager;
import xyz.oribuin.auctionhouse.manager.ConfigurationManager;
import xyz.oribuin.auctionhouse.manager.DataManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.LogManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;

import java.util.List;

public class AuctionHouse extends RosePlugin {

    private static AuctionHouse instance;

    public AuctionHouse() {
        super(-1, -1, ConfigurationManager.class, DataManager.class, LocaleManager.class, CommandManager.class);

        instance = this;
    }

    @Override
    protected void enable() {
        // Register plugin listeners.
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerListeners(this), this);

        // Register Plugin Hooks
        VaultProvider.get();
    }

    @Override
    protected void disable() {
        // Do nothing
    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return List.of(LogManager.class, AuctionManager.class, MenuManager.class);
    }

    public static AuctionHouse get() {
        return instance;
    }
}
