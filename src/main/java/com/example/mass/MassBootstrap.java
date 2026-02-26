package com.example.mass;

import com.example.mass.enchant.LighteningEnchant;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

@SuppressWarnings("UnstableApiUsage")
public final class MassBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(
            RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {

                var armorItems = RegistrySet.keySet(RegistryKey.ITEM,
                        ItemTypeKeys.LEATHER_HELMET,    ItemTypeKeys.LEATHER_CHESTPLATE,
                        ItemTypeKeys.LEATHER_LEGGINGS,  ItemTypeKeys.LEATHER_BOOTS,
                        ItemTypeKeys.CHAINMAIL_HELMET,  ItemTypeKeys.CHAINMAIL_CHESTPLATE,
                        ItemTypeKeys.CHAINMAIL_LEGGINGS, ItemTypeKeys.CHAINMAIL_BOOTS,
                        ItemTypeKeys.IRON_HELMET,       ItemTypeKeys.IRON_CHESTPLATE,
                        ItemTypeKeys.IRON_LEGGINGS,     ItemTypeKeys.IRON_BOOTS,
                        ItemTypeKeys.GOLDEN_HELMET,     ItemTypeKeys.GOLDEN_CHESTPLATE,
                        ItemTypeKeys.GOLDEN_LEGGINGS,   ItemTypeKeys.GOLDEN_BOOTS,
                        ItemTypeKeys.DIAMOND_HELMET,    ItemTypeKeys.DIAMOND_CHESTPLATE,
                        ItemTypeKeys.DIAMOND_LEGGINGS,  ItemTypeKeys.DIAMOND_BOOTS,
                        ItemTypeKeys.NETHERITE_HELMET,  ItemTypeKeys.NETHERITE_CHESTPLATE,
                        ItemTypeKeys.NETHERITE_LEGGINGS, ItemTypeKeys.NETHERITE_BOOTS,
                        ItemTypeKeys.TURTLE_HELMET);

                event.registry().register(
                    LighteningEnchant.TYPED_KEY,
                    b -> b
                        .description(Component.text("Lightening"))
                        .supportedItems(armorItems)
                        .primaryItems(armorItems)
                        .weight(5)
                        .maxLevel(3)
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(5, 8))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(8, 8))
                        .anvilCost(2)
                        .activeSlots(EquipmentSlotGroup.ARMOR)
                );
            })
        );
    }
}
