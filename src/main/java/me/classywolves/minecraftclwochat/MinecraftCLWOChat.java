package me.classywolves.minecraftclwochat;

import me.classywolves.minecraftclwochat.chat.GlobalChatToIngameRelay;
import me.classywolves.minecraftclwochat.events.PlayerSendGlobalMessageEvent;
import me.classywolves.minecraftclwochat.runnables.initGlobalChatReaderRunnable;
import me.classywolves.minecraftclwochat.util.SettingsConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public final class MinecraftCLWOChat extends JavaPlugin {

    private GlobalChatToIngameRelay globalRelay;
    private FileConfiguration settingsConfig;
    private File settingsFile;

    private void initGlobalChatToIngameRelay() {
        globalRelay = new GlobalChatToIngameRelay(settingsConfig);
    }

    public GlobalChatToIngameRelay getGlobalChatToIngameRelay() {
        return globalRelay;
    }

    private void initConfig() {

        settingsFile = new File(getDataFolder(), "settings.yml");
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);

        if (!(settingsFile.exists())) {

            HashMap<String, Object> defaults = new HashMap<>();
            defaults.put(SettingsConfig.GLOBALCHAT_SEND_MESSAGE, "&7&o[GlobalChat] Submitted message, awaiting broadcasting...");

            settingsConfig.addDefaults(defaults);
            settingsConfig.options().copyDefaults(true);
        }
        try {
            settingsConfig.save(settingsFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
     }

     @Override
     public FileConfiguration getConfig() {
        return settingsConfig;
     }

     public void saveConfig() {
        try {
            settingsConfig.save(settingsFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
     }

    @Override
    public void onEnable() {
        initConfig();
        initGlobalChatToIngameRelay();
        new initGlobalChatReaderRunnable(this, -1, globalRelay).runTaskAsynchronously(this);
        Bukkit.getPluginManager().registerEvents(new PlayerSendGlobalMessageEvent(this), this);
    }

    @Override
    public void onDisable() {

    }
}
