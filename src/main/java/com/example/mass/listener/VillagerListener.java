package com.example.mass.listener;

import com.example.mass.MassPlugin;
import com.example.mass.enchant.LighteningEnchant;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Random;

// Fallback: injects Lightening enchanted book trades when a librarian acquires a new trade slot.
// The registered enchantment should appear natively in villager trades, so this is a safety net.
public final class VillagerListener implements Listener {

    private final MassPlugin plugin;
    private final Random     rng = new Random();

    public VillagerListener(MassPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent e) {
        if (!plugin.massConfig().sourceVillagers) return;
        if (!(e.getEntity() instanceof AbstractVillager)) return;

        Enchantment lightening = LighteningEnchant.get();
        if (lightening == null) return;

        MerchantRecipe original = e.getRecipe();
        if (!isEnchantedBookTrade(original)) return;
        if (rng.nextDouble() > plugin.massConfig().sourceLootInjectChance) return;

        int level = 1 + rng.nextInt(lightening.getMaxLevel());
        ItemStack book = buildLighteningBook(lightening, level);

        MerchantRecipe injected = new MerchantRecipe(
                book,
                original.getUses(), original.getMaxUses(),
                original.hasExperienceReward(), original.getVillagerExperience(),
                original.getPriceMultiplier());
        for (ItemStack ingredient : original.getIngredients())
            injected.addIngredient(ingredient);

        e.setRecipe(injected);

        if (plugin.massConfig().debugLogLootInjection)
            plugin.getLogger().info("[VillagerTrade] Injected Lightening " + level + " book for " + e.getEntity().getType());
    }

    private static boolean isEnchantedBookTrade(MerchantRecipe recipe) {
        return recipe.getResult().getType() == org.bukkit.Material.ENCHANTED_BOOK;
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
