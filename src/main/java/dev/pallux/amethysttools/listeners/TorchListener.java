package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.ConfigManager;
import dev.pallux.amethysttools.managers.CooldownManager;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class TorchListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    private static final String COOLDOWN_KEY = "torch";

    public TorchListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Check if it's an Amethyst Torch
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.TORCH) {
            return;
        }

        // Only work on right-click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // Check cooldown
        if (cooldownManager.hasCooldown(player, COOLDOWN_KEY)) {
            long remainingTime = cooldownManager.getRemainingCooldown(player, COOLDOWN_KEY);
            messageManager.sendCooldownMessage(player, COOLDOWN_KEY, remainingTime);
            event.setCancelled(true);
            return;
        }

        Block targetBlock = getTargetBlock(player, event);
        if (targetBlock == null) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.torch.invalid-location", placeholders);
            event.setCancelled(true);
            return;
        }

        // Cancel the original event
        event.setCancelled(true);

        // Place torch
        if (placeTorch(targetBlock)) {
            // Set cooldown
            int cooldown = configManager.getTorchCooldown();
            cooldownManager.setCooldown(player, COOLDOWN_KEY, cooldown);

            // Send success message
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.torch.success", placeholders);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info(String.format(
                        "Player %s placed a torch with Amethyst Torch at %s",
                        player.getName(), targetBlock.getLocation()
                ));
            }
        } else {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.torch.invalid-location", placeholders);
        }
    }

    private Block getTargetBlock(Player player, PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock != null) {
            // Player clicked on a block, try to place torch on top or on the clicked face
            BlockFace face = event.getBlockFace();
            Block targetBlock = clickedBlock.getRelative(face);

            if (canPlaceTorchAt(targetBlock, face.getOppositeFace())) {
                return targetBlock;
            }
        } else {
            // Player right-clicked air, try to place at their feet location
            Block playerBlock = player.getLocation().getBlock();
            if (canPlaceTorchAt(playerBlock, BlockFace.UP)) {
                return playerBlock;
            }

            // Try one block in front of player
            Block frontBlock = player.getTargetBlock(null, 5);
            if (frontBlock != null && frontBlock.getType() != Material.AIR) {
                Block aboveBlock = frontBlock.getRelative(BlockFace.UP);
                if (canPlaceTorchAt(aboveBlock, BlockFace.DOWN)) {
                    return aboveBlock;
                }
            }
        }

        return null;
    }

    private boolean canPlaceTorchAt(Block block, BlockFace attachedFace) {
        // Check if the target block is air or replaceable
        if (block.getType() != Material.AIR && !isReplaceable(block.getType())) {
            return false;
        }

        // Check if there's a solid block to attach to
        Block attachedBlock = block.getRelative(attachedFace);

        // Torches can be placed on most solid blocks
        return isSolidBlock(attachedBlock.getType()) && !isBlacklisted(attachedBlock.getType());
    }

    private boolean isReplaceable(Material material) {
        return switch (material) {
            case AIR, WATER, LAVA, TALL_GRASS, SHORT_GRASS, FERN, LARGE_FERN,
                 DEAD_BUSH, VINE, SNOW, FIRE -> true;
            default -> false;
        };
    }

    private boolean isSolidBlock(Material material) {
        return switch (material) {
            case AIR, WATER, LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE -> false;
            default -> material.isSolid();
        };
    }

    private boolean isBlacklisted(Material material) {
        return switch (material) {
            case MAGMA_BLOCK, ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE,
                 SLIME_BLOCK, HONEY_BLOCK, SPAWNER, END_PORTAL, END_GATEWAY,
                 NETHER_PORTAL, BARRIER, STRUCTURE_VOID -> true;
            default -> false;
        };
    }

    private boolean placeTorch(Block block) {
        try {
            // Determine the best torch type based on the location
            Material torchType = getTorchType(block);

            // Set the block to torch
            block.setType(torchType);

            // Try to set proper torch direction if it's a wall torch
            if (torchType == Material.WALL_TORCH || torchType == Material.SOUL_WALL_TORCH) {
                setWallTorchDirection(block);
            }

            return true;
        } catch (Exception e) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().warning("Failed to place torch at " + block.getLocation() + ": " + e.getMessage());
            }
            return false;
        }
    }

    private Material getTorchType(Block block) {
        // Check if we can place a standing torch (on top of a block)
        Block below = block.getRelative(BlockFace.DOWN);
        if (isSolidBlock(below.getType())) {
            return Material.TORCH;
        }

        // Check for wall torch placement
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block adjacent = block.getRelative(face);
            if (isSolidBlock(adjacent.getType())) {
                return Material.WALL_TORCH;
            }
        }

        // Default to regular torch
        return Material.TORCH;
    }

    private void setWallTorchDirection(Block block) {
        // Find the first solid block adjacent to attach the wall torch to
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

        for (BlockFace face : faces) {
            Block adjacent = block.getRelative(face);
            if (isSolidBlock(adjacent.getType())) {
                // The torch should face the opposite direction of the solid block
                try {
                    org.bukkit.block.data.BlockData data = block.getBlockData();
                    if (data instanceof org.bukkit.block.data.type.WallTorch wallTorch) {
                        wallTorch.setFacing(face.getOppositeFace());
                        block.setBlockData(wallTorch);
                    }
                } catch (Exception e) {
                    // Fallback - just place regular torch
                    block.setType(Material.TORCH);
                }
                break;
            }
        }
    }
}