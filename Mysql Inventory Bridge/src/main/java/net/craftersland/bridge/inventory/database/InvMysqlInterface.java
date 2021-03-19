package net.craftersland.bridge.inventory.database;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.encoder.EncodeResult;
import net.craftersland.bridge.inventory.objects.DatabaseInventoryData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

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

            String sql = "INSERT INTO `" + main.getConfigHandler().getString("database.mysql.tableName") + "`(`player_uuid`, `inventory`, `armor`, `last_seen`) " + "VALUES(?, ?, ?, ?)";

            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, uniqueId.toString());
                preparedStatement.setString(2, "none");
                preparedStatement.setString(3, "none");
                preparedStatement.setLong(4, System.currentTimeMillis());

                preparedStatement.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public boolean setData(UUID uniqueId, EncodeResult inventory, EncodeResult armor) {
        if (!hasAccount(uniqueId)) {
            createAccount(uniqueId);
        }

        main.getConnectionHandler().execute(conn -> {
            if (conn != null) {
                String data = "UPDATE `" + main.getConfigHandler().getString("database.mysql.tableName") + "` " + "SET `inventory` = ?" +
                        ", `armor` = ?"
                        + ", `last_seen` = ?"
                        + ", `encode` = ?"
                        + " WHERE `player_uuid` = ?";

                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(data)) {

                    String invString = inventory != null ? inventory.getResult() : "none";
                    String armorString = armor != null ? armor.getResult() : "none";

                    preparedUpdateStatement.setString(1, invString);
                    preparedUpdateStatement.setString(2, armorString);
                    preparedUpdateStatement.setLong(3, System.currentTimeMillis());
                    preparedUpdateStatement.setString(4, inventory != null ? inventory.getCodec() : armor != null ? armor.getCodec() : null);
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
                            return new DatabaseInventoryData(
                                    result.getString("inventory"),
                                    result.getString("armor"),
                                    result.getLong("last_seen"),
                                    result.getString("encode"));
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


    public void getData(UUID uniqueId, Consumer<DatabaseInventoryData> data) {
        if (!hasAccount(uniqueId)) {
            createAccount(uniqueId);
        }

        if(main.getConnectionHandler() == null)
            data.accept(null);

        main.getConnectionHandler().executeVoid(conn -> {
            if (conn != null) {
                String sql = "SELECT * FROM `" + main.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `player_uuid` = ? LIMIT 1";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(sql)){

                    preparedUpdateStatement.setString(1, uniqueId.toString());

                    try (ResultSet result = preparedUpdateStatement.executeQuery()) {
                        if (result.next()) {
                            DatabaseInventoryData did = new DatabaseInventoryData(
                                    result.getString("inventory"),
                                    result.getString("armor"),
                                    result.getLong("last_seen"),
                                    result.getString("encode")
                            );
                            data.accept(did);
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    data.accept(null);
                }
            }
            data.accept(null);
        });
    }

}
