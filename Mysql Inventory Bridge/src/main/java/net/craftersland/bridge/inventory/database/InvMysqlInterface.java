package net.craftersland.bridge.inventory.database;

import net.craftersland.bridge.inventory.Main;
import net.craftersland.bridge.inventory.encoder.EncodeResult;
import net.craftersland.bridge.inventory.objects.DatabaseInventoryData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class InvMysqlInterface {

    private final Main main;

    public InvMysqlInterface(Main main) {
        this.main = main;
    }

    public boolean hasAccount(Connection conn, UUID uniqueID) {
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

    }

    public boolean createAccount(Connection conn, UUID uniqueId) {
        if (conn == null)
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
    }

    public boolean setData(UUID uniqueId, EncodeResult inventory, EncodeResult armor) {
        return main.getConnectionHandler().execute(conn -> {
            if (!hasAccount(conn, uniqueId))
                createAccount(conn, uniqueId);
            return updatePlayer(uniqueId, inventory, armor, conn);
        });
    }


    public List<UUID> setData(Collection<Object[]> uuids) {
        List<UUID> failed = new ArrayList<>();

        main.getConnectionHandler().executeVoid(conn -> {
            for (Object[] player : uuids) {
                if(player == null || player.length == 0)
                    continue;

                boolean result = updatePlayer((UUID) player[0],
                        (EncodeResult) player[1],
                        (EncodeResult) player[2],
                        conn);

                if (!result)
                    failed.add((UUID) player[0]);
            }
        });

        return failed;
    }

    public boolean updatePlayer(UUID uniqueId, EncodeResult inventory, EncodeResult armor, Connection conn) {
        if (conn != null) {
            if (!hasAccount(conn, uniqueId))
                createAccount(conn, uniqueId);

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
    }

    public DatabaseInventoryData getData(UUID uniqueId) {
        return main.getConnectionHandler().execute(conn -> {
            if (!hasAccount(conn, uniqueId)) {
                createAccount(conn, uniqueId);
            }

            if (conn != null) {
                String sql = "SELECT * FROM `" + main.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `player_uuid` = ? LIMIT 1";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(sql)) {

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

        if (main.getConnectionHandler() == null)
            data.accept(null);

        main.getConnectionHandler().executeVoid(conn -> {
            if (conn != null) {
                if (!hasAccount(conn, uniqueId)) {
                    createAccount(conn, uniqueId);
                }

                String sql = "SELECT * FROM `" + main.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `player_uuid` = ? LIMIT 1";
                try (PreparedStatement preparedUpdateStatement = conn.prepareStatement(sql)) {

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
