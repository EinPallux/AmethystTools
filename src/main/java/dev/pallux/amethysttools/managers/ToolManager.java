package dev.pallux.amethysttools.managers;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.models.AmethystTool;
import dev.pallux.amethysttools.models.ToolType;
import dev.pallux.amethysttools.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ToolManager {

    private final AmethystTools plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final Map<UUID, AmethystTool> activeTools;
    private final NamespacedKey toolKey;
    private final NamespacedKey createdKey;
    private final NamespacedKey uuidKey;

    public ToolManager(AmethystTools plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.activeTools = new ConcurrentHashMap<>();
        this.toolKey = new NamespacedKey(plugin, "amethyst_tool");
        this.createdKey = new NamespacedKey(plugin, "created_time");
        this.uuidKey = new NamespacedKey(plugin, "tool_uuid");

        loadAllTools();
    }

    public ItemStack createTool(ToolType toolType) {
        ItemStack item = createBaseItem(toolType);
        UUID toolUUID = UUID.randomUUID();
        long currentTime = System.currentTimeMillis();

        // Set persistent data
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, toolType.name());
            meta.getPersistentDataContainer().set(createdKey, PersistentDataType.LONG, currentTime);
            meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, toolUUID.toString());

            // Set display name
            String name = configManager.getToolName(toolType.getConfigName());
            meta.displayName(MessageUtil.colorizeComponent(name));

            // Set lore
            List<String> lore = configManager.getToolLore(toolType.getConfigName());
            List<String> finalLore = new ArrayList<>();

            for (String line : lore) {
                if (line.contains("{time}")) {
                    long lifetime = configManager.getToolLifetime() * 24 * 60 * 60 * 1000L; // Convert days to milliseconds
                    long timeLeft = (currentTime + lifetime - System.currentTimeMillis()) / 1000;
                    line = line.replace("{time}", MessageUtil.formatTime(timeLeft));
                }
                if (line.contains("{uuid}")) {
                    line = line.replace("{uuid}", toolUUID.toString());
                }
                if (line.contains("{amount}") && toolType == ToolType.BUCKET) {
                    line = line.replace("{amount}", String.valueOf(configManager.getBucketDrainAmount()));
                }
                finalLore.add(line);
            }

            meta.lore(finalLore.stream().map(MessageUtil::colorizeComponent).toList());

            // Set enchantments
            addEnchantments(meta, toolType);

            // Make unbreakable
            meta.setUnbreakable(true);

            // Hide attributes
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

            item.setItemMeta(meta);
        }

        // Create AmethystTool object
        AmethystTool amethystTool = new AmethystTool(toolUUID, toolType, currentTime, null);
        activeTools.put(toolUUID, amethystTool);

        return item;
    }

    private ItemStack createBaseItem(ToolType toolType) {
        return new ItemStack(switch (toolType) {
            case TREE_CHOPPER, SELL_AXE -> Material.NETHERITE_AXE;
            case PICKAXE -> Material.NETHERITE_PICKAXE;
            case BUCKET -> Material.BUCKET;
            case TORCH -> Material.TORCH;
            case ROCKET -> Material.FIREWORK_ROCKET;
        });
    }

    private void addEnchantments(ItemMeta meta, ToolType toolType) {
        switch (toolType) {
            case TREE_CHOPPER, SELL_AXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            case PICKAXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.addEnchant(Enchantment.FORTUNE, 3, true);
            }
            case BUCKET, TORCH, ROCKET -> {
                // Add a fake enchantment effect for visual appeal
                meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
            }
        }
    }

    public boolean isAmethystTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
    }

    public ToolType getToolType(ItemStack item) {
        if (!isAmethystTool(item)) return null;

        ItemMeta meta = item.getItemMeta();
        String typeString = meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);

        try {
            return ToolType.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public UUID getToolUUID(ItemStack item) {
        if (!isAmethystTool(item)) return null;

        ItemMeta meta = item.getItemMeta();
        String uuidString = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);

        try {
            return UUID.fromString(uuidString);
        } catch (Exception e) {
            return null;
        }
    }

    public long getCreationTime(ItemStack item) {
        if (!isAmethystTool(item)) return 0;

        ItemMeta meta = item.getItemMeta();
        Long creationTime = meta.getPersistentDataContainer().get(createdKey, PersistentDataType.LONG);
        return creationTime != null ? creationTime : 0;
    }

    public long getRemainingTime(ItemStack item) {
        long creationTime = getCreationTime(item);
        if (creationTime == 0) return 0;

        long lifetime = configManager.getToolLifetime() * 24 * 60 * 60 * 1000L; // Convert days to milliseconds
        long expirationTime = creationTime + lifetime;
        long currentTime = System.currentTimeMillis();

        return Math.max(0, (expirationTime - currentTime) / 1000); // Return seconds
    }

    public void updateAllTimers() {
        Set<UUID> toRemove = new HashSet<>();

        // Update all active tools
        for (Map.Entry<UUID, AmethystTool> entry : activeTools.entrySet()) {
            UUID toolUUID = entry.getKey();
            AmethystTool tool = entry.getValue();

            long remainingTime = getRemainingTimeByUUID(toolUUID);

            if (remainingTime <= 0) {
                // Tool expired, remove it
                destroyTool(toolUUID);
                toRemove.add(toolUUID);
            } else {
                // Send warnings
                sendTimerWarnings(tool, remainingTime);
            }
        }

        // Remove expired tools
        toRemove.forEach(activeTools::remove);
    }

    private long getRemainingTimeByUUID(UUID toolUUID) {
        AmethystTool tool = activeTools.get(toolUUID);
        if (tool == null) return 0;

        long lifetime = configManager.getToolLifetime() * 24 * 60 * 60 * 1000L;
        long expirationTime = tool.getCreationTime() + lifetime;
        long currentTime = System.currentTimeMillis();

        return Math.max(0, (expirationTime - currentTime) / 1000);
    }

    private void sendTimerWarnings(AmethystTool tool, long remainingTime) {
        UUID playerUUID = tool.getOwnerUUID();
        if (playerUUID == null) return;

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        String toolName = messageManager.getItemName(tool.getToolType().getConfigName());
        String timeLeft = MessageUtil.formatTime(remainingTime);

        // Send warnings at specific intervals
        if (remainingTime == 3600) { // 1 hour
            messageManager.sendTimerWarning(player, toolName, "1h");
        } else if (remainingTime == 600) { // 10 minutes
            messageManager.sendTimerWarning(player, toolName, "10m");
        } else if (remainingTime == 60) { // 1 minute
            messageManager.sendTimerWarning(player, toolName, "1m");
        }
    }

    public void destroyTool(UUID toolUUID) {
        AmethystTool tool = activeTools.get(toolUUID);
        if (tool == null) return;

        UUID playerUUID = tool.getOwnerUUID();
        if (playerUUID != null) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                // Remove tool from inventory
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && isAmethystTool(item)) {
                        UUID itemUUID = getToolUUID(item);
                        if (toolUUID.equals(itemUUID)) {
                            player.getInventory().remove(item);
                            String toolName = messageManager.getItemName(tool.getToolType().getConfigName());
                            messageManager.sendToolDestroyed(player, toolName);
                            break;
                        }
                    }
                }
            }
        }

        activeTools.remove(toolUUID);
    }

    public void assignToolToPlayer(ItemStack item, Player player) {
        UUID toolUUID = getToolUUID(item);
        if (toolUUID != null) {
            AmethystTool tool = activeTools.get(toolUUID);
            if (tool != null) {
                tool.setOwnerUUID(player.getUniqueId());
            }
        }
    }

    public void loadAllTools() {
        // This method can be expanded to load tools from a data file if needed
        // For now, tools are loaded from memory during runtime
    }

    public void saveAllTools() {
        // This method can be expanded to save tools to a data file if needed
        // For now, tools are managed in memory during runtime
    }

    public Collection<AmethystTool> getActiveTools() {
        return activeTools.values();
    }

    public AmethystTool getToolByUUID(UUID uuid) {
        return activeTools.get(uuid);
    }
}