package com.jakisniekot.playerBans;

import com.jakisniekot.playerBans.lang.LangFileHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class PlayerBans extends JavaPlugin {

    private PlayerBans playerBans;

    private LangFileHandler langFileHandler;





    private ErrorType errorType = ErrorType.NONE;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //Plugin
        playerBans = this;

        //Database

        //Lang
        this.langFileHandler = new LangFileHandler(playerBans);

        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reload() {
        saveConfig();

    }

    public String rewriteString(String text, Map<String, String> placeholder) {
        for (Map.Entry<String, String> entry : placeholder.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        return text;
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
}
