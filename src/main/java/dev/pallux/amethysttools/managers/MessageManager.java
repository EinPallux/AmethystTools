package dev.pallux.amethysttools.managers;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final AmethystTools plugin;
    private final ConfigManager configManager;

    public MessageManager(AmethystTools plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(message));
        }
    }

    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = configManager.getMessages().getString(path);
        if (message == null) {
            plugin.getLogger().warning("Missing message: " + path);
            return "&cMissing message: " + path;
        }

        // Replace prefix placeholder
        String prefix = configManager.getMessages().getString("prefix", "&#9d4edd&[&#c77dff&AmethystTools&#9d4edd&]");
        message = message.replace("{prefix}", prefix);

        // Replace custom placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    // Convenience methods for common messages
    public void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "general.no-permission");
    }

    public void sendPlayerNotFound(CommandSender sender) {
        sendMessage(sender, "general.player-not-found");
    }

    public void sendInvalidItem(CommandSender sender) {
        sendMessage(sender, "general.invalid-item");
    }

    public void sendReloadSuccess(CommandSender sender) {
        sendMessage(sender, "general.reload-success");
    }

    public void sendPluginInfo(CommandSender sender) {
        sendMessage(sender, "general.plugin-info");
    }

    public void sendHelp(CommandSender sender) {
        sendMessage(sender, "commands.help.header");
        sendMessage(sender, "commands.help.give");
        sendMessage(sender, "commands.help.reload");
        sendMessage(sender, "commands.help.destroy");
        sendMessage(sender, "commands.help.help");
        sendMessage(sender, "commands.help.footer");
    }

    public void sendGiveSuccess(CommandSender sender, String item, String player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item);
        placeholders.put("player", player);
        sendMessage(sender, "commands.give.success", placeholders);
    }

    public void sendInventoryFull(CommandSender sender, String player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player);
        sendMessage(sender, "commands.give.inventory-full", placeholders);
    }

    public void sendItemReceived(Player player, String item) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item);
        sendMessage(player, "commands.give.received", placeholders);
    }

    public void sendDestroySuccess(CommandSender sender, String uuid) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("uuid", uuid);
        sendMessage(sender, "commands.destroy.success", placeholders);
    }

    public void sendDestroyNotFound(CommandSender sender, String uuid) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("uuid", uuid);
        sendMessage(sender, "commands.destroy.not-found", placeholders);
    }

    public void sendInvalidUuid(CommandSender sender) {
        sendMessage(sender, "commands.destroy.invalid-uuid");
    }

    public void sendCooldownMessage(Player player, String toolType, long remainingTime) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(remainingTime));
        sendMessage(player, "tools." + toolType + ".cooldown", placeholders);
    }

    public void sendTimerWarning(Player player, String toolName, String timeLeft) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tool", toolName);
        placeholders.put("time", timeLeft);

        if (timeLeft.contains("1h")) {
            sendMessage(player, "timer.warning-1h", placeholders);
        } else if (timeLeft.contains("10m")) {
            sendMessage(player, "timer.warning-10m", placeholders);
        } else if (timeLeft.contains("1m")) {
            sendMessage(player, "timer.warning-1m", placeholders);
        }
    }

    public void sendToolDestroyed(Player player, String toolName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tool", toolName);
        sendMessage(player, "timer.destroyed", placeholders);
    }

    public String getItemName(String toolType) {
        return getMessage("item-names." + toolType);
    }
}