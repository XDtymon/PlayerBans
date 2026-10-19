package com.jakisniekot.playerBans.database;

import com.jakisniekot.playerBans.ErrorType;
import com.jakisniekot.playerBans.PlayerBans;
import com.jakisniekot.playerBans.handler.BanHandler;
import com.jakisniekot.playerBans.handler.IPBanHandler;
import com.jakisniekot.playerBans.handler.MuteHandler;
import com.jakisniekot.playerBans.util.SingleConnectionDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Database {

    private PlayerBans plugin;

    private String databaseInfo = "ERROR";
    private String DB_TYPE;
    private Connection connection;

    private BanHandler banHandler;
    private MuteHandler muteHandler;
    private IPBanHandler ipBanHandler;

    public Database(PlayerBans plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.DB_TYPE = config.getString("databaseType");

        connect();
        createTables();

        DataSource ds = new SingleConnectionDataSource(connection);
        this.banHandler = new BanHandler(ds);
        this.muteHandler = new MuteHandler(ds);
        this.ipBanHandler = new IPBanHandler(ds);
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

    private boolean isSQLite() throws SQLException {
        return getConnection().getMetaData()
                .getDatabaseProductName()
                .equalsIgnoreCase("SQLite");
    }

    public void createTables() {
        try {
            createPunishmentTable("ip_bans", "idx_bans_ip");
            createPunishmentTable("player_bans", "idx_bans_player");
            createPunishmentTable("player_mutes", "idx_mutes_player");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createPunishmentTable(String table, String indexName) throws SQLException {
        boolean sqlite = isSQLite();

        String inlineIndex = sqlite ? "" : ",\n    INDEX " + indexName + " (player_uuid)";

        String createTable = """
        CREATE TABLE IF NOT EXISTS %s (
            punishment_id          VARCHAR(12)  NOT NULL PRIMARY KEY,
            target     CHAR(36)     NOT NULL,
            issued_at   BIGINT       NOT NULL,
            duration_ms BIGINT       NOT NULL,
            reason      VARCHAR(255) NOT NULL,
            issuer_uuid CHAR(36)     NOT NULL%s
        )
        """.formatted(table, inlineIndex);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createTable);
            if (sqlite) {
                stmt.execute("CREATE INDEX IF NOT EXISTS " + indexName
                        + " ON " + table + "(target)");
            }
        }
    }

    public IPBanHandler ipBans() {
        return ipBanHandler;
    }

    public BanHandler bans() {
        return banHandler;
    }

    public MuteHandler mutes() {
        return muteHandler;
    }
}