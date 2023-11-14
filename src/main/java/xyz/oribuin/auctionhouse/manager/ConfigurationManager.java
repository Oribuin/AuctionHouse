package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.manager.AbstractConfigurationManager;
import xyz.oribuin.auctionhouse.AuctionHouse;

import java.util.Arrays;
import java.util.Locale;

public class ConfigurationManager extends AbstractConfigurationManager {

    public ConfigurationManager(RosePlugin rosePlugin) {
        super(rosePlugin, Settings.class);
    }

    public enum Settings implements RoseSetting {

        // Auction Listing Options
        LIST_PRICE("list-price", 100.0, "The price of putting an item on the auction"),
        LIST_MAX("list-max", 1000000.0, "The maximum amount of items that can be listed at once"),
        LIST_MIN("list-min", 1.0, "The minimum amount of items that can be listed at once"),
        LIST_COOLDOWN("list-cooldown", 10.0, "The cooldown in seconds between listing items"),
        LIST_TIME("list-time", "1d", "The time before an auction expires"),
        LIST_TAX("list-tax", 0.05, "The tax on the price of listing an item"),

        // Auction House Options
        MONEY_LOCALE("money-locale", Locale.US.toLanguageTag(), "The locale to use for money formatting"),
        AUCTION_LOGGING("auction-logging", false, "Should auction logs be logged?"),
        DISABLED_MATERIALS("disabled-materials", Arrays.asList("BEDROCK", "END_PORTAL_FRAME"), "The materials that are disabled from being listed"),
        ;

        private final String key;
        private final Object defaultValue;
        private final String[] comments;
        private Object value = null;

        Settings(String key, Object defaultValue, String... comments) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comments = comments;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Object getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public String[] getComments() {
            return this.comments;
        }

        @Override
        public Object getCachedValue() {
            return this.value;
        }

        @Override
        public void setCachedValue(Object value) {
            this.value = value;
        }

        @Override
        public CommentedFileConfiguration getBaseConfig() {
            return AuctionHouse.get().getManager(ConfigurationManager.class).getConfig();
        }

    }

    @Override
    protected String[] getHeader() {
        return new String[]{"   _____                 __  .__                ___ ___                             ",
                "  /  _  \\  __ __   _____/  |_|__| ____   ____  /   |   \\  ____  __ __  ______ ____  ",
                " /  /_\\  \\|  |  \\_/ ___\\   __\\  |/  _ \\ /    \\/    ~    \\/  _ \\|  |  \\/  ___// __ \\ ",
                "/    |    \\  |  /\\  \\___|  | |  (  <_> )   |  \\    Y    (  <_> )  |  /\\___ \\\\  ___/ ",
                "\\____|__  /____/  \\___  >__| |__|\\____/|___|  /\\___|_  / \\____/|____//____  >\\___  >",
                "        \\/            \\/                    \\/       \\/                   \\/     \\/ "};
    }
}
