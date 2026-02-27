package com.example.mass.listener;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import com.example.mass.service.WeightService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public final class MobListener implements Listener {

    private static final NamespacedKey SPEED_KEY = new NamespacedKey("mass", "mob_speed");

    private final MassPlugin    plugin;
    private final WeightService weights;

    public MobListener(MassPlugin plugin, WeightService weights) {
        this.plugin  = plugin;
        this.weights = weights;
    }

    // Delay 1 tick so natural spawn equipment (spawner NBT, etc.) is fully applied
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob instanceof Player) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) applyPenalty(mob);
        }, 1L);
    }

    public void applyPenalty(Mob mob) {
        MassConfig config = plugin.massConfig();
        if (!config.mobsEnabled || !config.penaltiesEnabled) {
            clearModifiers(mob);
            return;
        }
        double armorWeight = computeArmorWeight(mob);
        MassConfig.PenaltyTier tier = config.resolveTier(armorWeight);
        applySpeedModifier(mob, tier.speedMultiplier());
    }

    private double computeArmorWeight(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return 0.0;
        double total = 0.0;
        for (ItemStack piece : eq.getArmorContents()) {
            if (piece != null && piece.getType() != Material.AIR)
                total += weights.getBaseWeight(piece);
        }
        return total;
    }

    private static void applySpeedModifier(LivingEntity entity, double multiplier) {
        AttributeInstance attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        removeKey(attr, SPEED_KEY);
        if (multiplier < 1.0) {
            attr.addModifier(new AttributeModifier(SPEED_KEY, multiplier - 1.0,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    public static void clearModifiers(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) removeKey(speed, SPEED_KEY);
    }

    private static void removeKey(AttributeInstance attr, NamespacedKey key) {
        Collection<AttributeModifier> mods = attr.getModifiers();
        for (AttributeModifier mod : mods) {
            if (key.equals(mod.getKey())) {
                attr.removeModifier(mod);
                return;
            }
        }
    }
}
