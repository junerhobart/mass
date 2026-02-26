package com.example.mass.listener;

import com.example.mass.MassPlugin;
import com.example.mass.enchant.LighteningEnchant;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Fallback: appends a Lightening book to naturally generated loot.
// Prefer a datapack loot table entry in production; this fires after the loot table runs.
public final class LootListener implements Listener {

    private final MassPlugin plugin;
    private final Random     rng = new Random();

    public LootListener(MassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent e) {
        if (!plugin.massConfig().sourceLoot) return;

        Enchantment lightening = LighteningEnchant.get();
        if (lightening == null) return;
        if (rng.nextDouble() > plugin.massConfig().sourceLootInjectChance) return;

        int level = 1 + rng.nextInt(lightening.getMaxLevel());
        ItemStack book = buildLighteningBook(lightening, level);

        List<ItemStack> loot = new ArrayList<>(e.getLoot());
        boolean replaced = false;
        for (int i = 0; i < loot.size(); i++) {
            ItemStack item = loot.get(i);
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                loot.set(i, book);
                replaced = true;
                break;
            }
        }
        if (!replaced) loot.add(book);
        e.setLoot(loot);

        if (plugin.massConfig().debugLogLootInjection)
            plugin.getLogger().info("[Loot] Injected Lightening " + level + " book at " + e.getLootContext().getLocation());
    }

    private static ItemStack buildLighteningBook(Enchantment lightening, int level) {
        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(lightening, level, false);
            book.setItemMeta(meta);
        }
        return book;
    }
}
