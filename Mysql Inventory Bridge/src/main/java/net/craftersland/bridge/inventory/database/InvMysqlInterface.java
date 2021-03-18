package net.craftersland.bridge.inventory.database;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.objects.DatabaseInventoryData;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class InvMysqlInterface {

    private final Main main;

    public InvMysqlInterface(Main main) {
        this.main = main;
    }

    public boolean hasAccount(UUID uniqueID) {
        return main.getConnectionHandler().execute(conn -> {
            if (conn != null) {
                String sql = "SELECT `player_uuid` FROM `" + main.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `player_uuid` = ? LIMIT 1";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(sql)) {
                    preparedUpdateStatement.setString(1, uniqueID.toString());

                    try (ResultSet result = preparedUpdateStatement.executeQuery()) {
                        if (result.next())
                            return true;
                    } catch (SQLException exception) {
                        exception.printStackTrace();

                        return false;
                    }

                } catch (SQLException exception) {
                    exception.printStackTrace();

                    return false;
                }
            }

            return false;
        });

    }

    public boolean createAccount(UUID uniqueId) {
        return main.getConnectionHandler().execute(conn -> {
            if(conn == null)
                return false;

            String sql = "INSERT INTO `" + main.getConfigHandler().getString("database.mysql.tableName") + "`(`player_uuid`, `inventory`, `armor`, `sync_complete`, `last_seen`) " + "VALUES(?, ?, ?, ?, ?)";

            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, uniqueId.toString());
                preparedStatement.setString(2, "none");
                preparedStatement.setString(3, "none");
                preparedStatement.setString(4, "true");
                preparedStatement.setString(5, String.valueOf(System.currentTimeMillis()));

                preparedStatement.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public boolean setData(UUID uniqueId, String inventory, String armor, String syncComplete) {
        if (!hasAccount(uniqueId)) {
            createAccount(uniqueId);
        }

        main.getConnectionHandler().execute(conn -> {
            if (conn != null) {
                String data = "UPDATE `" + main.getConfigHandler().getString("database.mysql.tableName") + "` " + "SET `inventory` = ?" + ", `armor` = ?" + ", `sync_complete` = ?" + ", `last_seen` = ?" + " WHERE `player_uuid` = ?";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(data)) {

                    preparedUpdateStatement.setString(1, inventory);
                    preparedUpdateStatement.setString(2, armor);
                    preparedUpdateStatement.setString(3, syncComplete);
                    preparedUpdateStatement.setString(4, String.valueOf(System.currentTimeMillis()));
                    preparedUpdateStatement.setString(5, uniqueId.toString());

                    preparedUpdateStatement.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return false;
        });

        return false;
    }

    public boolean setSyncStatus(Player player, String syncStatus) {

        return main.getConnectionHandler().execute(conn -> {
            if (conn != null) {
                String data = "UPDATE `" + main.getConfigHandler().getString("database.mysql.tableName") + "` " + "SET `sync_complete` = ?" + ", `last_seen` = ?" + " WHERE `player_uuid` = ?";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(data)) {

                    preparedUpdateStatement.setString(1, syncStatus);
                    preparedUpdateStatement.setString(2, String.valueOf(System.currentTimeMillis()));
                    preparedUpdateStatement.setString(3, player.getUniqueId().toString());

                    preparedUpdateStatement.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return false;
        });

    }

    public DatabaseInventoryData getData(UUID uniqueId) {
        if (!hasAccount(uniqueId)) {
            createAccount(uniqueId);
        }

        return main.getConnectionHandler().execute(conn -> {
            if (conn != null) {
                String sql = "SELECT * FROM `" + main.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `player_uuid` = ? LIMIT 1";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(sql)){

                    preparedUpdateStatement.setString(1, uniqueId.toString());

                    try (ResultSet result = preparedUpdateStatement.executeQuery()) {
                        if (result.next()) {
                            return new DatabaseInventoryData(result.getString("inventory"), result.getString("armor"), result.getString("sync_complete"), result.getString("last_seen"));
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return null;
        });
    }

}
