package com.example.mass.service;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import com.example.mass.enchant.LighteningEnchant;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class WeightService {

    public static final NamespacedKey PDC_WEIGHT = new NamespacedKey("mass", "weight");

    private final MassPlugin plugin;
    private       MassConfig config;

    public WeightService(@NotNull MassPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.massConfig();
    }

    public void reloadConfig() {
        this.config = plugin.massConfig();
    }

    public double getBaseWeight(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR) return 0.0;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(PDC_WEIGHT, PersistentDataType.DOUBLE)) {
                double cached = pdc.get(PDC_WEIGHT, PersistentDataType.DOUBLE);
                if (config.debugLogWeightLookups)
                    plugin.getLogger().info("[Weight] PDC hit for " + item.getType() + " → " + cached);
                return cached;
            }
        }

        double weight = resolveFromConfig(item.getType());
        if (config.debugLogWeightLookups)
            plugin.getLogger().info("[Weight] Config lookup " + item.getType() + " → " + weight);
        return weight;
    }

    public double getEffectiveWeight(@NotNull ItemStack item) {
        double base = getBaseWeight(item);
        int level = LighteningEnchant.levelOf(item);
        if (level > 0 && isArmour(item.getType())) {
            return base * (1.0 - config.getLighteningReduction(level));
        }
        return base;
    }

    public void setWeight(@NotNull ItemStack item, double weight) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(PDC_WEIGHT, PersistentDataType.DOUBLE, weight);
        item.setItemMeta(meta);
    }

    public void clearWeight(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(PDC_WEIGHT);
        item.setItemMeta(meta);
    }

    public double computeTotalWeight(@NotNull Player player) {
        double total = 0;

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && piece.getType() != Material.AIR)
                total += getEffectiveWeight(piece) * piece.getAmount();
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() != Material.AIR)
            total += getBaseWeight(offHand) * offHand.getAmount();

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR)
                total += getBaseWeight(item) * item.getAmount();
        }

        // Chest boat cargo counts against the rider, same as a donkey chest
        if (player.getVehicle() instanceof ChestBoat chestBoat) {
            for (ItemStack item : chestBoat.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR)
                    total += getBaseWeight(item) * item.getAmount();
            }
        }

        return total;
    }

    public double computeArmorWeight(@NotNull Player player) {
        double total = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && piece.getType() != Material.AIR)
                total += getEffectiveWeight(piece) * piece.getAmount();
        }
        return total;
    }

    public double computeHandWeight(@NotNull Player player) {
        double total = 0;
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off  = player.getInventory().getItemInOffHand();
        if (main.getType() != Material.AIR) total += getBaseWeight(main) * main.getAmount();
        if (off.getType()  != Material.AIR) total += getBaseWeight(off)  * off.getAmount();
        return total;
    }

    public double computeInventoryWeight(@NotNull Player player) {
        double total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR)
                total += getBaseWeight(item) * item.getAmount();
        }
        return total;
    }

    private double resolveFromConfig(@NotNull Material material) {
        Double w = config.getConfiguredWeight(material);
        if (w != null) return w;
        return resolveCategory(material.name());
    }

    private double resolveCategory(String name) {
        if (name.endsWith("_CONCRETE_POWDER"))          return config.categoryWeight("concrete_powder");
        if (name.endsWith("_CONCRETE"))                 return config.categoryWeight("concrete");
        if (name.endsWith("_STAINED_GLASS_PANE")
         || name.endsWith("_GLASS_PANE"))               return config.categoryWeight("glass_pane");
        if (name.endsWith("_STAINED_GLASS"))            return config.categoryWeight("glass");
        if (name.endsWith("_GLAZED_TERRACOTTA")
         || (name.endsWith("_TERRACOTTA")
             && !name.equals("TERRACOTTA")))            return config.categoryWeight("terracotta");
        if (name.endsWith("_WOOL"))                     return config.categoryWeight("wool");
        if (name.endsWith("_CARPET"))                   return config.categoryWeight("carpet");
        if (name.endsWith("_HANGING_SIGN"))             return config.categoryWeight("hanging_sign");
        if (name.endsWith("_SIGN"))                     return config.categoryWeight("sign");
        if (name.endsWith("_FENCE_GATE"))               return config.categoryWeight("fence_gate");
        if (name.endsWith("_FENCE"))                    return config.categoryWeight("fence");
        if (name.endsWith("_STAIRS"))                   return config.categoryWeight("stairs");
        if (name.endsWith("_SLAB"))                     return config.categoryWeight("slab");
        if (name.endsWith("_WALL"))                     return config.categoryWeight("wall");
        if (name.endsWith("_LEAVES"))                   return config.categoryWeight("leaves");
        if (name.endsWith("_BED"))                      return config.categoryWeight("bed");
        if (name.endsWith("_BANNER"))                   return config.categoryWeight("banner");
        if (name.endsWith("_CANDLE"))                   return config.categoryWeight("candle");
        if (name.endsWith("_TRAPDOOR"))                 return config.categoryWeight("trapdoor");
        if (name.endsWith("_DOOR"))                     return config.categoryWeight("door");
        if (name.endsWith("_PRESSURE_PLATE"))           return config.categoryWeight("pressure_plate");
        if (name.endsWith("_BUTTON"))                   return config.categoryWeight("button");
        if (name.endsWith("_SAPLING"))                  return config.categoryWeight("sapling");
        if (name.endsWith("_CORAL_FAN"))                return config.categoryWeight("coral_fan");
        if (name.endsWith("_CORAL_BLOCK"))              return config.categoryWeight("coral_block");
        if (name.endsWith("_CORAL"))                    return config.categoryWeight("coral");
        if (name.endsWith("_SPAWN_EGG"))                return config.categoryWeight("spawn_egg");
        if (name.endsWith("_POTTERY_SHERD"))            return config.categoryWeight("pottery_sherd");
        if (name.endsWith("_SMITHING_TEMPLATE"))        return config.categoryWeight("smithing_template");
        if (name.startsWith("MUSIC_DISC_"))             return config.categoryWeight("music_disc");
        if (name.endsWith("_SEEDS") || name.endsWith("_SEED")) return config.categoryWeight("seeds");
        if (name.endsWith("_DYE"))                      return config.categoryWeight("dye");
        return config.weightFallback;
    }

    static boolean isArmour(@NotNull Material m) {
        String name = m.name();
        return name.endsWith("_HELMET")
            || name.endsWith("_CHESTPLATE")
            || name.endsWith("_LEGGINGS")
            || name.endsWith("_BOOTS");
    }
}
