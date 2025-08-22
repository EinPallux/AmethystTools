package dev.pallux.amethysttools.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> playerCooldowns;

    public CooldownManager() {
        this.playerCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Sets a cooldown for a player and action type
     * @param player The player
     * @param actionType The type of action (e.g., "torch", "rocket")
     * @param cooldownSeconds The cooldown duration in seconds
     */
    public void setCooldown(Player player, String actionType, int cooldownSeconds) {
        UUID playerUUID = player.getUniqueId();
        long expirationTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        playerCooldowns.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(actionType, expirationTime);
    }

    /**
     * Checks if a player has a cooldown for a specific action
     * @param player The player
     * @param actionType The type of action
     * @return true if the player is on cooldown, false otherwise
     */
    public boolean hasCooldown(Player player, String actionType) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Long> playerActions = playerCooldowns.get(playerUUID);

        if (playerActions == null) {
            return false;
        }

        Long expirationTime = playerActions.get(actionType);
        if (expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expirationTime) {
            // Cooldown expired, remove it
            playerActions.remove(actionType);
            if (playerActions.isEmpty()) {
                playerCooldowns.remove(playerUUID);
            }
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds
     * @param player The player
     * @param actionType The type of action
     * @return The remaining cooldown time in seconds, or 0 if no cooldown
     */
    public long getRemainingCooldown(Player player, String actionType) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Long> playerActions = playerCooldowns.get(playerUUID);

        if (playerActions == null) {
            return 0;
        }

        Long expirationTime = playerActions.get(actionType);
        if (expirationTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= expirationTime) {
            // Cooldown expired, remove it
            playerActions.remove(actionType);
            if (playerActions.isEmpty()) {
                playerCooldowns.remove(playerUUID);
            }
            return 0;
        }

        return (expirationTime - currentTime) / 1000L;
    }

    /**
     * Removes a specific cooldown for a player
     * @param player The player
     * @param actionType The type of action
     */
    public void removeCooldown(Player player, String actionType) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Long> playerActions = playerCooldowns.get(playerUUID);

        if (playerActions != null) {
            playerActions.remove(actionType);
            if (playerActions.isEmpty()) {
                playerCooldowns.remove(playerUUID);
            }
        }
    }

    /**
     * Removes all cooldowns for a player
     * @param player The player
     */
    public void removeAllCooldowns(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerCooldowns.remove(playerUUID);
    }

    /**
     * Clears all cooldowns (useful for plugin reload)
     */
    public void clearAllCooldowns() {
        playerCooldowns.clear();
    }

    /**
     * Cleans up expired cooldowns (can be called periodically)
     */
    public void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        playerCooldowns.entrySet().removeIf(playerEntry -> {
            Map<String, Long> playerActions = playerEntry.getValue();

            playerActions.entrySet().removeIf(actionEntry ->
                    actionEntry.getValue() <= currentTime
            );

            return playerActions.isEmpty();
        });
    }

    /**
     * Gets all active cooldowns for debugging purposes
     * @return A map of player UUIDs to their action cooldowns
     */
    public Map<UUID, Map<String, Long>> getAllCooldowns() {
        return new HashMap<>(playerCooldowns);
    }

    /**
     * Gets the number of active cooldowns
     * @return The total number of active cooldowns across all players
     */
    public int getActiveCooldownCount() {
        return playerCooldowns.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}