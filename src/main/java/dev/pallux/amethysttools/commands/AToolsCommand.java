package dev.pallux.amethysttools.commands;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AToolsCommand implements CommandExecutor, TabCompleter {

    private final AmethystTools plugin;
    private final MessageManager messageManager;
    private final ToolManager toolManager;

    public AToolsCommand(AmethystTools plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.toolManager = plugin.getToolManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messageManager.sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGiveCommand(sender, args);
            case "reload" -> handleReloadCommand(sender);
            case "destroy" -> handleDestroyCommand(sender, args);
            case "help" -> messageManager.sendHelp(sender);
            default -> messageManager.sendHelp(sender);
        }

        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("atools.give") && !sender.hasPermission("atools.op")) {
            messageManager.sendNoPermission(sender);
            return;
        }

        if (args.length < 3) {
            messageManager.sendMessage(sender, "commands.help.give");
            return;
        }

        String playerName = args[1];
        String itemName = args[2].toLowerCase().replace("_", "-");

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            messageManager.sendPlayerNotFound(sender);
            return;
        }

        ToolType toolType = getToolTypeFromString(itemName);
        if (toolType == null) {
            messageManager.sendInvalidItem(sender);
            return;
        }

        if (!plugin.getConfigManager().isToolEnabled(toolType.getConfigName())) {
            messageManager.sendMessage(sender, "general.invalid-item");
            return;
        }

        // Check if player's inventory has space
        if (target.getInventory().firstEmpty() == -1) {
            messageManager.sendInventoryFull(sender, target.getName());
            return;
        }

        ItemStack tool = toolManager.createTool(toolType);
        target.getInventory().addItem(tool);

        // Assign tool to player
        toolManager.assignToolToPlayer(tool, target);

        String toolDisplayName = messageManager.getItemName(toolType.getConfigName());
        messageManager.sendGiveSuccess(sender, toolDisplayName, target.getName());
        messageManager.sendItemReceived(target, toolDisplayName);
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("atools.reload") && !sender.hasPermission("atools.op")) {
            messageManager.sendNoPermission(sender);
            return;
        }

        plugin.reload();
        messageManager.sendReloadSuccess(sender);
    }

    private void handleDestroyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("atools.destroy") && !sender.hasPermission("atools.op")) {
            messageManager.sendNoPermission(sender);
            return;
        }

        if (args.length < 2) {
            messageManager.sendMessage(sender, "commands.help.destroy");
            return;
        }

        String uuidString = args[1];
        UUID toolUUID;

        try {
            toolUUID = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            messageManager.sendInvalidUuid(sender);
            return;
        }

        if (toolManager.getToolByUUID(toolUUID) == null) {
            messageManager.sendDestroyNotFound(sender, uuidString);
            return;
        }

        toolManager.destroyTool(toolUUID);
        messageManager.sendDestroySuccess(sender, uuidString);
    }

    private ToolType getToolTypeFromString(String input) {
        return switch (input) {
            case "tree-chopper", "treechopper", "axe" -> ToolType.TREE_CHOPPER;
            case "sell-axe", "sellaxe", "sell" -> ToolType.SELL_AXE;
            case "pickaxe", "pick" -> ToolType.PICKAXE;
            case "bucket" -> ToolType.BUCKET;
            case "torch" -> ToolType.TORCH;
            case "rocket" -> ToolType.ROCKET;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("give", "reload", "destroy", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    if (hasPermissionForSubCommand(sender, subCommand)) {
                        completions.add(subCommand);
                    }
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if ("give".equals(subCommand)) {
                // Second argument for give - player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if ("destroy".equals(subCommand)) {
                // Second argument for destroy - UUIDs (we could show active tool UUIDs if needed)
                completions.add("<uuid>");
            }
        } else if (args.length == 3 && "give".equals(args[0].toLowerCase())) {
            // Third argument for give - item types
            List<String> items = Arrays.asList(
                    "tree-chopper", "sell-axe", "pickaxe",
                    "bucket", "torch", "rocket"
            );
            for (String item : items) {
                if (item.toLowerCase().startsWith(args[2].toLowerCase())) {
                    ToolType toolType = getToolTypeFromString(item);
                    if (toolType != null && plugin.getConfigManager().isToolEnabled(toolType.getConfigName())) {
                        completions.add(item);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        return switch (subCommand) {
            case "give" -> sender.hasPermission("atools.give") || sender.hasPermission("atools.op");
            case "reload" -> sender.hasPermission("atools.reload") || sender.hasPermission("atools.op");
            case "destroy" -> sender.hasPermission("atools.destroy") || sender.hasPermission("atools.op");
            case "help" -> true; // Help is available to everyone
            default -> false;
        };
    }
}