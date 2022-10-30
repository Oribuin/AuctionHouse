package xyz.oribuin.auctionhouse.locale;

import dev.rosewood.rosegarden.locale.Locale;

import java.util.HashMap;
import java.util.Map;

public class EnglishLocale implements Locale {

    @Override
    public String getLocaleName() {
        return "en_US";
    }

    @Override
    public String getTranslatorName() {
        return "Oribuin";
    }

    @Override
    public Map<String, Object> getDefaultLocaleValues() {
        return new HashMap<>() {{
            this.put("#0", "Plugin Command Prefix");
            this.put("prefix", "<g:#00B4DB:#0083B0>&lAuctionHouse &8| &f");

            this.put("#1", "Generic Command Messages");
            this.put("no-permission", "You do not have permission to do that!");
            this.put("only-player", "This command can only be executed by a player!");
            this.put("unknown-command", "Unknown command, use #00B4DB/%cmd%&f help for more info");
            this.put("invalid-funds", "You do not have enough funds to do that!");

            this.put("#2", "Help Command Messages");
            this.put("command-help-title", "Available Commands:");
            this.put("command-help-description", "Displays the help menu");
            this.put("command-help-list-description", " &8- #00B4DB/%cmd% %subcmd% %args% &7- &f%desc%");
            this.put("command-help-list-description-no-args", " &8- #00B4DB/%cmd% %subcmd% &7- &f%desc%");

            this.put("#3", "Reload Command Messages");
            this.put("command-reload-description", "Reloads the plugin.");
            this.put("command-reload-reloaded", "Configuration and locale files were reloaded");

            this.put("#4", "Sell Command");
            this.put("command-sell-description", "Put your hand item in the auction.");
            this.put("command-sell-confirm", "Please confirm you want to sell your item for %price%");
            this.put("command-sell-success", "You have put an item on the auction for %price%");
            this.put("command-sell-max-reached", "You have reached the maximum amount of items you can sell at once. (%current%/%max%)");
            this.put("command-sell-invalid-price", "Please specify a valid price. ($%min%-$%max%)");
            this.put("command-sell-invalid-item", "Please provide a valid item to sell.");
            this.put("command-sell-no-item", "We couldn't remove your item from your inventory. Please try again.");
            this.put("command-sell-cooldown", "You must wait %time% seconds before you can sell another item.");
            this.put("command-sell-disabled-item", "You cannot sell this item.");

            this.put("#5", "Buy Command");
            this.put("command-buy-success", "You have bought an item for %price% from %seller%");
            this.put("command-buy-own-auction", "You can't buy your own auction!");
            this.put("command-buy-auction-gone", "The auction you are trying to buy has already been sold.");
            this.put("command-buy-no-space", "You don't have enough space in your inventory to buy this item.");

            this.put("#6", "View Command");
            this.put("command-view-description", "View any player's active auctions.");

            this.put("#7", "Admin Command");
            this.put("command-admin-description", "Admin command for AuctionHouse.");
            this.put("command-admin-usage", "&f%command%");
            this.put("command-admin-check-header", "All auctions created by %player%");
            this.put("command-admin-check-format", " &8- #00B4DB%id% &7- &f%item% &7- &f%price% &7- %sold%");
            this.put("command-admin-delete-success", "You have deleted the auction %id%");
            this.put("command-admin-expire-success", "You have expired the auction %id%");

            this.put("#8", "General Auction Messages");
            this.put("offline-profits", "You have earned #00B4DB$%amount%&f from #00B4DB%total%&f auctions while offline.");
            this.put("auction-sold", "Your auction has been bought for #00B4DB%price%&f by #00B4DB%buyer%&f!");
            this.put("invalid-auction", "Please provide a valid auction ID.");
        }};
    }
}
