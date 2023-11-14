package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.auctionhouse.gui.ConfirmMenu;
import xyz.oribuin.auctionhouse.gui.ExpiredAuctionsMenu;
import xyz.oribuin.auctionhouse.gui.MainAuctionsMenu;
import xyz.oribuin.auctionhouse.gui.PersonalAuctionsMenu;
import xyz.oribuin.auctionhouse.gui.SellerMenu;
import xyz.oribuin.auctionhouse.gui.SoldAuctionsMenu;
import xyz.oribuin.auctionhouse.gui.ViewMenu;
import xyz.oribuin.auctionhouse.gui.api.PluginMenu;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuManager extends Manager {

    private final Map<Class<? extends PluginMenu>, PluginMenu> registeredMenus = new LinkedHashMap<>();

    public MenuManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.put(new ConfirmMenu(this.rosePlugin));
        this.put(new ExpiredAuctionsMenu(this.rosePlugin));
        this.put(new MainAuctionsMenu(this.rosePlugin));
        this.put(new PersonalAuctionsMenu(this.rosePlugin));
        this.put(new SellerMenu(this.rosePlugin));
        this.put(new SoldAuctionsMenu(this.rosePlugin));
        this.put(new ViewMenu(this.rosePlugin));

        this.registeredMenus.forEach((name, gui) -> gui.load());
    }

    public void put(PluginMenu menu) {
        this.registeredMenus.put(menu.getClass(), menu);
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginMenu> T get(Class<T> menuClass) {
        if (this.registeredMenus.containsKey(menuClass)) {
            return (T) this.registeredMenus.get(menuClass);
        }

        return null;
    }

    @Override
    public void disable() {

    }
}
