package com.example.mass.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.example.mass.service.EncumbranceService;
import com.example.mass.service.LoreService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PlayerListener implements Listener {

    private final EncumbranceService encumbrance;
    private final LoreService        lore;

    public PlayerListener(EncumbranceService encumbrance, LoreService lore) {
        this.encumbrance = encumbrance;
        this.lore        = lore;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        refreshAllLore(e.getPlayer());
        encumbrance.scheduleUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        encumbrance.cleanup(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack current = e.getCurrentItem();
        if (current != null && current.getType() != Material.AIR) {
            lore.updateLore(current);
            e.setCurrentItem(current);
        }
        ItemStack cursor = e.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            lore.updateLore(cursor);
        }

        encumbrance.scheduleUpdate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack old = e.getOldCursor();
        if (old != null && old.getType() != Material.AIR) lore.updateLore(old);
        encumbrance.scheduleUpdate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Inventory inv = e.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) lore.updateLore(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        encumbrance.scheduleUpdate(player);
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) lore.updateLore(item);
        }
        player.getInventory().setStorageContents(contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        encumbrance.scheduleUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        encumbrance.scheduleUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        ItemStack item = e.getItem().getItemStack();
        lore.updateLore(item);
        e.getItem().setItemStack(item);
        encumbrance.scheduleUpdate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        encumbrance.scheduleUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        ItemStack item = e.getNewItem();
        if (item != null && item.getType() != Material.AIR) lore.updateLore(item);
        encumbrance.scheduleUpdate(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent e) {
        if (e.isSprinting() && encumbrance.isSprintDisabled(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent e) {
        if (encumbrance.isJumpDisabled(e.getPlayer()))
            e.setCancelled(true);
    }

    private void refreshAllLore(Player player) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack item : contents)
            if (item != null && item.getType() != Material.AIR) lore.updateLore(item);
        player.getInventory().setStorageContents(contents);

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor)
            if (piece != null && piece.getType() != Material.AIR) lore.updateLore(piece);
        player.getInventory().setArmorContents(armor);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() != Material.AIR) {
            lore.updateLore(offHand);
            player.getInventory().setItemInOffHand(offHand);
        }
    }
}
