package com.example.mass.service;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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

    private static final NamespacedKey SPEED_KEY = new NamespacedKey("mass", "encumbrance_speed");
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

        applySpeedModifier(player, tier.speedMultiplier());
        applyJumpModifier(player, tier.jumpMultiplier());

        // Disable sprint if overloaded
        double sprintThreshold = config.disableSprintAbove;
        if (sprintThreshold > 0 && totalWeight > sprintThreshold && player.isSprinting()) {
            player.setSprinting(false);
        }

        // Apply speed penalty to any living vehicle (horse, donkey, pig, strider, llama, etc.)
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity livingVehicle && !(vehicle instanceof Player)) {
            applyLivingVehiclePenalty(player, livingVehicle, totalWeight);
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

        // Include chest contents for donkeys, mules, llamas with chests
        if (vehicle instanceof ChestedHorse chestedHorse && chestedHorse.isCarryingChest()) {
            for (ItemStack item : chestedHorse.getInventory().getContents()) {
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
        Vector vel = player.getVelocity();
        if (vel.getY() > -config.waterSinkSpeed) {
            vel.setY(-config.waterSinkSpeed);
            player.setVelocity(vel);
        }
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

    private void applySpeedModifier(@NotNull Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        removeKey(attr, SPEED_KEY);
        if (multiplier < 1.0) {
            attr.addModifier(new AttributeModifier(SPEED_KEY, multiplier - 1.0,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
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

    private void removeModifiers(@NotNull Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance jump  = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (speed != null) removeKey(speed, SPEED_KEY);
        if (jump  != null) removeKey(jump,  JUMP_KEY);
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
