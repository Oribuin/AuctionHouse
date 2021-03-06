package xyz.oribuin.auctionhouse.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import xyz.oribuin.auctionhouse.auction.Auction;
import xyz.oribuin.auctionhouse.auction.OfflineProfits;
import xyz.oribuin.auctionhouse.database.migration._1_CreateInitialTables;
import xyz.oribuin.auctionhouse.database.migration._2_CreateOfflineProfitsTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

            try (var statement = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "auctions")) {
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

            this.rosePlugin.getLogger().info("Loaded " + this.auctionCache.size() + " auctions from the database.");
            this.rosePlugin.getLogger().info("Loading offline profits from database...");

            try (var statement = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "offline_profits")) {
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
            try (var statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, uuid.toString());
                statement.setBytes(2, this.serialize(item));
                statement.setDouble(3, price);
                statement.executeUpdate();

                // Get the ID of the auction
                try (var resultSet = statement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        auction.setId(resultSet.getInt(1));
                        this.saveAuction(auction);
                        this.rosePlugin.getServer().getScheduler().runTask(this.rosePlugin, () -> callback.accept(auction));
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
            try (var statement = connection.prepareStatement(query)) {
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
    public void deleteAuction(Auction auction) {
        this.auctionCache.remove(auction.getId());
        this.async(() -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "auctions WHERE id = ?";
            try (var statement = connection.prepareStatement(query)) {
                statement.setInt(1, auction.getId());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Get an auction from the cache
     *
     * @param id The auction ID
     * @return The auction
     */
    public Optional<Auction> getAuction(int id) {
        return Optional.ofNullable(this.auctionCache.get(id));
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return List.of(_1_CreateInitialTables.class, _2_CreateOfflineProfitsTable.class);
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

            try (var statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setDouble(2, profits.getProfit());
                statement.setInt(3, profits.getSold());
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

    /**
     * Runs an async task
     *
     * @param runnable The runnable to run
     */
    public void async(Runnable runnable) {
        this.rosePlugin.getServer().getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }
}
