package dev.pallux.amethysttools.listeners;

import dev.pallux.amethysttools.AmethystTools;
import dev.pallux.amethysttools.managers.ConfigManager;
import dev.pallux.amethysttools.managers.CooldownManager;
import dev.pallux.amethysttools.managers.MessageManager;
import dev.pallux.amethysttools.managers.ToolManager;
import dev.pallux.amethysttools.models.ToolType;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RocketListener implements Listener {

    private final AmethystTools plugin;
    private final ToolManager toolManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    private static final String COOLDOWN_KEY = "rocket";

    public RocketListener(AmethystTools plugin) {
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

        // Check if it's an Amethyst Rocket
        if (!toolManager.isAmethystTool(item) ||
                toolManager.getToolType(item) != ToolType.ROCKET) {
            return;
        }

        // Only work on right-click
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return;
        }

        // Check if player is flying with elytra
        if (!player.isGliding()) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(player, "tools.rocket.not-flying", placeholders);
            event.setCancelled(true);
            return;
        }

        // Check cooldown
        if (cooldownManager.hasCooldown(player, COOLDOWN_KEY)) {
            long remainingTime = cooldownManager.getRemainingCooldown(player, COOLDOWN_KEY);
            messageManager.sendCooldownMessage(player, COOLDOWN_KEY, remainingTime);
            event.setCancelled(true);
            return;
        }

        // Cancel the original event to prevent consuming the rocket
        event.setCancelled(true);

        // Launch the player forward
        launchPlayer(player);

        // Set cooldown
        int cooldown = configManager.getRocketCooldown();
        cooldownManager.setCooldown(player, COOLDOWN_KEY, cooldown);

        // Send success message
        Map<String, String> placeholders = new HashMap<>();
        messageManager.sendMessage(player, "tools.rocket.success", placeholders);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info(String.format(
                    "Player %s used Amethyst Rocket for elytra boost",
                    player.getName()
            ));
        }
    }

    private void launchPlayer(Player player) {
        // Get player's current velocity and direction
        Vector playerVelocity = player.getVelocity();
        Vector direction = player.getLocation().getDirection();

        // Calculate boost force (similar to firework rocket)
        double boostStrength = 1.5; // Configurable boost strength
        Vector boost = direction.multiply(boostStrength);

        // Apply the boost while preserving some existing velocity
        Vector newVelocity = playerVelocity.add(boost);

        // Limit maximum velocity to prevent excessive speed
        double maxSpeed = 3.0;
        if (newVelocity.length() > maxSpeed) {
            newVelocity = newVelocity.normalize().multiply(maxSpeed);
        }

        // Apply the new velocity
        player.setVelocity(newVelocity);

        // Create visual firework effect
        createVisualEffect(player);
    }

    private void createVisualEffect(Player player) {
        Location playerLoc = player.getLocation();

        // Spawn a firework behind the player for visual effect
        Location fireworkLoc = playerLoc.clone().subtract(player.getLocation().getDirection().multiply(2));

        Firework firework = (Firework) playerLoc.getWorld().spawnEntity(fireworkLoc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        // Create a beautiful amethyst-colored effect
        Random random = new Random();
        Color[] amethystColors = {
                Color.fromRGB(157, 78, 221),  // Amethyst purple
                Color.fromRGB(199, 125, 255), // Light purple
                Color.fromRGB(224, 170, 255), // Very light purple
                Color.fromRGB(139, 92, 246),  // Deep purple
                Color.fromRGB(99, 102, 241)   // Blue-purple
        };

        FireworkEffect effect = FireworkEffect.builder()
                .withColor(amethystColors[random.nextInt(amethystColors.length)])
                .withFade(amethystColors[random.nextInt(amethystColors.length)])
                .with(FireworkEffect.Type.BURST)
                .withTrail()
                .withFlicker()
                .build();

        meta.addEffect(effect);
        meta.setPower(0); // Short duration
        firework.setFireworkMeta(meta);

        // Detonate the firework quickly for immediate visual effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (firework.isValid()) {
                    firework.detonate();
                }
            }
        }.runTaskLater(plugin, 5L); // Detonate after 5 ticks

        // Add some particle effects around the player
        addParticleTrail(player);
    }

    private void addParticleTrail(Player player) {
        Location playerLoc = player.getLocation();

        // Create a particle trail effect
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20; // 1 second trail

            @Override
            public void run() {
                if (!player.isOnline() || !player.isGliding() || ticks >= maxTicks) {
                    cancel();
                    return;
                }

                Location currentLoc = player.getLocation();

                // Spawn particles behind the player
                Vector direction = currentLoc.getDirection().multiply(-1); // Behind player
                Location particleLoc = currentLoc.add(direction.multiply(0.5));

                // Spawn amethyst-colored particles
                if (currentLoc.getWorld() != null) {
                    // Use END_ROD particles for a magical trail effect
                    currentLoc.getWorld().spawnParticle(
                            org.bukkit.Particle.END_ROD,
                            particleLoc,
                            3, // count
                            0.2, 0.2, 0.2, // spread
                            0.05 // speed
                    );

                    // Add some purple particles
                    currentLoc.getWorld().spawnParticle(
                            Particle.DRAGON_BREATH,
                            particleLoc,
                            2, // count
                            0.1, 0.1, 0.1, // spread
                            0.02 // speed
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
    }
}