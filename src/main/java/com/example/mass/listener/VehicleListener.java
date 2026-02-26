package com.example.mass.listener;

import com.example.mass.MassPlugin;
import com.example.mass.service.EncumbranceService;
import com.example.mass.service.WeightService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

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
        if (!(vehicle instanceof AbstractHorse)) return;

        // Schedule 2 ticks out so the player is fully seated before we read their inventory
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) encumbrance.scheduleUpdate(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player player)) return;
        Entity vehicle = e.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) return;

        // Remove the load modifier from the horse when the rider gets off
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                encumbrance.removeHorseModifier(horse), 1L);

        encumbrance.scheduleUpdate(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!e.isGliding()) return;   // only block starting to glide, not stopping

        if (!plugin.massConfig().vehicleElytraEnabled) return;

        double weight = weightService.computeTotalWeight(player);
        double limit  = plugin.massConfig().vehicleElytraDisableAbove;

        if (weight > limit) {
            e.setCancelled(true);
            player.sendMessage(Component.text(
                    String.format("Too heavy to glide! (%.1f / %.1f kg)", weight, limit))
                    .color(NamedTextColor.RED));
        }
    }
}
