package xyz.oribuin.auctionhouse.database.migration;

import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class _2_CreateOfflineProfitsTable extends DataMigration {

    public _2_CreateOfflineProfitsTable() {
        super(2);
    }

    @Override
    public void migrate(DatabaseConnector connector, Connection connection, String tablePrefix) throws SQLException {
        // Store all the player's offline profits while they are offline
        final String createOfflineProfitsTable = "CREATE TABLE " + tablePrefix + "offline_profits (" +
                                                 "uuid VARCHAR(36) PRIMARY KEY, " +
                                                 "profits DOUBLE DEFAULT 0.0 NOT NULL," +
                                                 "totalSold INTEGER DEFAULT 0 NOT NULL)";

        try (PreparedStatement statement = connection.prepareStatement(createOfflineProfitsTable)) {
            statement.executeUpdate();
        }
    }

}
