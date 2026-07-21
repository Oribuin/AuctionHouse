package xyz.oribuin.auctionhouse;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.config.SettingHolder;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.auctionhouse.config.Setting;
import xyz.oribuin.auctionhouse.hook.VaultProvider;
import xyz.oribuin.auctionhouse.listener.PlayerListeners;
import xyz.oribuin.auctionhouse.manager.AuctionManager;
import xyz.oribuin.auctionhouse.manager.CommandManager;
import xyz.oribuin.auctionhouse.manager.DataManager;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.manager.LogManager;
import xyz.oribuin.auctionhouse.manager.MenuManager;

import java.util.List;

public class AuctionHouse extends RosePlugin {

    private static AuctionHouse instance;

    public AuctionHouse() {
        super(-1, -1, DataManager.class, LocaleManager.class, CommandManager.class);

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

    @Override
    protected @Nullable SettingHolder getRoseConfigSettingHolder() {
        return Setting.INSTANCE;
    }

    @Override
    protected @NotNull String[] getRoseConfigHeader() {
        return new String[]{"   _____                 __  .__                ___ ___                             ",
                "  /  _  \\  __ __   _____/  |_|__| ____   ____  /   |   \\  ____  __ __  ______ ____  ",
                " /  /_\\  \\|  |  \\_/ ___\\   __\\  |/  _ \\ /    \\/    ~    \\/  _ \\|  |  \\/  ___// __ \\ ",
                "/    |    \\  |  /\\  \\___|  | |  (  <_> )   |  \\    Y    (  <_> )  |  /\\___ \\\\  ___/ ",
                "\\____|__  /____/  \\___  >__| |__|\\____/|___|  /\\___|_  / \\____/|____//____  >\\___  >",
                "        \\/            \\/                    \\/       \\/                   \\/     \\/ "};
    }

    public static AuctionHouse get() {
        return instance;
    }
}
