package com.example.mass.service;

import com.example.mass.MassPlugin;
import com.example.mass.config.MassConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class LoreService {

    private static final String WEIGHT_PREFIX    = "Weight: ";
    private static final String EFFECTIVE_PREFIX = "Effective: ";

    private final MassPlugin    plugin;
    private       MassConfig    config;
    private final WeightService weightService;

    public LoreService(@NotNull MassPlugin plugin, @NotNull WeightService weightService) {
        this.plugin        = plugin;
        this.config        = plugin.massConfig();
        this.weightService = weightService;
    }

    public void reloadConfig() {
        this.config = plugin.massConfig();
    }

    public void updateLore(@NotNull ItemStack item) {
        if (!config.loreEnabled) return;
        if (item.getType() == Material.AIR) return;
        // Stackable items (food, materials, blocks, etc.) must not have their NBT modified —
        // custom lore makes them unable to merge with clean items of the same type,
        // which breaks furnace fuel/ingredient stacking and normal inventory behaviour.
        if (item.getType().getMaxStackSize() > 1) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int    amount    = item.getAmount();
        double base      = weightService.getBaseWeight(item)      * amount;
        double effective = weightService.getEffectiveWeight(item) * amount;

        List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());

        Component weightLine = buildLine(config.loreWeightFormat, "{weight}", base);
        int idx = findLine(lore, WEIGHT_PREFIX);
        if (idx >= 0) lore.set(idx, weightLine);
        else          lore.add(0, weightLine);

        if (config.loreEffectiveEnabled && effective != base) {
            Component effLine = buildLine(config.loreEffectiveFormat, "{effective}", effective);
            int effIdx = findLine(lore, EFFECTIVE_PREFIX);
            if (effIdx >= 0) lore.set(effIdx, effLine);
            else             lore.add(findLine(lore, WEIGHT_PREFIX) + 1, effLine);
        } else {
            lore.removeIf(c -> plain(c).startsWith(EFFECTIVE_PREFIX));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public void clearLore(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;
        List<Component> cleaned = new ArrayList<>(lore);
        cleaned.removeIf(c -> {
            String p = plain(c);
            return p.startsWith(WEIGHT_PREFIX) || p.startsWith(EFFECTIVE_PREFIX);
        });
        meta.lore(cleaned);
        item.setItemMeta(meta);
    }

    private static Component buildLine(String format, String placeholder, double value) {
        String text = format.replace(placeholder, String.format("%.1f", value));
        NamedTextColor color = placeholder.equals("{weight}") ? NamedTextColor.GRAY : NamedTextColor.AQUA;
        return Component.text(text)
                .color(color)
                .decoration(TextDecoration.ITALIC, true);
    }

    private static int findLine(@NotNull List<Component> lore, @NotNull String prefix) {
        for (int i = 0; i < lore.size(); i++)
            if (plain(lore.get(i)).startsWith(prefix)) return i;
        return -1;
    }

    private static String plain(@NotNull Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }
}
