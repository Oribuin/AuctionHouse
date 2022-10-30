package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.manager.ConfigurationManager.Settings;
import xyz.oribuin.auctionhouse.util.PluginUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager extends Manager {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private File logFile;

    public LogManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        if (!Settings.AUCTION_LOGGING.getBoolean())
            return;

        this.logFile = new File(this.rosePlugin.getDataFolder(), "auction-logs.log");
        if (!this.logFile.exists()) {
            try {
                this.logFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Log an auction to the log file
     *
     * @param logMessage The message to log
     * @param auction    The auction to log
     */
    public void addLogMessage(LogMessage logMessage, Auction auction) {
        if (!Settings.AUCTION_LOGGING.getBoolean())
            return;

        try {
            final FileWriter fileWriter = new FileWriter(this.logFile, true);
            final BufferedWriter writer = new BufferedWriter(fileWriter);

            String date = this.dateFormat.format(new Date());
            writer.write("[" + date + "] " + this.getPlaceholders(auction).apply(logMessage.getMessage()));
            writer.newLine();
            writer.close();
            fileWriter.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void disable() {

    }

    /**
     * Get the placeholders for the auction
     *
     * @param auction The auction to get the placeholders for
     * @return The placeholders for the auction
     */
    private StringPlaceholders getPlaceholders(Auction auction) {

        StringPlaceholders.Builder builder = StringPlaceholders.builder()
                .addPlaceholder("price", auction.getPrice())
                .addPlaceholder("seller", auction.getSeller() != null ? auction.getSeller() : "N/A")
                .addPlaceholder("buyer", auction.getBuyer() != null ? auction.getBuyer() : "N/A")
                .addPlaceholder("id", auction.getId());

        if (auction.getItem() != null)
            builder.addPlaceholder("item", PluginUtils.getItemName(auction.getItem()));

        return builder.build();
    }

    public enum LogMessage {
        AUCTION_CREATED("Auction was created. Id: %id%, Seller %seller%, Price %price%, Item %item%"),
        AUCTION_SOLD("Auction was sold. Id: %id%, Seller %seller%, Buyer %buyer%, Price %price%, Item %item%"),
        AUCTION_EXPIRED("Auction was expired. Id: %id%, Seller %seller%, Item %item%, Price %price%"),
        AUCTION_DELETED("Auction was deleted. Id: %id%, Seller %seller%, Item %item%, Price %price%");

        private final String message;

        LogMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }

    }

}
