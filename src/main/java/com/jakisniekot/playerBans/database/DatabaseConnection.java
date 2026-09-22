package com.jakisniekot.playerBans.database;

import com.jakisniekot.playerBans.ErrorType;
import com.jakisniekot.playerBans.PlayerBans;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseConnection {

    private PlayerBans plugin;

    private String databaseInfo = "ERROR";
    private String DB_TYPE;
    private Connection connection;

    public DatabaseConnection(PlayerBans plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.DB_TYPE = config.getString("databaseType");
    }

    public void connect() {
        FileConfiguration config = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        Map<String, String> placeholder = new HashMap<>();

        if (Objects.equals(DB_TYPE, "MYSQL")) {

            String forceURL = config.getString("databaseConnection.forceURL");
            String user = config.getString("databaseConnection.user");
            String password = config.getString("databaseConnection.password");
            String name = config.getString("databaseConnection.name");
            String port = config.getString("databaseConnection.port");
            String host = config.getString("databaseConnection.host");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&allowPublicKeyRetrieval=true";

            if (forceURL != null) {
                url = forceURL;
                placeholder.put("{forceURL}", forceURL);
            } else if (user == null || password == null || name == null || port == null || host == null) {



                plugin.onDisable();

            } else {
                placeholder.put("{userName}", user);
                placeholder.put("{userPassword}", password);
                placeholder.put("{databaseName}", name);
                placeholder.put("{databasePort}", port);
                placeholder.put("{databaseIP}", host);
            }





            try {
                this.connection = DriverManager.getConnection(url, user, password);

                placeholder.put("{databaseStatus}", lang.getString("startup.database.status.online"));


            } catch (SQLException e) {

                switch (e.getErrorCode()) {
                    case 1045 -> placeholder.put("{databaseStatus}", lang.getString("info.database.exitCodes.1045"));
                    case 1049 -> placeholder.put("{databaseStatus}", lang.getString("info.database.exitCodes.1049"));
                    case 0, -1 -> placeholder.put("{databaseStatus}", lang.getString("info.database.status.false"));
                    default -> placeholder.put("{databaseStatus}", lang.getString("info.database.status.fatal"));
                }


            /*
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.getLogger().log(Level.INFO, String.valueOf(e.getErrorCode()));
            }, 20L * 5);
             */

            }
        } else if (Objects.equals(DB_TYPE, "SQLITE")) {

            placeholder.put("{userName}", "root");
            placeholder.put("{userPassword}", "root");
            placeholder.put("{databaseName}", "database");
            placeholder.put("{databasePort}", "none");
            placeholder.put("{databaseIP}", "localhost");
            placeholder.put("{databaseStatus}", lang.getString("startup.database.status.local"));

            File dbFile = new File(plugin.getDataFolder(), "database.db");
            if (!dbFile.exists()) {
                plugin.getDataFolder().mkdirs();
                try {
                    dbFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create database.db: " + e.getMessage());
                }
            }

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            try {
                connection = DriverManager.getConnection(url);



            } catch (SQLException e) {
                switch (e.getErrorCode()) {
                    case 1045 -> placeholder.put("{databaseStatus}", lang.getString("info.database.exitCodes.1045"));
                    case 1049 -> placeholder.put("{databaseStatus}", lang.getString("info.database.exitCodes.1049"));
                    case 0, -1 -> placeholder.put("{databaseStatus}", lang.getString("info.database.status.false"));
                    default -> placeholder.put("{databaseStatus}", lang.getString("info.database.status.fatal"));
                }
            }

        }

        if (connection == null) {
            plugin.setErrorType(ErrorType.DATABASE_NOT_LOADED);
            plugin.forceStop();
            return;
        }
    }

    public String getStatus() {
        return databaseInfo;
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTables() {

        String connectors = """
        CREATE TABLE IF NOT EXISTS connectors (
            id VARCHAR(64) PRIMARY KEY,
            max_connections INTEGER NOT NULL DEFAULT 0,
            allowed_wires VARCHAR(64),
            blacklist BOOLEAN NOT NULL DEFAULT FALSE,
            connectorType VARCHAR(64),
            itemID VARCHAR(64)
        );
        """;
/*
        String relays = """
        CREATE TABLE IF NOT EXISTS relays (
            id VARCHAR(64) PRIMARY KEY,
            max_connections INTEGER NOT NULL DEFAULT 0
        );
        """;
 */
        String wires = """
        CREATE TABLE IF NOT EXISTS wires (
            loc1 VARCHAR(64) NOT NULL,
            loc2 VARCHAR(64) NOT NULL,
            watt_transfer INTEGER NOT NULL DEFAULT 0,
            headBlocks VARCHAR(64),
            itemID VARCHAR(64),
            PRIMARY KEY (loc1, loc2)
        );
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(connectors);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(wires);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        addColumnIfMissing("wires", "headBlocks", "TEXT");
        addColumnIfMissing("wires", "itemID", "VARCHAR(64)");
        addColumnIfMissing("connectors", "itemID", "VARCHAR(64)");
    }

    private void addColumnIfMissing(String table, String column, String type) {

        try {
            boolean exists;

            if (Objects.equals(DB_TYPE, "MYSQL")) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
                    ps.setString(1, table);
                    ps.setString(2, column);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        exists = rs.getInt(1) > 0;
                    }
                }
            } else {
                // SQLite
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                    exists = false;
                    while (rs.next()) {
                        if (rs.getString("name").equalsIgnoreCase(column)) {
                            exists = true;
                            break;
                        }
                    }
                }
            }

            if (!exists) {
                try (Statement alter = connection.createStatement()) {
                    alter.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                    plugin.getLogger().info("Migrated table " + table + ": added missing column " + column);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}