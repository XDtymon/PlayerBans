package com.jakisniekot.playerBans.lang;

import com.jakisniekot.playerBans.PlayerBans;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class LangFileHandler {

    private File langFile;
    private FileConfiguration langConfig;

    private PlayerBans plugin;

    public LangFileHandler(PlayerBans plugin) {
        this.plugin = plugin;
    }

    public void setupLangFiles() {
        String langCode = plugin.getConfig().getString("lang", "en");
        langFile = new File(plugin.getDataFolder() + "/lang", langCode + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            plugin.saveResource("lang/" + langCode + ".yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defStream = plugin.getResource("lang/" + langCode + ".yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
        }
    }

    public FileConfiguration getLang() {
        return langConfig;
    }

}