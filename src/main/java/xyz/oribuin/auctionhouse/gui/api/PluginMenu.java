package xyz.oribuin.auctionhouse.gui.api;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.auctionhouse.manager.LocaleManager;
import xyz.oribuin.auctionhouse.util.AuctionUtils;
import xyz.oribuin.auctionhouse.util.FileUtils;

import java.io.File;

public abstract class PluginMenu {

    protected final RosePlugin rosePlugin;
    protected final String menuName;
    protected CommentedFileConfiguration config;

    public PluginMenu(RosePlugin rosePlugin, String menuName) {
        this.rosePlugin = rosePlugin;
        this.menuName = menuName;
    }
    /**
     * Create the menu file if it doesn't exist and add the default values
     */
    public void load() {
        final File menuFile = FileUtils.createFile(this.rosePlugin, "menu", this.menuName + ".yml");
        this.config = CommentedFileConfiguration.loadConfiguration(menuFile);
        this.config.save(menuFile);
    }

    /**
     * Create a GUI for the given player without pages
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    @NotNull
    protected final Gui createGUI(Player player) {
        final int rows = this.config.getInt("gui-settings.rows");
        final String title = this.config.getString("gui-settings.title", "Missing Title");

        return Gui.gui()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, title))
                .disableAllInteractions()
                .create();
    }

    /**
     * Create a paged GUI for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    @NotNull
    protected final PaginatedGui createPagedGUI(Player player) {
        if (this.config.getBoolean("gui-settings.scrolling-gui", false)) {
            return this.createScrollingGui(player);
        }

        final int rows = this.config.getInt("gui-settings.rows");
        final String preTitle = this.config.getString("gui-settings.pre-title", "Missing Pre Title");

        return Gui.paginated()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, preTitle))
                .disableAllInteractions()
                .create();
    }


    /**
     * Scrolling gui for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    @NotNull
    protected final ScrollingGui createScrollingGui(Player player) {
        final int rows = this.config.getInt("gui-settings.rows");
        final String preTitle = this.config.getString("gui-settings.pre-title", "Missing Pre Title");
        final ScrollType scrollType = AuctionUtils.getEnum(
                ScrollType.class,
                this.config.getString("gui-settings.scroll-type"),
                ScrollType.VERTICAL
        );

        return Gui.scrolling()
                .scrollType(scrollType)
                .rows(rows == 0 ? 6 : rows)
                .pageSize(0)
                .title(this.format(player, preTitle))
                .disableAllInteractions()
                .create();
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The string to format
     * @return The formatted string
     */
    protected final Component format(Player player, String text) {
        final String result = this.rosePlugin.getManager(LocaleManager.class).format(
                player,
                text,
                StringPlaceholders.empty()
        );

        return Component.text(result);
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    protected final Component format(Player player, String text, StringPlaceholders placeholders) {
        final String result = this.rosePlugin.getManager(LocaleManager.class).format(
                player,
                text,
                StringPlaceholders.empty()
        );

        return Component.text(result);
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The text to format
     * @return The formatted string
     */
    @NotNull
    protected final String formatString(Player player, String text) {
        return this.rosePlugin.getManager(LocaleManager.class).format(
                player,
                text,
                StringPlaceholders.empty()
        );
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    @NotNull
    protected final String formatString(Player player, String text, StringPlaceholders placeholders) {
        return this.rosePlugin.getManager(LocaleManager.class).format(
                player,
                text,
                placeholders
        );
    }

    /**
     * Get the page placeholders for the gui
     *
     * @param baseGui The gui
     * @return The page placeholders
     */
    protected StringPlaceholders getPagePlaceholders(BaseGui baseGui) {
        if (!(baseGui instanceof PaginatedGui gui))
            return StringPlaceholders.empty();

        return StringPlaceholders.builder()
                .add("page", gui.getCurrentPageNum())
                .add("total", Math.max(gui.getPagesNum(), 1))
                .add("next", gui.getNextPageNum())
                .add("previous", gui.getPrevPageNum())
                .build();

    }

    /**
     * Update the title of the gui
     *
     * @param gui The gui
     */
    public final void updateTitle(final BaseGui gui) {
        final String newTitle = this.config.getString("gui-settings.title", "Missing Title (menus/" + this.menuName + ".yml)");

        this.sync(() -> gui.updateTitle(this.formatString(null, newTitle, this.getPagePlaceholders(gui))));
    }

    public final void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }

    public final void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(this.rosePlugin, runnable);
    }

    /**
     * @return Whether the title should be updated (Used for page placeholders)
     */
    public boolean reloadTitle() {
        return this.config.getBoolean("gui-settings.update-title", true);
    }

    /**
     * @return Whether the gui should be updated asynchronously
     */
    public boolean addPagesAsynchronously() {
        return this.config.getBoolean("gui-settings.add-pages-asynchronously", true);
    }

}
