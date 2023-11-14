package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.auction.OfflineProfits;
import xyz.oribuin.auctionhouse.database.migration._1_CreateInitialTables;
import xyz.oribuin.auctionhouse.database.migration._2_CreateOfflineProfitsTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class DataManager extends AbstractDataManager {

    private final Map<Integer, Auction> auctionCache = new HashMap<>();
    private final Map<UUID, OfflineProfits> offlineProfitsCache = new HashMap<>();

    public DataManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    /**
     * Load all auctions from the database
     */
    public void loadAuctions() {
        this.auctionCache.clear();
        this.offlineProfitsCache.clear();

        this.async(() -> this.databaseConnector.connect(connection -> {
            this.rosePlugin.getLogger().info("Loading auctions from database...");

            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "auctions WHERE expired = ? AND sold = ?")) {
                statement.setBoolean(1, false);
                statement.setBoolean(2, false);

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {

                    // we're saving resources by not getting all expired auctions
                    if (resultSet.getBoolean("expired") || resultSet.getBoolean("sold"))
                        continue;

                    final int id = resultSet.getInt("id");
                    final UUID seller = UUID.fromString(resultSet.getString("seller"));
                    final ItemStack item = this.deserialize(resultSet.getBytes("item"));
                    final double price = resultSet.getDouble("price");

                    // Create the auction
                    final Auction auction = new Auction(id, seller, item, price);
                    this.auctionCache.put(id, auction);
                }
            }

            this.rosePlugin.getLogger().info("Loaded " + this.auctionCache.size() + "active auctions from the database.");
            this.rosePlugin.getLogger().info("Loading offline profits from database...");

            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "offline_profits")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    final double profits = resultSet.getDouble("profits");
                    final int totalSold = resultSet.getInt("totalSold");
                    this.offlineProfitsCache.put(uuid, new OfflineProfits(profits, totalSold));
                }
            }

            this.rosePlugin.getLogger().info("Loaded " + this.offlineProfitsCache.size() + " offline profits from the database.");
        }));
    }

    /**
     * Load the user's auctions from the database
     *
     * @param uuid The user's UUID
     */
    public void loadUserAuctions(UUID uuid) {
        this.async(() -> this.databaseConnector.connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "auctions WHERE seller = ? AND expired = ? OR sold = ?")) {
                statement.setString(1, uuid.toString());
                statement.setBoolean(2, true);
                statement.setBoolean(3, true);

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final int id = resultSet.getInt("id");
                    final UUID seller = UUID.fromString(resultSet.getString("seller"));
                    final ItemStack item = this.deserialize(resultSet.getBytes("item"));
                    final double price = resultSet.getDouble("price");

                    // Create the auction
                    final Auction auction = new Auction(id, seller, item, price);
                    if (resultSet.getString("buyer") != null) {
                        auction.setBuyer(UUID.fromString(resultSet.getString("buyer")));
                    }

                    auction.setCreatedTime(resultSet.getLong("createdTime"));
                    auction.setExpiredTime(resultSet.getLong("expiredTime"));
                    auction.setSoldTime(resultSet.getLong("soldTime"));
                    auction.setSold(resultSet.getBoolean("sold"));
                    auction.setExpired(resultSet.getBoolean("expired"));
                    this.auctionCache.put(id, auction);
                }
            }
        }));
    }

    /**
     * Create a new auction in the database with the given information and return the ID
     *
     * @param uuid  The seller's UUID
     * @param item  The item to auction
     * @param price The price of the item
     */
    public void createAuction(UUID uuid, ItemStack item, double price, Consumer<Auction> callback) {
        final Auction auction = new Auction(-1, uuid, item, price);
        auction.setCreatedTime(System.currentTimeMillis());

        this.async(() -> this.databaseConnector.connect(connection -> {
            final String query = "INSERT INTO " + this.getTablePrefix() + "auctions (seller, item, price) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, uuid.toString());
                statement.setBytes(2, this.serialize(item));
                statement.setDouble(3, price);
                statement.executeUpdate();

                // Get the ID of the auction
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        auction.setId(resultSet.getInt(1));
                        this.saveAuction(auction);

                        Bukkit.getScheduler().runTask(this.rosePlugin, () -> callback.accept(auction));
                    }
                }
            }
        }));
    }

    /**
     * Save an auction to the database
     *
     * @param auction The auction to save
     */
    public void saveAuction(Auction auction) {
        this.auctionCache.put(auction.getId(), auction);

        this.async(() -> this.databaseConnector.connect(connection -> {

            // Save auction in database where id matches the auction id
            final String query = "REPLACE INTO " + this.getTablePrefix() + "auctions (id, seller, item, price, createdTime, expiredTime, soldTime, sold, expired) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, auction.getId());
                statement.setString(2, auction.getSeller().toString());
                statement.setBytes(3, this.serialize(auction.getItem()));
                statement.setDouble(4, auction.getPrice());
                statement.setLong(5, auction.getCreatedTime());
                statement.setLong(6, auction.getExpiredTime());
                statement.setLong(7, auction.getSoldTime());
                statement.setBoolean(8, auction.isSold());
                statement.setBoolean(9, auction.isExpired());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Delete an auction from the database
     *
     * @param auction The auction to delete
     */
    public void deleteAuction(Auction auction, Consumer<Auction> callback) {
        if (this.auctionCache.remove(auction.getId()) == null)
            return;

        this.async(() -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "auctions WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, auction.getId());
                int result = statement.executeUpdate();

                if (result > 0) {
                    Bukkit.getScheduler().runTask(this.rosePlugin, () -> callback.accept(auction));
                }
            }
        }));
    }

    /**
     * Save the user's profits to the database.
     *
     * @param uuid    The user's UUID
     * @param profits The profits
     */
    public void saveProfits(UUID uuid, OfflineProfits profits) {
        this.offlineProfitsCache.put(uuid, profits);

        this.async(() -> this.databaseConnector.connect(connection -> {
            final String query = "REPLACE INTO " + this.getTablePrefix() + "offline_profits (uuid, profits, totalSold) VALUES (?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setDouble(2, profits.profit());
                statement.setInt(3, profits.sold());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Serializes an item stack to a byte array
     *
     * @param itemStack The item stack to serialize
     * @return The serialized item stack
     */
    private byte[] serialize(ItemStack itemStack) {
        byte[] data = new byte[0];
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(stream)) {
            oos.writeObject(itemStack);
            data = stream.toByteArray();
        } catch (IOException ignored) {
        }

        return data;
    }

    // Deserialize an ItemStack from a byte array using Bukkit serialization
    private ItemStack deserialize(byte[] data) {
        ItemStack itemStack = null;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(stream)) {
            itemStack = (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
        }

        return itemStack;
    }


    public Map<Integer, Auction> getAuctionCache() {
        return this.auctionCache;
    }

    public Map<UUID, OfflineProfits> getOfflineProfitsCache() {
        return offlineProfitsCache;
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return List.of(_1_CreateInitialTables.class, _2_CreateOfflineProfitsTable.class);
    }

    public void async(Runnable runnable) {
        this.rosePlugin.getServer().getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }
}
