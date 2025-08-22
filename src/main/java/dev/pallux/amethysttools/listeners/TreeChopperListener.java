package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TreeChopperListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;

    private static final Set<Material> LOG_TYPES = Set.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.WARPED_STEM, Material.CRIMSON_STEM,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
            Material.STRIPPED_WARPED_STEM, Material.STRIPPED_CRIMSON_STEM
    );

    private static final Set<Material> LEAF_TYPES = Set.of(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES,
            Material.WARPED_WART_BLOCK, Material.NETHER_WART_BLOCK
    );

    public TreeChopperListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if it's an Amethyst Tree Chopper
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.TREE_CHOPPER) {
            return;
        }

        Block block = event.getBlock();

        // Check if the broken block is a log
        if (!LOG_TYPES.contains(block.getType())) {
            return;
        }

        // Find and chop the entire tree
        Set<Block> treeBlocks = findTreeBlocks(block);

        if (treeBlocks.isEmpty()) {
            return;
        }

        // Cancel the original event
        event.setCancelled(true);

        // Chop the entire tree
        chopTree(player, treeBlocks, item);

        // Send success message
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "tools.tree-chopper.success", placeholders);
    }

    private Set<Block> findTreeBlocks(Block startBlock) {
        Set<Block> treeBlocks = new HashSet<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty() && treeBlocks.size() < 1000) { // Limit to prevent lag
            Block current = queue.poll();

            if (LOG_TYPES.contains(current.getType())) {
                treeBlocks.add(current);

                // Check surrounding blocks
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;

                            Block neighbor = current.getRelative(x, y, z);

                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);

                                if (LOG_TYPES.contains(neighbor.getType())) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Also find connected leaves
        findConnectedLeaves(treeBlocks, startBlock.getLocation(), 5); // 5 block radius for leaves

        return treeBlocks;
    }

    private void findConnectedLeaves(Set<Block> treeBlocks, Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();

                    if (LEAF_TYPES.contains(block.getType())) {
                        // Check if this leaf is connected to any log block
                        if (isLeafConnectedToTree(block, treeBlocks, 4)) {
                            treeBlocks.add(block);
                        }
                    }
                }
            }
        }
    }

    private boolean isLeafConnectedToTree(Block leaf, Set<Block> logBlocks, int maxDistance) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(leaf);
        visited.add(leaf);

        int distance = 0;

        while (!queue.isEmpty() && distance <= maxDistance) {
            int queueSize = queue.size();

            for (int i = 0; i < queueSize; i++) {
                Block current = queue.poll();

                // Check if we've reached a log block
                if (logBlocks.contains(current)) {
                    return true;
                }

                // Check surrounding blocks
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;

                            Block neighbor = current.getRelative(x, y, z);

                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);

                                if (LOG_TYPES.contains(neighbor.getType()) ||
                                        LEAF_TYPES.contains(neighbor.getType())) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }

            distance++;
        }

        return false;
    }

    private void chopTree(Player player, Set<Block> treeBlocks, ItemStack tool) {
        int fortuneLevel = 0;
        ItemMeta meta = tool.getItemMeta();
        if (meta != null && meta.hasEnchant(Enchantment.FORTUNE)) {
            fortuneLevel = meta.getEnchantLevel(Enchantment.FORTUNE);
        }

        Location playerLocation = player.getLocation();

        for (Block block : treeBlocks) {
            Material blockType = block.getType();

            // Drop items naturally with fortune effect
            Collection<ItemStack> drops = block.getDrops(tool);

            // Apply fortune to logs (not leaves, as they don't benefit from fortune)
            if (LOG_TYPES.contains(blockType) && fortuneLevel > 0) {
                // Fortune increases drop chances
                Random random = new Random();
                for (ItemStack drop : new ArrayList<>(drops)) {
                    int bonusDrops = random.nextInt(fortuneLevel + 1);
                    if (bonusDrops > 0) {
                        ItemStack bonus = drop.clone();
                        bonus.setAmount(bonusDrops);
                        drops.add(bonus);
                    }
                }
            }

            // Break the block
            block.setType(Material.AIR);

            // Drop items at the original block location or near player if too far
            Location dropLocation = block.getLocation();
            if (dropLocation.distanceSquared(playerLocation) > 100) { // 10 blocks away
                dropLocation = playerLocation;
            }

            for (ItemStack drop : drops) {
                if (drop != null && drop.getType() != Material.AIR) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, drop);
                }
            }
        }
    }
}