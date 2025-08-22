package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ToolProtectionListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;

    public ToolProtectionListener(AmethystTools plugin) {
        this.plugin = plugin;
        this.toolManager = plugin.getToolManager();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update any AmethystTools in the player's inventory with current owner
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && toolManager.isAmethystTool(item)) {
                toolManager.assignToolToPlayer(item, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && toolManager.isAmethystTool(item)) {
            // Update lore with current timer when player holds the tool
            updateToolLore(item);
            toolManager.assignToolToPlayer(item, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && toolManager.isAmethystTool(item)) {
            // Update tool ownership when moved in inventory
            toolManager.assignToolToPlayer(item, player);
            updateToolLore(item);
        }

        // Also check cursor item
        ItemStack cursor = event.getCursor();
        if (cursor != null && toolManager.isAmethystTool(cursor)) {
            toolManager.assignToolToPlayer(cursor, player);
            updateToolLore(cursor);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (toolManager.isAmethystTool(item)) {
            // Update lore before dropping
            updateToolLore(item);
            event.getItemDrop().setItemStack(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();

        if (toolManager.isAmethystTool(item)) {
            // Prevent AmethystTools from despawning due to item despawn
            // They should only be destroyed by the timer system
            event.setCancelled(true);

            if (plugin.getConfigManager().isDebugEnabled()) {
                UUID toolUUID = toolManager.getToolUUID(item);
                plugin.getLogger().info("Prevented AmethystTool from despawning: " + toolUUID);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();

        if (toolManager.isAmethystTool(item)) {
            // Update lore when picked up by hoppers/droppers
            updateToolLore(item);
            event.getItem().setItemStack(item);
        }
    }

    private void updateToolLore(ItemStack item) {
        if (!toolManager.isAmethystTool(item)) return;

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            List<String> currentLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (currentLore.isEmpty()) return;

            UUID toolUUID = toolManager.getToolUUID(item);
            if (toolUUID == null) return;

            long remainingTime = toolManager.getRemainingTime(item);
            String timeString = MessageUtil.formatTime(remainingTime);

            // Update lore lines
            List<String> newLore = new ArrayList<>();
            boolean hasUuidPermission = false; // We'll assume false for dropped items

            for (String line : currentLore) {
                String plainLine = MessageUtil.stripColors(line);

                if (plainLine.contains("Self Destruct:")) {
                    // Update timer line
                    String updatedLine = line.replaceAll("Self Destruct: .*", "Self Destruct: " + timeString);
                    newLore.add(updatedLine);
                } else if (plainLine.contains("UUID:")) {
                    // Update UUID line - only show if player has permission
                    if (hasUuidPermission) {
                        newLore.add(line);
                    } else {
                        // Hide UUID from players without permission
                        String hiddenUuidLine = line.replaceAll("UUID: .*", "UUID: ████████");
                        newLore.add(hiddenUuidLine);
                    }
                } else {
                    newLore.add(line);
                }
            }

            meta.setLore(newLore);
            item.setItemMeta(meta);

        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error updating tool lore: " + e.getMessage());
            }
        }
    }

    /**
     * Updates tool lore with permission check for UUID visibility
     */
    public void updateToolLoreForPlayer(ItemStack item, Player player) {
        if (!toolManager.isAmethystTool(item)) return;

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            List<String> currentLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (currentLore.isEmpty()) return;

            UUID toolUUID = toolManager.getToolUUID(item);
            if (toolUUID == null) return;

            long remainingTime = toolManager.getRemainingTime(item);
            String timeString = MessageUtil.formatTime(remainingTime);
            boolean hasUuidPermission = player != null &&
                    (player.hasPermission("atools.uuid") || player.hasPermission("atools.op"));

            // Update lore lines
            List<String> newLore = new ArrayList<>();

            for (String line : currentLore) {
                String plainLine = MessageUtil.stripColors(line);

                if (plainLine.contains("Self Destruct:")) {
                    // Update timer line
                    String updatedLine = line.replaceAll("Self Destruct: .*", "Self Destruct: " + timeString);
                    newLore.add(updatedLine);
                } else if (plainLine.contains("UUID:")) {
                    // Update UUID line - only show if player has permission
                    if (hasUuidPermission) {
                        String updatedLine = line.replaceAll("UUID: .*", "UUID: " + toolUUID.toString());
                        newLore.add(updatedLine);
                    } else {
                        // Hide UUID from players without permission
                        String hiddenUuidLine = line.replaceAll("UUID: .*", "UUID: ████████");
                        newLore.add(hiddenUuidLine);
                    }
                } else {
                    newLore.add(line);
                }
            }

            meta.setLore(newLore);
            item.setItemMeta(meta);

        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error updating tool lore for player: " + e.getMessage());
            }
        }
    }
}