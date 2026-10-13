package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;

public class ModEnchantmentProvider extends DatapackBuiltinEntriesProvider {
    public ModEnchantmentProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture, new RegistrySetBuilder().add(Registries.ENCHANTMENT, ModEnchantmentProvider::bootstrapEnchantments),
                Set.of(WhipsAndGiggles.MOD_ID));
    }

    private static void bootstrapEnchantments(BootstrapContext<Enchantment> bootstrapContext) {
        var enchantableWhipItems = bootstrapContext.lookup(Registries.ITEM).getOrThrow(ModEnchantments.WHIP_ENCHANTABLE_ITEMS);
        var exclusiveWhipEnchantments = bootstrapContext.lookup(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.EXCLUSIVE_WHIP_ENCHANTMENTS);
        bootstrapContext.register(ModEnchantments.GRAPPLING, Enchantment.enchantment(Enchantment.definition(
                        enchantableWhipItems, enchantableWhipItems, 2, 1,
                        Enchantment.constantCost(20), Enchantment.constantCost(50), 4, EquipmentSlotGroup.HAND))
                .exclusiveWith(exclusiveWhipEnchantments).build(ModEnchantments.GRAPPLING.identifier()));
        bootstrapContext.register(ModEnchantments.PIERCING, Enchantment.enchantment(Enchantment.definition(
                        enchantableWhipItems, enchantableWhipItems, 5, 3,
                        Enchantment.dynamicCost(10, 10), Enchantment.dynamicCost(40, 10), 2, EquipmentSlotGroup.HAND))
                .exclusiveWith(exclusiveWhipEnchantments).build(ModEnchantments.PIERCING.identifier()));
        bootstrapContext.register(ModEnchantments.REACH, Enchantment.enchantment(Enchantment.definition(
                        enchantableWhipItems, enchantableWhipItems, 5, 3,
                        Enchantment.dynamicCost(10, 10), Enchantment.dynamicCost(40, 10), 2, EquipmentSlotGroup.HAND))
                .build(ModEnchantments.REACH.identifier()));
    }
}
