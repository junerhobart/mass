package com.example.mass.listener;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import com.example.mass.service.EncumbranceService;
import com.example.mass.service.WeightService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class VehicleListener implements Listener {

    private final MassPlugin        plugin;
    private final EncumbranceService encumbrance;
    private final WeightService      weightService;

    public VehicleListener(MassPlugin plugin, EncumbranceService encumbrance, WeightService weightService) {
        this.plugin        = plugin;
        this.encumbrance   = encumbrance;
        this.weightService = weightService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player player)) return;
        Entity vehicle = e.getVehicle();
        if (!(vehicle instanceof LivingEntity) || vehicle instanceof Player) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) encumbrance.scheduleUpdate(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player player)) return;
        Entity vehicle = e.getVehicle();

        if (vehicle instanceof LivingEntity livingVehicle && !(vehicle instanceof Player)) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> encumbrance.removeLivingVehicleModifier(livingVehicle), 1L);
        }

        encumbrance.scheduleUpdate(player);
    }

    // Cap boat horizontal speed based on rider + chest cargo weight.
    // Fires each tick the boat moves; we only intervene when speed exceeds the cap.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBoatMove(VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof Boat boat)) return;

        MassConfig cfg = plugin.massConfig();
        if (!cfg.vehicleBoatsEnabled) return;

        Player player = null;
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Player p) { player = p; break; }
        }
        if (player == null) return;

        double load = weightService.computeTotalWeight(player);

        if (boat instanceof ChestBoat chestBoat) {
            for (ItemStack item : chestBoat.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR)
                    load += weightService.getBaseWeight(item) * item.getAmount();
            }
        }

        double reduction = Math.min(cfg.vehicleBoatMaxReduction, load * cfg.vehicleBoatSpeedReductionPerKg);
        if (reduction <= 0) return;

        double maxSpeed = cfg.vehicleBoatBaseSpeed * (1.0 - reduction);
        Vector vel = boat.getVelocity();
        double horiz = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

        if (horiz > maxSpeed && horiz > 0.001) {
            double scale = maxSpeed / horiz;
            boat.setVelocity(new Vector(vel.getX() * scale, vel.getY(), vel.getZ() * scale));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!e.isGliding()) return;

        MassConfig cfg = plugin.massConfig();
        if (!cfg.vehicleElytraEnabled) return;

        double weight = weightService.computeTotalWeight(player);
        double limit  = cfg.vehicleElytraDisableAbove;

        if (weight > limit) {
            e.setCancelled(true);
            player.sendMessage(Component.text(
                    String.format("Too heavy to glide! (%.1f / %.1f kg)", weight, limit))
                    .color(NamedTextColor.RED));
        }
    }
}
