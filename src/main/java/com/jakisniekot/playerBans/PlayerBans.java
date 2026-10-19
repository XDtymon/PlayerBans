package com.jakisniekot.playerBans;

import com.jakisniekot.playerBans.command.*;
import com.jakisniekot.playerBans.database.Database;
import com.jakisniekot.playerBans.lang.LangFileHandler;
import com.jakisniekot.playerBans.listener.PlayerChatListener;
import com.jakisniekot.playerBans.listener.PlayerJoinListener;
import com.jakisniekot.playerBans.util.ScreenUtil;
import com.jakisniekot.playerBans.util.TextUtility;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public final class PlayerBans extends JavaPlugin {

    private PlayerBans playerBans;

    private LangFileHandler langFileHandler;
    private Database database;
    private PlayerActions playerActions;
    private ScreenUtil screenUtil;

    private List<String> playerNicks = new ArrayList<>();

    private ErrorType errorType = ErrorType.NONE;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //Plugin
        playerBans = this;

        //Config
        saveDefaultConfig();

        //Lang
        this.langFileHandler = new LangFileHandler(playerBans);
        langFileHandler.setupLangFiles();

        //Database
        Database database = new Database(this);

        this.database = database;



        this.playerActions = new PlayerActions(database, this);
        this.screenUtil = new ScreenUtil(this);
        //
        getCommand("ban").setExecutor(new banExecutor(this));
        getCommand("unban").setExecutor(new unbanExecutor(this));
        getCommand("mute").setExecutor(new muteExecutor(this));
        getCommand("unmute").setExecutor(new unmuteExecutor(this));

        getCommand("ban").setTabCompleter(new punishTabber(this));
        getCommand("unban").setTabCompleter(new unpunishTabber(this));
        getCommand("mute").setTabCompleter(new punishTabber(this));
        getCommand("unmute").setTabCompleter(new unpunishTabber(this));

        getCommand("playerbans").setExecutor(new mainExecutor(this));
        getCommand("playerbans").setTabCompleter(new mainTabber(this));

        getCommand("getip").setExecutor(new getIPExecutor(this));
        getCommand("getip").setTabCompleter(new punishTabber(this));

        getCommand("getuuid").setExecutor(new getUUIDExecutor(this));
        getCommand("getuuid").setTabCompleter(new punishTabber(this));

        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(new PlayerChatListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                        playerNicks = Arrays.stream(Bukkit.getOfflinePlayers())
                                .map(OfflinePlayer::getName)
                                .filter(Objects::nonNull)
                                .toList(),
                0L, 20L * 60);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        this.langFileHandler = new LangFileHandler(this);
        langFileHandler.setupLangFiles();
        this.database = new Database(this);
        database.connect();
        database.createTables();
        this.playerActions = new PlayerActions(database, this);
        this.screenUtil = new ScreenUtil(this);

    }

    public FileConfiguration getLang() {
        return langFileHandler.getLang();
    }

    public void forceStop() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerBans");
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            //this.getLang().
        }
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public List<String> getPlayerNicks() {
        return playerNicks;
    }

    public Database db() {
        return database;
    }

    public PlayerActions playerActions() {
        return playerActions;
    }

    public ScreenUtil getScreenUtil() {
        return screenUtil;
    }

    public String getMessage(String messageID) {
        return getLang().getString(messageID, "ERROR");
    }

    public Component getColoredMessage(String messageID) {
        return TextUtility.color(getMessage(messageID));
    }

    public Component getColoredMessage(String messageID, Map<String, String> placeholder) {
        String text = getMessage(messageID);

        for (Map.Entry<String, String> entry : placeholder.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        return TextUtility.color(text);
    }

    public void printConsoleColoredMessage(String messageID, Map<String, String> placeholder, Level level) {
        String text = getMessage(messageID);

        for (Map.Entry<String, String> entry : placeholder.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        printLogs(text, level);
    }

    public void printConsoleColoredMessage(String messageID, Level level) {
        String text = getMessage(messageID);
        printLogs(text, level);
    }

    public void printLogs(String text, Level level) {
        this.getLogger().log(level, text);
    }
}
