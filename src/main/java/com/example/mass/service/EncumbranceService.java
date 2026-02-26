package com.example.mass.service;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EncumbranceService {

    private static final NamespacedKey JUMP_KEY  = new NamespacedKey("mass", "encumbrance_jump");
    private static final NamespacedKey HORSE_KEY = new NamespacedKey("mass", "horse_load");

    private final MassPlugin    plugin;
    private       MassConfig    config;
    private final WeightService weightService;

    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public EncumbranceService(@NotNull MassPlugin plugin, @NotNull WeightService weightService) {
        this.plugin        = plugin;
        this.config        = plugin.massConfig();
        this.weightService = weightService;
    }

    public void reloadConfig() {
        this.config = plugin.massConfig();
    }

    public void scheduleUpdate(@NotNull Player player) {
        UUID id = player.getUniqueId();
        BukkitTask existing = pending.remove(id);
        if (existing != null) existing.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            if (player.isOnline()) applyPenalty(player);
        }, 1L);
        pending.put(id, task);
    }

    public void cleanup(@NotNull Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = pending.remove(id);
        if (task != null) task.cancel();
        removeModifiers(player);
    }

    public void applyPenalty(@NotNull Player player) {
        if (!config.penaltiesEnabled) {
            removeModifiers(player);
            return;
        }

        double totalWeight = weightService.computeTotalWeight(player);
        MassConfig.PenaltyTier tier = config.resolveTier(totalWeight);

        Entity vehicle = player.getVehicle();
        boolean onLivingVehicle = vehicle instanceof LivingEntity && !(vehicle instanceof Player);

        if (onLivingVehicle) {
            // While mounted the vehicle's attribute controls movement speed.
            // Reset the player's own modifiers so they don't compound with it.
            applySpeedModifier(player, 1.0);
            applyJumpModifier(player, 1.0);
            applyLivingVehiclePenalty(player, (LivingEntity) vehicle, totalWeight);
        } else {
            applySpeedModifier(player, tier.speedMultiplier());
            applyJumpModifier(player, tier.jumpMultiplier());

            double sprintThreshold = config.disableSprintAbove;
            if (sprintThreshold > 0 && totalWeight > sprintThreshold && player.isSprinting()) {
                player.setSprinting(false);
            }
        }

        if (config.debugLogPenaltyChanges) {
            plugin.getLogger().info(String.format("[Encumbrance] %s → %.1f kg  speed=%.0f%%  jump=%.0f%%",
                    player.getName(), totalWeight,
                    tier.speedMultiplier() * 100, tier.jumpMultiplier() * 100));
        }
    }

    public void applyLivingVehiclePenalty(@NotNull Player player, @NotNull LivingEntity vehicle, double riderWeight) {
        if (!config.vehicleHorsesEnabled) return;

        double load = riderWeight;

        // Include saddle, armor/decor, and chest contents for all horse-type animals
        if (vehicle instanceof AbstractHorse abstractHorse) {
            for (ItemStack item : abstractHorse.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    load += weightService.getBaseWeight(item) * item.getAmount();
                }
            }
        }

        AttributeInstance attr = vehicle.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        removeKey(attr, HORSE_KEY);

        double reduction = Math.min(config.vehicleMaxReduction, load * config.vehicleSpeedReductionPerKg);
        if (reduction > 0) {
            attr.addModifier(new AttributeModifier(HORSE_KEY, -reduction,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    public void removeLivingVehicleModifier(@NotNull LivingEntity vehicle) {
        AttributeInstance attr = vehicle.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) removeKey(attr, HORSE_KEY);
    }

    public void tickWater(@NotNull Player player) {
        double weight = weightService.computeTotalWeight(player);
        if (weight <= config.waterSinkAbove) return;

        // Require mid-body to be in water to avoid triggering at shallow edges
        if (player.getLocation().add(0, 0.6, 0).getBlock().getType() != Material.WATER) return;

        // ratio: 0 = just over threshold, 1 = double the threshold, capped at 4
        double ratio = Math.min(4.0, (weight - config.waterSinkAbove) / config.waterSinkAbove);

        Vector vel = player.getVelocity();

        // At low excess allow slight swim-up; at high excess clamp to 0 (can't surface)
        double maxUp = Math.max(0.0, 0.10 - ratio * 0.03);
        if (vel.getY() > maxUp) vel.setY(maxUp);

        // Downward pull and terminal sink speed both scale with how overloaded the player is
        double pullPerCall = 0.025 + ratio * 0.035;
        double sinkCap     = config.waterSinkSpeed * (1.0 + ratio * 0.75);
        double newY = Math.max(-sinkCap, vel.getY() - pullPerCall);
        vel.setY(newY);
        player.setVelocity(vel);
    }

    public boolean isSprintDisabled(@NotNull Player player) {
        double threshold = config.disableSprintAbove;
        if (threshold <= 0) return false;
        return weightService.computeTotalWeight(player) > threshold;
    }

    public boolean isJumpDisabled(@NotNull Player player) {
        return config.resolveTier(weightService.computeTotalWeight(player)).jumpMultiplier() <= 0.0;
    }

    public double getTotalWeight(@NotNull Player player) {
        return weightService.computeTotalWeight(player);
    }

    private static void applySpeedModifier(@NotNull Player player, double multiplier) {
        // setWalkSpeed only updates the abilities packet, leaving the generic.movement_speed
        // attribute at its base value. The client FOV formula reads the attribute, so FOV
        // stays unchanged while the player's actual walking speed is correctly reduced.
        float speed = multiplier >= 1.0 ? 0.2f : (float) Math.max(0.02, 0.2 * multiplier);
        player.setWalkSpeed(speed);
    }

    private void applyJumpModifier(@NotNull Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (attr == null) return;
        removeKey(attr, JUMP_KEY);
        if (multiplier < 1.0) {
            attr.addModifier(new AttributeModifier(JUMP_KEY, multiplier - 1.0,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private static void removeModifiers(@NotNull Player player) {
        player.setWalkSpeed(0.2f);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) removeKey(jump, JUMP_KEY);
    }

    private static void removeKey(@NotNull AttributeInstance attr, @NotNull NamespacedKey key) {
        Collection<AttributeModifier> mods = attr.getModifiers();
        for (AttributeModifier mod : mods) {
            if (key.equals(mod.getKey())) {
                attr.removeModifier(mod);
                return;
            }
        }
    }
}
