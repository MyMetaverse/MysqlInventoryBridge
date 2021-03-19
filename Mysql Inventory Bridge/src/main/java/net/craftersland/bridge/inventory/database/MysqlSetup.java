package net.craftersland.bridge.inventory.database;

import net.craftersland.bridge.inventory.Main;
import org.bukkit.Bukkit;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlSetup {
	
	private final Main eco;
	
	public MysqlSetup(Main eco) {
		this.eco = eco;
		setupDatabase();
		updateTables();
		databaseMaintenanceTask();
	}
	
	public void setupDatabase() {
	    eco.getConnectionHandler().executeVoid(connection -> {
			if (connection != null) {
				String data = "CREATE TABLE IF NOT EXISTS `" + eco.getConfigHandler().getString("database.mysql.tableName") + "` " +
						"(id int(10) AUTO_INCREMENT, " +
						"player_uuid char(36) NOT NULL UNIQUE, " +
						"inventory LONGTEXT NOT NULL, " +
						"armor LONGTEXT NOT NULL, " +
						"last_seen char(13) NOT NULL, " +
						"encode varchar(15) NULL, " +
						"PRIMARY KEY(id));";

				try (PreparedStatement query = connection.prepareStatement(data)) {
					query.execute();
				} catch (SQLException e) {
					e.printStackTrace();
					Main.log.severe("Error creating tables! Error: " + e.getMessage());
				}
			}
		});
	}
	
	public void closeConnection() {
		Main.log.info("Closing database connection...");
		eco.getConnectionHandler().endPool();
	}
	
	private void updateTables() {
	    eco.getConnectionHandler().executeVoid(conn -> {
			if (conn != null) {
				DatabaseMetaData md;
				ResultSet rs1 = null;
				ResultSet rs2 = null;
				ResultSet rs3;
				PreparedStatement query1 = null;
				PreparedStatement query2 = null;
				PreparedStatement query3;

				try {
					md = conn.getMetaData();
					rs1 = md.getColumns(null, null, eco.getConfigHandler().getString("database.mysql.tableName"), "inventory");
					if (rs1.next()) {
						if (rs1.getString("TYPE_NAME").matches("VARCHAR")) {
							String data = "ALTER TABLE `" + eco.getConfigHandler().getString("database.mysql.tableName") + "` MODIFY inventory LONGTEXT NOT NULL;";
							query1 = conn.prepareStatement(data);
							query1.execute();
						}
					}
					rs2 = md.getColumns(null, null, eco.getConfigHandler().getString("database.mysql.tableName"), "armor");
					if (rs2.next()) {
						if (rs2.getString("TYPE_NAME").matches("VARCHAR")) {
							String data = "ALTER TABLE `" + eco.getConfigHandler().getString("database.mysql.tableName") + "` MODIFY armor LONGTEXT NOT NULL;";
							query2 = conn.prepareStatement(data);
							query2.execute();
						}
					}

				} catch (Exception e) {
					Main.log.severe("Error updating table! Error: " + e.getMessage());
				} finally {
					try {
						if (query1 != null) {
							query1.close();
						}
						if (rs1 != null) {
							rs1.close();
						}
						if (query2 != null) {
							query2.close();
						}
						if (rs2 != null) {
							rs2.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

	}
	
	private void databaseMaintenanceTask() {
		if (eco.getConfigHandler().getBoolean("database.maintenance.enabled")) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(eco, () ->
			    eco.getConnectionHandler().executeVoid(conn -> {
					if (conn != null) {
						long inactivityDays = Long.parseLong(eco.getConfigHandler().getString("database.maintenance.inactivity"));
						long inactivityMils = inactivityDays * 24 * 60 * 60 * 1000;
						long currentTime = System.currentTimeMillis();
						long inactiveTime = currentTime - inactivityMils;
						Main.log.info("Database maintenance task started...");
						PreparedStatement preparedStatement = null;
						try {
							String sql = "DELETE FROM `" + eco.getConfigHandler().getString("database.mysql.tableName") + "` WHERE `last_seen` < ?";
							preparedStatement = conn.prepareStatement(sql);
							preparedStatement.setString(1, String.valueOf(inactiveTime));
							preparedStatement.execute();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								if (preparedStatement != null) {
									preparedStatement.close();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Main.log.info("Database maintenance complete!");
					}
				}), 100 * 20L);
		}
	}

}
