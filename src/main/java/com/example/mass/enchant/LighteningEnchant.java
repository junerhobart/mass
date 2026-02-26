package com.example.mass.enchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public final class LighteningEnchant {

    public static final String        NAMESPACE  = "mass";
    public static final String        PATH       = "lightening";
    public static final NamespacedKey KEY        = new NamespacedKey(NAMESPACE, PATH);
    public static final TypedKey<Enchantment> TYPED_KEY =
            TypedKey.create(RegistryKey.ENCHANTMENT, Key.key(NAMESPACE, PATH));

    private LighteningEnchant() {}

    public static @Nullable Enchantment get() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(KEY);
    }

    public static int levelOf(@NotNull ItemStack item) {
        Enchantment ench = get();
        return ench == null ? 0 : item.getEnchantmentLevel(ench);
    }
}
