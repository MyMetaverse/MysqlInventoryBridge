package net.craftersland.bridge.inventory.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.craftersland.bridge.inventory.Main;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public class ConnectionHandler {

    private final HikariDataSource ds;

    public ConnectionHandler(Main plugin) {
        HikariConfig config = new HikariConfig();

        config.setPoolName("MysqlInventoryBridge-hikari");

        config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        config.addDataSourceProperty("serverName", plugin.getConfigHandler().getString("database.mysql.host"));
        config.addDataSourceProperty("port", plugin.getConfigHandler().getString("database.mysql.port"));
        config.addDataSourceProperty("databaseName", plugin.getConfigHandler().getString("database.mysql.databaseName"));
        config.setUsername(plugin.getConfigHandler().getString("database.mysql.user"));
        config.setPassword(plugin.getConfigHandler().getString("database.mysql.password"));

        config.setAutoCommit(true);

        config.setMaximumPoolSize(plugin.getConfigHandler().getInteger("database.mysql.maxPoolSize", 10));
        config.setMaxLifetime(300000); // 5 MINUTES
        config.setConnectionTimeout(8000); // 8 SECONDS
        config.setLeakDetectionThreshold(15000); // 15 SECONDS

        ds = new HikariDataSource(config);
    }


    public <T> T execute(ConnectionCallback<T> callback) {
        try (Connection connection = ds.getConnection()) {
            return callback.executeConnection(connection);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    public void executeVoid(Consumer<Connection> callback) {
        try (Connection connection = ds.getConnection()) {
            callback.accept(connection);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void endPool() {
        ds.close();
    }

    public interface ConnectionCallback<T> {
        T executeConnection(Connection connection) throws SQLException;
    }
}
