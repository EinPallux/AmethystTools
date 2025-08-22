package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.EconomyIntegrationManager;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class SellAxeListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;
    private final EconomyIntegrationManager economyManager;
    private final DecimalFormat moneyFormat;

    public SellAxeListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
        this.economyManager = plugin.getEconomyIntegrationManager();
        this.moneyFormat = new DecimalFormat("#,##0.00");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if it's an Amethyst Sell Axe
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.SELL_AXE) {
            return;
        }

        Block block = event.getBlock();

        // Check if the broken block is a chest
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        // Check if economy is available
        if (!economyManager.isEconomyAvailable()) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.sell-axe.no-economy", placeholders);
            return;
        }

        // Get chest contents
        Chest chest = (Chest) block.getState();
        Inventory inventory = chest.getInventory();
        ItemStack[] contents = inventory.getContents();

        // Calculate total value
        double totalValue = economyManager.calculateChestValue(contents);

        if (totalValue <= 0) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.sell-axe.no-items", placeholders);
            return;
        }

        // Create a copy of items to sell
        ItemStack[] itemsToSell = new ItemStack[contents.length];
        int itemCount = 0;

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                itemsToSell[i] = contents[i].clone();
                itemCount++;
            }
        }

        // Clear the chest inventory
        inventory.clear();

        // Try to sell items
        boolean success = economyManager.sellItems(player, itemsToSell, totalValue);

        if (success) {
            // Cancel the block break event so the chest doesn't drop
            event.setCancelled(true);

            // Break the chest block manually
            block.setType(Material.AIR);

            // Drop the chest item
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));

            // Send success message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", moneyFormat.format(totalValue));
            messageManager.sendMessage(player, "tools.sell-axe.success", placeholders);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format(
                        "Player %s sold %d items for $%.2f using Sell Axe",
                        player.getName(), itemCount, totalValue
                ));
            }
        } else {
            // Restore items to chest if selling failed
            for (int i = 0; i < itemsToSell.length; i++) {
                if (itemsToSell[i] != null) {
                    inventory.setItem(i, itemsToSell[i]);
                }
            }

            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.sell-axe.no-economy", placeholders);
        }
    }
}