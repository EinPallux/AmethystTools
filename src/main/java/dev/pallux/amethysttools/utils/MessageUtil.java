package dev.pallux.amethysttools.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})&");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Colorizes text with support for both legacy and hex colors
     * Format: &#RRGGBB& for hex colors, &c for legacy colors
     */
    public static String colorize(String text) {
        if (text == null) return "";

        // Convert hex colors to MiniMessage format
        text = convertHexColors(text);

        // Convert legacy colors
        text = ChatColor.translateAlternateColorCodes('&', text);

        return text;
    }

    /**
     * Creates a Component with hex color support
     */
    public static Component colorizeComponent(String text) {
        if (text == null) return Component.empty();

        // Convert hex colors to MiniMessage format
        text = convertHexColors(text);

        // Convert legacy colors to MiniMessage format
        text = convertLegacyColors(text);

        try {
            return MINI_MESSAGE.deserialize(text).decoration(TextDecoration.ITALIC, false);
        } catch (Exception e) {
            // Fallback to legacy if MiniMessage fails
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }

    private static String convertHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hexCode + ">");
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertLegacyColors(String text) {
        return text
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    /**
     * Strips all color codes from text
     */
    public static String stripColors(String text) {
        if (text == null) return "";

        // Remove hex colors
        text = HEX_PATTERN.matcher(text).replaceAll("");

        // Remove legacy colors
        text = ChatColor.stripColor(text);

        return text;
    }

    /**
     * Formats time in a readable format
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append("d");
            if (hours > 0 || minutes > 0) result.append(", ");
        }

        if (hours > 0) {
            result.append(hours).append("h");
            if (minutes > 0) result.append(", ");
        }

        if (minutes > 0) {
            result.append(minutes).append("m");
            if (secs > 0 && days == 0) result.append(", ");
        }

        if (secs > 0 && days == 0) {
            result.append(secs).append("s");
        }

        return result.toString();
    }

    /**
     * Creates a progress bar
     */
    public static String createProgressBar(double percentage, int length, String completeColor, String incompleteColor) {
        int completed = (int) (length * percentage);
        int remaining = length - completed;

        StringBuilder bar = new StringBuilder();
        bar.append(completeColor);

        for (int i = 0; i < completed; i++) {
            bar.append("█");
        }

        bar.append(incompleteColor);

        for (int i = 0; i < remaining; i++) {
            bar.append("█");
        }

        return bar.toString();
    }
}