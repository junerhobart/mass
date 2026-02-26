package com.example.mass.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class MassConfig {

    public final boolean loreEnabled;
    public final String  loreWeightFormat;
    public final boolean loreEffectiveEnabled;
    public final String  loreEffectiveFormat;

    private final Map<String, Double> weightOverrides  = new HashMap<>();
    private final Map<String, Double> weightTable      = new HashMap<>();
    private final Map<String, Double> weightCategories = new HashMap<>();
    public final double weightFallback;

    public final String scopeMode;

    public final boolean penaltiesEnabled;
    public final List<PenaltyTier> penaltyTiers;
    public final double disableSprintAbove;

    public final boolean lighteningEnabled;
    public final int     lighteningMaxLevel;
    private final Map<Integer, Double> lighteningReductions = new HashMap<>();
    public final boolean lighteningTreasure;
    public final boolean lighteningAnvilCombine;

    public final boolean sourceEnchantTable;
    public final boolean sourceVillagers;
    public final int     sourceVillagersMaxPerVillager;
    public final boolean sourceLoot;
    public final double  sourceLootInjectChance;

    public final boolean vehicleHorsesEnabled;
    public final double  vehicleSpeedReductionPerKg;
    public final double  vehicleMaxReduction;
    public final boolean vehicleElytraEnabled;
    public final double  vehicleElytraDisableAbove;
    public final double  waterSinkAbove;
    public final double  waterSinkSpeed;

    public final boolean debugLogWeightLookups;
    public final boolean debugLogPenaltyChanges;
    public final boolean debugLogLootInjection;

    public MassConfig(@NotNull FileConfiguration cfg, @NotNull Logger log) {
        loreEnabled          = cfg.getBoolean("display.lore.enabled", true);
        loreWeightFormat     = cfg.getString("display.lore.weight_format", "Weight: {weight} kg");
        loreEffectiveEnabled = cfg.getBoolean("display.lore.effective_enabled", false);
        loreEffectiveFormat  = cfg.getString("display.lore.effective_format", "Effective: {effective} kg");

        weightFallback = cfg.getDouble("weights.defaults.misc.fallback", 0.05);
        buildWeightTable(cfg, log);
        buildCategories(cfg);

        ConfigurationSection overridesSec = cfg.getConfigurationSection("weights.overrides");
        if (overridesSec != null) {
            for (String key : overridesSec.getKeys(false))
                weightOverrides.put(key.toUpperCase(), overridesSec.getDouble(key));
        }

        scopeMode = cfg.getString("scope.mode", "EVERYTHING");

        penaltiesEnabled   = cfg.getBoolean("penalties.enabled", true);
        disableSprintAbove = cfg.getDouble("penalties.disable_sprint_above", -1);

        List<PenaltyTier> tiers = new ArrayList<>();
        List<?> tierList = cfg.getList("penalties.tiers", Collections.emptyList());
        for (Object obj : tierList) {
            if (obj instanceof Map<?, ?> map) {
                tiers.add(new PenaltyTier(
                        toDouble(map.get("max_weight"), 9999.0),
                        toDouble(map.get("speed_multiplier"), 1.0),
                        toDouble(map.get("jump_multiplier"), 1.0)));
            }
        }
        if (tiers.isEmpty()) tiers.add(new PenaltyTier(9999.0, 1.0, 1.0));
        penaltyTiers = Collections.unmodifiableList(tiers);

        lighteningEnabled      = cfg.getBoolean("enchantments.lightening.enabled", true);
        lighteningMaxLevel     = cfg.getInt("enchantments.lightening.max_level", 3);
        lighteningTreasure     = cfg.getBoolean("enchantments.lightening.treasure", false);
        lighteningAnvilCombine = cfg.getBoolean("enchantments.lightening.anvil_combine", true);

        ConfigurationSection reductions = cfg.getConfigurationSection("enchantments.lightening.reductions");
        if (reductions != null) {
            for (String lvlStr : reductions.getKeys(false)) {
                try { lighteningReductions.put(Integer.parseInt(lvlStr), reductions.getDouble(lvlStr)); }
                catch (NumberFormatException ignored) {}
            }
        }
        lighteningReductions.putIfAbsent(1, 0.30);
        lighteningReductions.putIfAbsent(2, 0.45);
        lighteningReductions.putIfAbsent(3, 0.60);

        sourceEnchantTable            = cfg.getBoolean("sources.enchanting_table.enabled", true);
        sourceVillagers               = cfg.getBoolean("sources.villagers.enabled", true);
        sourceVillagersMaxPerVillager = cfg.getInt("sources.villagers.max_per_villager", 1);
        sourceLoot                    = cfg.getBoolean("sources.loot.enabled", true);
        sourceLootInjectChance        = cfg.getDouble("sources.loot.inject_chance", 0.30);

        vehicleHorsesEnabled             = cfg.getBoolean("vehicles.horses.enabled", true);
        vehicleSpeedReductionPerKg       = cfg.getDouble("vehicles.horses.speed_reduction_per_kg", 0.010);
        vehicleMaxReduction              = cfg.getDouble("vehicles.horses.max_reduction", 0.85);
        vehicleElytraEnabled             = cfg.getBoolean("vehicles.elytra.enabled", true);
        vehicleElytraDisableAbove        = cfg.getDouble("vehicles.elytra.disable_above", 12.0);
        waterSinkAbove                   = cfg.getDouble("water.sink_above", 35.0);
        waterSinkSpeed                   = cfg.getDouble("water.sink_speed", 0.15);

        debugLogWeightLookups  = cfg.getBoolean("debug.log_weight_lookups", false);
        debugLogPenaltyChanges = cfg.getBoolean("debug.log_penalty_changes", false);
        debugLogLootInjection  = cfg.getBoolean("debug.log_loot_injection", false);
    }

    public @Nullable Double getConfiguredWeight(@NotNull Material material) {
        String name = material.name();
        Double override = weightOverrides.get(name);
        if (override != null) return override;
        return weightTable.get(name);
    }

    public double categoryWeight(@NotNull String category) {
        return weightCategories.getOrDefault(category, weightFallback);
    }

    public double getLighteningReduction(int level) {
        return lighteningReductions.getOrDefault(level, 0.0);
    }

    public @NotNull PenaltyTier resolveTier(double totalWeight) {
        for (PenaltyTier tier : penaltyTiers)
            if (totalWeight <= tier.maxWeight()) return tier;
        return penaltyTiers.get(penaltyTiers.size() - 1);
    }

    private void buildCategories(@NotNull FileConfiguration cfg) {
        ConfigurationSection sec = cfg.getConfigurationSection("weights.categories");
        if (sec == null) return;
        for (String key : sec.getKeys(false))
            weightCategories.put(key.toLowerCase(), sec.getDouble(key));
    }

    private void buildWeightTable(@NotNull FileConfiguration cfg, @NotNull Logger log) {
        ConfigurationSection armorSec = cfg.getConfigurationSection("weights.defaults.armor");
        if (armorSec != null) {
            for (String matKey : armorSec.getKeys(false)) {
                ConfigurationSection ms = armorSec.getConfigurationSection(matKey);
                if (ms == null) continue;
                String prefix = armorMaterialPrefix(matKey);
                for (String typeKey : ms.getKeys(false))
                    weightTable.put((prefix + "_" + typeKey).toUpperCase(), ms.getDouble(typeKey));
            }
        }

        ConfigurationSection toolSec = cfg.getConfigurationSection("weights.defaults.tools");
        if (toolSec != null) {
            for (String matKey : toolSec.getKeys(false)) {
                ConfigurationSection ms = toolSec.getConfigurationSection(matKey);
                if (ms == null) continue;
                String prefix = toolMaterialPrefix(matKey);
                for (String typeKey : ms.getKeys(false))
                    weightTable.put((prefix + "_" + typeKey).toUpperCase(), ms.getDouble(typeKey));
            }
        }

        ConfigurationSection weaponSec = cfg.getConfigurationSection("weights.defaults.weapons");
        if (weaponSec != null) {
            for (String matKey : weaponSec.getKeys(false)) {
                ConfigurationSection ms = weaponSec.getConfigurationSection(matKey);
                if (ms instanceof ConfigurationSection sub) {
                    String prefix = toolMaterialPrefix(matKey);
                    for (String typeKey : sub.getKeys(false))
                        weightTable.put((prefix + "_" + typeKey).toUpperCase(), sub.getDouble(typeKey));
                } else {
                    weightTable.put(matKey.toUpperCase(), weaponSec.getDouble(matKey));
                }
            }
        }

        ConfigurationSection miscSec = cfg.getConfigurationSection("weights.defaults.misc");
        if (miscSec != null) {
            for (String key : miscSec.getKeys(false))
                if (!key.equals("fallback"))
                    weightTable.put(key.toUpperCase(), miscSec.getDouble(key));
        }
    }

    private static String armorMaterialPrefix(String key) {
        return switch (key.toLowerCase()) {
            case "leather"        -> "LEATHER";
            case "chainmail"      -> "CHAINMAIL";
            case "iron"           -> "IRON";
            case "golden", "gold" -> "GOLDEN";
            case "diamond"        -> "DIAMOND";
            case "netherite"      -> "NETHERITE";
            case "turtle"         -> "TURTLE";
            default               -> key.toUpperCase();
        };
    }

    private static String toolMaterialPrefix(String key) {
        return switch (key.toLowerCase()) {
            case "wooden", "wood" -> "WOODEN";
            case "stone"          -> "STONE";
            case "iron"           -> "IRON";
            case "golden", "gold" -> "GOLDEN";
            case "diamond"        -> "DIAMOND";
            case "netherite"      -> "NETHERITE";
            default               -> key.toUpperCase();
        };
    }

    private static double toDouble(@Nullable Object o, double def) {
        return o instanceof Number n ? n.doubleValue() : def;
    }

    public record PenaltyTier(double maxWeight, double speedMultiplier, double jumpMultiplier) {}
}
