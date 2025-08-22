package dev.pallux.amethysttools.managers;

import dev.pallux.amethysttools.AmethystTools;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class EconomyIntegrationManager {

    private final AmethystTools plugin;
    private final ConfigManager configManager;
    private IntegrationType integrationType;
    private final Map<Material, Double> materialValues;

    public EconomyIntegrationManager(AmethystTools plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.materialValues = new HashMap<>();

        detectIntegrationType();
        loadDefaultValues();
    }

    private void detectIntegrationType() {
        String configuredIntegration = configManager.getEconomyIntegration();

        Plugin economyShopGUI = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
        Plugin shopGUIPlus = Bukkit.getPluginManager().getPlugin("ShopGUIPlus");
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");

        switch (configuredIntegration.toLowerCase()) {
            case "economyshopgui" -> {
                if (economyShopGUI != null && economyShopGUI.isEnabled()) {
                    integrationType = IntegrationType.ECONOMY_SHOP_GUI;
                    plugin.getLogger().info("Using EconomyShopGUI integration");
                } else {
                    fallbackToVault();
                }
            }
            case "shopguiplus", "shopgui+" -> {
                if (shopGUIPlus != null && shopGUIPlus.isEnabled()) {
                    integrationType = IntegrationType.SHOP_GUI_PLUS;
                    plugin.getLogger().info("Using ShopGUIPlus integration");
                } else {
                    fallbackToVault();
                }
            }
            case "vault" -> {
                if (vault != null && vault.isEnabled() && plugin.getEconomy() != null) {
                    integrationType = IntegrationType.VAULT;
                    plugin.getLogger().info("Using Vault integration");
                } else {
                    integrationType = IntegrationType.NONE;
                    plugin.getLogger().warning("Vault not available, economy features disabled");
                }
            }
            default -> fallbackToVault();
        }
    }

    private void fallbackToVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null && plugin.getEconomy() != null) {
            integrationType = IntegrationType.VAULT;
            plugin.getLogger().info("Falling back to Vault integration");
        } else {
            integrationType = IntegrationType.NONE;
            plugin.getLogger().warning("No economy integration available, economy features disabled");
        }
    }

    private void loadDefaultValues() {
        // Load default material values (can be made configurable later)
        materialValues.put(Material.DIAMOND, 100.0);
        materialValues.put(Material.EMERALD, 50.0);
        materialValues.put(Material.GOLD_INGOT, 25.0);
        materialValues.put(Material.IRON_INGOT, 10.0);
        materialValues.put(Material.COPPER_INGOT, 5.0);
        materialValues.put(Material.COAL, 2.0);
        materialValues.put(Material.REDSTONE, 3.0);
        materialValues.put(Material.LAPIS_LAZULI, 8.0);
        materialValues.put(Material.QUARTZ, 15.0);
        materialValues.put(Material.NETHERITE_INGOT, 500.0);

        // Logs
        materialValues.put(Material.OAK_LOG, 1.0);
        materialValues.put(Material.BIRCH_LOG, 1.0);
        materialValues.put(Material.SPRUCE_LOG, 1.0);
        materialValues.put(Material.JUNGLE_LOG, 1.0);
        materialValues.put(Material.ACACIA_LOG, 1.0);
        materialValues.put(Material.DARK_OAK_LOG, 1.0);
        materialValues.put(Material.MANGROVE_LOG, 1.0);
        materialValues.put(Material.CHERRY_LOG, 1.0);

        // Food
        materialValues.put(Material.WHEAT, 0.5);
        materialValues.put(Material.CARROT, 0.5);
        materialValues.put(Material.POTATO, 0.5);
        materialValues.put(Material.BEETROOT, 0.5);
        materialValues.put(Material.APPLE, 1.0);
        materialValues.put(Material.BREAD, 2.0);
        materialValues.put(Material.COOKED_BEEF, 3.0);
        materialValues.put(Material.COOKED_PORKCHOP, 3.0);
        materialValues.put(Material.COOKED_CHICKEN, 2.5);

        // Blocks
        materialValues.put(Material.COBBLESTONE, 0.1);
        materialValues.put(Material.STONE, 0.2);
        materialValues.put(Material.DIRT, 0.05);
        materialValues.put(Material.SAND, 0.1);
        materialValues.put(Material.GRAVEL, 0.1);
    }

    public double calculateChestValue(ItemStack[] contents) {
        double totalValue = 0.0;

        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                double itemValue = getItemValue(item);
                totalValue += itemValue * item.getAmount();
            }
        }

        return totalValue;
    }

    public double getItemValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0.0;
        }

        switch (integrationType) {
            case ECONOMY_SHOP_GUI -> {
                return getEconomyShopGUIValue(item);
            }
            case SHOP_GUI_PLUS -> {
                return getShopGUIPlusValue(item);
            }
            case VAULT -> {
                return getVaultValue(item);
            }
            default -> {
                return getDefaultValue(item);
            }
        }
    }

    private double getEconomyShopGUIValue(ItemStack item) {
        try {
            // Try to hook into EconomyShopGUI API if available
            // For now, fall back to default values
            return getDefaultValue(item);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting EconomyShopGUI value for " + item.getType(), e);
            return getDefaultValue(item);
        }
    }

    private double getShopGUIPlusValue(ItemStack item) {
        try {
            // Try to hook into ShopGUIPlus API if available
            // For now, fall back to default values
            return getDefaultValue(item);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting ShopGUIPlus value for " + item.getType(), e);
            return getDefaultValue(item);
        }
    }

    private double getVaultValue(ItemStack item) {
        // Vault doesn't provide item values, so use default values
        return getDefaultValue(item);
    }

    private double getDefaultValue(ItemStack item) {
        return materialValues.getOrDefault(item.getType(), 0.0);
    }

    public boolean sellItems(Player player, ItemStack[] items, double totalValue) {
        if (integrationType == IntegrationType.NONE || totalValue <= 0) {
            return false;
        }

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return false;
        }

        try {
            economy.depositPlayer(player, totalValue);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error depositing money to player " + player.getName(), e);
            return false;
        }
    }

    public boolean isEconomyAvailable() {
        return integrationType != IntegrationType.NONE;
    }

    public IntegrationType getIntegrationType() {
        return integrationType;
    }

    public void reload() {
        detectIntegrationType();
    }

    public enum IntegrationType {
        ECONOMY_SHOP_GUI,
        SHOP_GUI_PLUS,
        VAULT,
        NONE
    }
}