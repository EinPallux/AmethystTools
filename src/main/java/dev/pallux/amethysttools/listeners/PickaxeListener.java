package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.ConfigManager;
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

public class PickaxeListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;

    public PickaxeListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if it's an Amethyst Pickaxe
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.PICKAXE) {
            return;
        }

        Block block = event.getBlock();

        // Check if block can be mined with a pickaxe
        if (!canMineWithPickaxe(block.getType())) {
            return;
        }

        // Check if block is blacklisted
        if (isBlockBlacklisted(block.getType())) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.pickaxe.blacklisted", placeholders);
            event.setCancelled(true);
            return;
        }

        // Cancel the original event
        event.setCancelled(true);

        // Get the blocks to mine in 3x3x3 area
        List<Block> blocksToMine = getBlocksToMine(block, player);

        // Mine all blocks
        mineBlocks(player, blocksToMine, item);

        // Send success message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("blocks", String.valueOf(blocksToMine.size()));
        messageManager.sendMessage(player, "tools.pickaxe.success", placeholders);
    }

    private boolean canMineWithPickaxe(Material material) {
        // Check if the material can be mined with a pickaxe
        return switch (material) {
            case STONE, COBBLESTONE, GRANITE, DIORITE, ANDESITE,
                 DEEPSLATE, COBBLED_DEEPSLATE, NETHERRACK, BLACKSTONE,
                 COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE,
                 LAPIS_ORE, REDSTONE_ORE, COPPER_ORE, NETHER_GOLD_ORE,
                 NETHER_QUARTZ_ORE, ANCIENT_DEBRIS,
                 DEEPSLATE_COAL_ORE, DEEPSLATE_IRON_ORE, DEEPSLATE_GOLD_ORE,
                 DEEPSLATE_DIAMOND_ORE, DEEPSLATE_EMERALD_ORE, DEEPSLATE_LAPIS_ORE,
                 DEEPSLATE_REDSTONE_ORE, DEEPSLATE_COPPER_ORE,
                 SANDSTONE, RED_SANDSTONE, PRISMARINE, DARK_PRISMARINE,
                 PRISMARINE_BRICKS, SEA_LANTERN, MAGMA_BLOCK,
                 OBSIDIAN, CRYING_OBSIDIAN, RESPAWN_ANCHOR,
                 STONE_BRICKS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS,
                 CHISELED_STONE_BRICKS, SMOOTH_STONE, POLISHED_GRANITE,
                 POLISHED_DIORITE, POLISHED_ANDESITE, BRICKS, NETHER_BRICKS,
                 RED_NETHER_BRICKS, CHISELED_NETHER_BRICKS, CRACKED_NETHER_BRICKS,
                 BASALT, SMOOTH_BASALT, POLISHED_BASALT,
                 TUFF, CALCITE, AMETHYST_BLOCK, BUDDING_AMETHYST -> true;
            default -> false;
        };
    }

    private boolean isBlockBlacklisted(Material material) {
        List<String> blacklist = configManager.getBlacklistedBlocks();
        return blacklist.contains(material.name());
    }

    private List<Block> getBlocksToMine(Block centerBlock, Player player) {
        List<Block> blocks = new ArrayList<>();
        Location center = centerBlock.getLocation();

        // Get player's facing direction to determine mining orientation
        float yaw = player.getLocation().getYaw();
        boolean miningVertically = Math.abs(player.getLocation().getPitch()) > 45;

        if (miningVertically) {
            // Mining up/down - use XZ plane
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        if (shouldMineBlock(block)) {
                            blocks.add(block);
                        }
                    }
                }
            }
        } else {
            // Mining horizontally - adjust based on facing direction
            int[] xRange, yRange, zRange;

            if ((yaw >= -45 && yaw <= 45) || (yaw >= 135 || yaw <= -135)) {
                // Facing North/South - mine in X and Y directions
                xRange = new int[]{-1, 0, 1};
                yRange = new int[]{-1, 0, 1};
                zRange = new int[]{-1, 0, 1};
            } else {
                // Facing East/West - mine in Z and Y directions
                xRange = new int[]{-1, 0, 1};
                yRange = new int[]{-1, 0, 1};
                zRange = new int[]{-1, 0, 1};
            }

            for (int x : xRange) {
                for (int y : yRange) {
                    for (int z : zRange) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        if (shouldMineBlock(block)) {
                            blocks.add(block);
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private boolean shouldMineBlock(Block block) {
        Material type = block.getType();

        // Don't mine air or liquid
        if (type == Material.AIR || type == Material.WATER || type == Material.LAVA) {
            return false;
        }

        // Don't mine blacklisted blocks
        if (isBlockBlacklisted(type)) {
            return false;
        }

        // Only mine blocks that can be mined with a pickaxe
        return canMineWithPickaxe(type);
    }

    private void mineBlocks(Player player, List<Block> blocks, ItemStack tool) {
        int fortuneLevel = 0;
        int silkTouchLevel = 0;

        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            if (meta.hasEnchant(Enchantment.FORTUNE)) {
                fortuneLevel = meta.getEnchantLevel(Enchantment.FORTUNE);
            }
            if (meta.hasEnchant(Enchantment.SILK_TOUCH)) {
                silkTouchLevel = meta.getEnchantLevel(Enchantment.SILK_TOUCH);
            }
        }

        Location playerLocation = player.getLocation();

        for (Block block : blocks) {
            Material blockType = block.getType();
            Location blockLocation = block.getLocation();

            // Get drops with appropriate enchantments
            Collection<ItemStack> drops;
            if (silkTouchLevel > 0) {
                // Silk touch drops
                drops = block.getDrops(tool);
            } else {
                // Normal drops with fortune
                drops = block.getDrops(tool);

                // Apply fortune multiplier for applicable blocks
                if (fortuneLevel > 0 && isFortuneApplicable(blockType)) {
                    Random random = new Random();
                    Collection<ItemStack> fortuneDrops = new ArrayList<>();

                    for (ItemStack drop : drops) {
                        ItemStack fortuneDrop = drop.clone();
                        int bonusAmount = 0;

                        // Fortune calculation
                        for (int i = 0; i < fortuneLevel; i++) {
                            if (random.nextDouble() < 0.33) { // 33% chance per level
                                bonusAmount++;
                            }
                        }

                        fortuneDrop.setAmount(drop.getAmount() + bonusAmount);
                        fortuneDrops.add(fortuneDrop);
                    }
                    drops = fortuneDrops;
                }
            }

            // Break the block
            block.setType(Material.AIR);

            // Drop items at appropriate location
            Location dropLocation = blockLocation;
            if (dropLocation.distanceSquared(playerLocation) > 64) { // 8 blocks away
                dropLocation = playerLocation;
            }

            for (ItemStack drop : drops) {
                if (drop != null && drop.getType() != Material.AIR) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, drop);
                }
            }
        }
    }

    private boolean isFortuneApplicable(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, NETHER_QUARTZ_ORE,
                 NETHER_GOLD_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
            default -> false;
        };
    }
}