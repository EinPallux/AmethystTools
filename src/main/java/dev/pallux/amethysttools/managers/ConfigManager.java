package dev.pallux.amethysttools.managers;

import dev.pallux.amethysttools.AmethystTools;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final AmethystTools plugin;
    private FileConfiguration config;
    private File configFile;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(AmethystTools plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        loadConfig();
        loadMessages();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Check for updates and add missing values
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
            saveConfig();
        }
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Check for updates and add missing values
        InputStream defMessagesStream = plugin.getResource("messages.yml");
        if (defMessagesStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(defMessagesStream));
            messages.setDefaults(defMessages);
            messages.options().copyDefaults(true);
            saveMessages();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml!", e);
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml!", e);
        }
    }

    // Config getters
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public int getToolLifetime() {
        return config.getInt("general.tool-lifetime", 7);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("general.debug", false);
    }

    public String getEconomyIntegration() {
        return config.getString("economy.integration", "EconomyShopGUI");
    }

    public boolean isToolEnabled(String tool) {
        return config.getBoolean("tools." + tool + ".enabled", true);
    }

    public String getToolName(String tool) {
        return config.getString("tools." + tool + ".name", "&7Unknown Tool");
    }

    public List<String> getToolLore(String tool) {
        return config.getStringList("tools." + tool + ".lore");
    }

    public List<String> getBlacklistedBlocks() {
        return config.getStringList("tools.pickaxe.blacklisted-blocks");
    }

    public int getBucketDrainAmount() {
        return config.getInt("tools.bucket.drain-amount", 27);
    }

    public int getTorchCooldown() {
        return config.getInt("tools.torch.cooldown", 5);
    }

    public int getRocketCooldown() {
        return config.getInt("tools.rocket.cooldown", 2);
    }

    public int getSaveInterval() {
        return config.getInt("advanced.save-interval", 300);
    }

    public int getMaxToolsPerPlayer() {
        return config.getInt("advanced.max-tools-per-player", 10);
    }
}