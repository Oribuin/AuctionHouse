package xyz.oribuin.auctionhouse.util;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.oribuin.auctionhouse.hook.PAPI;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PluginUtils {


    final DecimalFormat formatter;

    private PluginUtils() {

        this.formatter = new DecimalFormat("#,###.##");
    }

    // parse a string such as 1w 2d 3h 4m 5s into milliseconds
    public static long parseTime(String time) {
        String[] parts = time.split(" ");
        long totalSeconds = 0;

        for (String part : parts) {

            // get the last character
            char lastChar = part.charAt(part.length() - 1);
            String num = part.substring(0, part.length() - 1);
            if (num.isEmpty())
                continue;

            int amount;
            try {
                amount = Integer.parseInt(num);
            } catch (NumberFormatException e) {
                continue;
            }

            switch (lastChar) {
                case 'w' -> totalSeconds += amount * 604800L;
                case 'd' -> totalSeconds += amount * 86400L;
                case 'h' -> totalSeconds += amount * 3600L;
                case 'm' -> totalSeconds += amount * 60L;
                case 's' -> totalSeconds += amount;
            }
        }

        return totalSeconds * 1000;
    }

    public static String formatCurrency(double amount) {
        return DecimalFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    /**
     * Format a time in milliseconds into a string
     *
     * @param time Time in milliseconds
     * @return Formatted time
     */
    public static String formatTime(long time) {
        long totalSeconds = time / 1000;

        if (totalSeconds <= 0)
            return "";

        long days = (int) Math.floor(totalSeconds / 86400.0);
        totalSeconds %= 86400;

        long hours = (int) Math.floor(totalSeconds / 3600.0);
        totalSeconds %= 3600;

        long minutes = (int) Math.floor(totalSeconds / 60.0);
        long seconds = (totalSeconds % 60);

        final StringBuilder builder = new StringBuilder();

        if (days > 0)
            builder.append(days).append("d, ");

        builder.append(hours).append("h, ");
        builder.append(minutes).append("m, ");
        builder.append(seconds).append("s");

        return builder.toString();
    }


    /**
     * Get a configuration value or default from the file config
     *
     * @param config The configuration file.
     * @param path   The path to the value
     * @param def    The default value if the original value doesnt exist
     * @return The config value or default value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(CommentedFileConfiguration config, String path, T def) {
        return config.get(path) != null ? (T) config.get(path) : def;
    }

    /**
     * Get a value from a configuration section.
     *
     * @param section The configuration section
     * @param path    The path to the option.
     * @param def     The default value for the option.
     * @return The config option or the default.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(CommentedConfigurationSection section, String path, T def) {
        return section.get(path) != null ? (T) section.get(path) : def;
    }

    /**
     * Get ItemStack from CommentedFileSection path
     *
     * @param config       The CommentedFileSection
     * @param path         The path to the item
     * @param player       The player
     * @param placeholders The placeholders
     * @return The itemstack
     */
    public static ItemStack getItemStack(CommentedConfigurationSection config, String path, Player player, StringPlaceholders placeholders) {


        Material material = Material.getMaterial(get(config, path + ".material", "STONE"));
        if (material == null) {
            return new ItemStack(Material.BARRIER);
        }

        // Format the item lore
        List<String> lore = get(config, path + ".lore", List.of());
        lore = lore.stream().map(s -> format(player, s, placeholders)).collect(Collectors.toList());

        // Get item flags
        ItemFlag[] flags = get(config, path + ".flags", new ArrayList<String>())
                .stream()
                .map(String::toUpperCase)
                .map(ItemFlag::valueOf)
                .toArray(ItemFlag[]::new);

        // Build the item stack
        ItemBuilder builder = new ItemBuilder(material)
                .setName(format(player, get(config, path + ".name", null), placeholders))
                .setLore(lore)
                .setAmount(Math.max(get(config, path + ".amount", 1), 1))
                .setFlags(flags)
                .glow(get(config, path + ".glow", false))
                .setTexture(get(config, path + ".texture", null))
                .setPotionColor(fromHex(get(config, path + ".potion-color", null)))
                .setModel(get(config, path + ".model-data", -1));

        // Get item owner
        String owner = get(config, path + ".owner", null);
        if (owner != null) {
            if (owner.equalsIgnoreCase("self"))
                builder.setOwner(player);
            else
                builder.setOwner(Bukkit.getOfflinePlayer(UUID.fromString(PAPI.apply(player, owner))));
        }

        // Get item enchantments
        final CommentedConfigurationSection enchants = config.getConfigurationSection(path + "enchants");
        if (enchants != null) {
            enchants.getKeys(false).forEach(key -> {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
                if (enchantment == null)
                    return;

                builder.addEnchant(enchantment, enchants.getInt(key));
            });
        }

        return builder.create();
    }

    /**
     * Get ItemStack from CommentedFileSection path
     *
     * @param config The CommentedFileSection
     * @param path   The path to the item
     * @return The itemstack
     */
    public static ItemStack getItemStack(CommentedConfigurationSection config, String path) {
        return getItemStack(config, path, null, StringPlaceholders.empty());
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The string to format
     * @return The formatted string
     */
    public static String format(Player player, String text) {
        return format(player, text, StringPlaceholders.empty());
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    public static String format(Player player, String text, StringPlaceholders placeholders) {
        return HexUtils.colorify(PAPI.apply(player, placeholders.apply(text)));
    }


    /**
     * Get a bukkit color from a hex code
     *
     * @param hex The hex code
     * @return The bukkit color
     */
    public static Color fromHex(String hex) {
        if (hex == null)
            return Color.BLACK;

        java.awt.Color awtColor;
        try {
            awtColor = java.awt.Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }

        return Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
    }

    public static String getItemName(ItemStack item) {
        String displayName = item.getType().name();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName())
            displayName = meta.getDisplayName();

        return ChatColor.stripColor(displayName);
    }

}
