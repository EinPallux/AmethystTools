package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.ConfigManager;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BucketListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;

    public BucketListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Check if it's an Amethyst Bucket
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.BUCKET) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Only work on right-click
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return;
        }

        // Check if clicking on water
        if (clickedBlock.getType() != Material.WATER) {
            return;
        }

        // Cancel the original event to prevent normal bucket behavior
        event.setCancelled(true);

        // Find water blocks to drain
        Set<Block> waterBlocks = findWaterBlocks(clickedBlock);
        int drainAmount = configManager.getBucketDrainAmount();

        if (waterBlocks.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.bucket.no-water", placeholders);
            return;
        }

        // Limit the amount of water to drain
        List<Block> blocksToRemove = new ArrayList<>(waterBlocks);
        if (blocksToRemove.size() > drainAmount) {
            // Sort by distance to clicked block to drain nearest water first
            Location clickedLocation = clickedBlock.getLocation();
            blocksToRemove.sort(Comparator.comparingDouble(block ->
                    block.getLocation().distanceSquared(clickedLocation)));
            blocksToRemove = blocksToRemove.subList(0, drainAmount);
        }

        // Remove water blocks
        int removedCount = 0;
        for (Block waterBlock : blocksToRemove) {
            if (waterBlock.getType() == Material.WATER) {
                waterBlock.setType(Material.AIR);
                removedCount++;
            }
        }

        // Send success message
        if (removedCount > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(removedCount));
            messageManager.sendMessage(player, "tools.bucket.success", placeholders);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info(String.format(
                        "Player %s drained %d water blocks with Amethyst Bucket",
                        player.getName(), removedCount
                ));
            }
        } else {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.bucket.no-water", placeholders);
        }
    }

    private Set<Block> findWaterBlocks(Block startBlock) {
        Set<Block> waterBlocks = new HashSet<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        if (startBlock.getType() == Material.WATER) {
            queue.add(startBlock);
            visited.add(startBlock);
        }

        int maxBlocks = configManager.getBucketDrainAmount() * 3; // Search more than needed

        while (!queue.isEmpty() && waterBlocks.size() < maxBlocks) {
            Block current = queue.poll();

            if (current.getType() == Material.WATER) {
                waterBlocks.add(current);

                // Check all 6 adjacent blocks (not diagonal)
                Block[] adjacent = {
                        current.getRelative(1, 0, 0),
                        current.getRelative(-1, 0, 0),
                        current.getRelative(0, 1, 0),
                        current.getRelative(0, -1, 0),
                        current.getRelative(0, 0, 1),
                        current.getRelative(0, 0, -1)
                };

                for (Block neighbor : adjacent) {
                    if (!visited.contains(neighbor) && neighbor.getType() == Material.WATER) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }

                // Also check some diagonal water blocks for better draining
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue; // Skip center

                        Block diagonal = current.getRelative(x, 0, z);
                        if (!visited.contains(diagonal) && diagonal.getType() == Material.WATER) {
                            visited.add(diagonal);
                            queue.add(diagonal);
                        }
                    }
                }
            }
        }

        return waterBlocks;
    }
}