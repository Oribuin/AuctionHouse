package xyz.oribuin.auctionhouse.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import xyz.oribuin.auctionhouse.AuctionHousePlugin;

public final class VaultHook {

    private static Economy economy;

    public static void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            AuctionHousePlugin.getInstance().getLogger().info("Hooking into Vault...");
            economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
        }
    }

    public static Economy getEconomy() {
        return economy;
    }

}

