package xyz.oribuin.auctionhouse.config;

import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.config.SettingHolder;
import dev.rosewood.rosegarden.config.SettingSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import xyz.oribuin.auctionhouse.AuctionHouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.rosewood.rosegarden.config.SettingSerializers.BOOLEAN;
import static dev.rosewood.rosegarden.config.SettingSerializers.DOUBLE;
import static dev.rosewood.rosegarden.config.SettingSerializers.MATERIAL_LIST;
import static dev.rosewood.rosegarden.config.SettingSerializers.STRING;


public class Setting implements SettingHolder {

    public static final Setting INSTANCE = new Setting();
    private static final List<RoseSetting<?>> KEYS = new ArrayList<>();

    // Auction Listing Options
    public static RoseSetting<Double> LIST_PRICE = create("list-price", DOUBLE, 100.0, "The price of putting an item on the auction");
    public static RoseSetting<Double> LIST_MAX = create("list-max", DOUBLE, 1000000.0, "The maximum amount of items that can be listed at once");
    public static RoseSetting<Double> LIST_MIN = create("list-min", DOUBLE, 1.0, "The minimum amount of items that can be listed at once");
    public static RoseSetting<Double> LIST_COOLDOWN = create("list-cooldown", DOUBLE, 10.0, "The cooldown in seconds between listing items");
    public static RoseSetting<String> LIST_TIME = create("list-time", STRING, "1d", "The time before an auction expires");
    public static RoseSetting<Double> LIST_TAX = create("list-tax", DOUBLE, 0.05, "The tax on the price of listing an item");

    // Auction House Options
    public static RoseSetting<String> MONEY_LOCALE = create("money-locale", STRING, Locale.US.toLanguageTag(), "The locale to use for money formatting");
    public static RoseSetting<Boolean> AUCTION_LOGGING = create("auction-logging", BOOLEAN, false, "Should auction logs be logged?");
    public static RoseSetting<List<Material>> DISABLED_MATERIALS = create("disabled-materials", MATERIAL_LIST, List.of(Material.BEDROCK, Material.END_PORTAL_FRAME), "The materials that are disabled from being listed");
    public static RoseSetting<String> DATE_FORMAT = create("date-format", STRING, "hh:mma 'on' dd MMM yy", "The date format to use for dates in the plugin");

    private static <T> RoseSetting<T> create(String key, SettingSerializer<T> serializer, T defaultValue, String... comments) {
        RoseSetting<T> setting = RoseSetting.ofBackedValue(key, AuctionHouse.get(), serializer, defaultValue, comments);
        KEYS.add(setting);
        return setting;
    }

    private static RoseSetting<ConfigurationSection> create(String key, String... comments) {
        RoseSetting<ConfigurationSection> setting = RoseSetting.ofBackedSection(key, AuctionHouse.get(), comments);
        KEYS.add(setting);
        return setting;
    }

    public static List<RoseSetting<?>> getKeys() {
        return KEYS;
    }

    @Override
    public List<RoseSetting<?>> get() {
        return KEYS;
    }
}