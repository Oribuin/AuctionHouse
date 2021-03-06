package xyz.oribuin.auctionhouse;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.NMSUtil;
import org.bukkit.plugin.PluginManager;
import xyz.oribuin.auctionhouse.hook.PAPI;
import xyz.oribuin.auctionhouse.hook.VaultHook;
import xyz.oribuin.auctionhouse.listener.PlayerListeners;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.CommandManager;
import xyz.oribuin.auctionhouse.manager.ConfigurationManager;
import xyz.oribuin.auctionhouse.manager.DataManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.LogManager;

import java.util.List;

public class AuctionHousePlugin extends RosePlugin {

    private static AuctionHousePlugin instance;

    public AuctionHousePlugin() {
        super(-1, -1, ConfigurationManager.class, DataManager.class, LocaleManager.class, CommandManager.class);

        instance = this;
    }

    @Override
    protected void enable() {
        // Check if the server is on 1.17 or higher
        if (NMSUtil.getVersionNumber() < 17) {
            this.getLogger().severe("This plugin requires 1.17 or higher, Disabling plugin!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check if vault is installed
        if (!this.getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.getLogger().severe("Vault is not installed or not enabled, Disabling plugin!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check if PAPI is installed
        if (!this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().severe("PlaceholderAPI is not installed or not enabled, Disabling plugin!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register plugin listeners.
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerListeners(this), this);

        // Register plugin hooks
        VaultHook.hook();
        new PAPI(this).register();

    }

    @Override
    protected void disable() {
        // Do nothing
    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return List.of(LogManager.class, AuctionManager.class);
    }

    public static AuctionHousePlugin getInstance() {
        return instance;
    }
}
