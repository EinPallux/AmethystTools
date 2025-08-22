package dev.pallux.amethysttools;

import dev.pallux.amethysttools.commands.AToolsCommand;
import dev.pallux.amethysttools.listeners.*;
import dev.pallux.amethysttools.managers.*;
import dev.pallux.amethysttools.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public final class AmethystTools extends JavaPlugin {

    private static AmethystTools instance;
    private Economy economy;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ToolManager toolManager;
    private CooldownManager cooldownManager;
    private EconomyIntegrationManager economyIntegrationManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        toolManager = new ToolManager(this);
        cooldownManager = new CooldownManager();
        economyIntegrationManager = new EconomyIntegrationManager(this);

        // Load configurations
        configManager.loadConfigs();

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().log(Level.WARNING, "Vault not found! Economy features disabled.");
        }

        // Register commands
        getCommand("atools").setExecutor(new AToolsCommand(this));

        // Register listeners
        registerListeners();

        // Start timer task
        startTimerTask();

        getLogger().info("AmethystTools has been enabled!");
        getLogger().info("Author: Pallux");
        getLogger().info("Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (toolManager != null) {
            toolManager.saveAllTools();
        }
        getLogger().info("AmethystTools has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TreeChopperListener(this), this);
        getServer().getPluginManager().registerEvents(new SellAxeListener(this), this);
        getServer().getPluginManager().registerEvents(new PickaxeListener(this), this);
        getServer().getPluginManager().registerEvents(new BucketListener(this), this);
        getServer().getPluginManager().registerEvents(new TorchListener(this), this);
        getServer().getPluginManager().registerEvents(new RocketListener(this), this);
        getServer().getPluginManager().registerEvents(new ToolProtectionListener(this), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startTimerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                toolManager.updateAllTimers();
            }
        }.runTaskTimer(this, 20L, 20L); // Run every second
    }

    public void reload() {
        configManager.loadConfigs();
        getLogger().info("AmethystTools has been reloaded!");
    }

    // Getters
    public static AmethystTools getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public EconomyIntegrationManager getEconomyIntegrationManager() {
        return economyIntegrationManager;
    }
}