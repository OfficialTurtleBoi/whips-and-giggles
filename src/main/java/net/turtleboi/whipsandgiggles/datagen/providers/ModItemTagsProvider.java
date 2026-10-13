package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;
import net.turtleboi.whipsandgiggles.item.ModItems;

public class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, Registries.ITEM, registryLookup,
                item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow(), WhipsAndGiggles.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registryLookup) {
        tag(ModEnchantments.WHIP_ENCHANTABLE_ITEMS).add(ModItems.WHIP.get());
        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE).add(ModItems.WHIP.get());
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).add(ModItems.WHIP.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.WHIP.get());
    }
}
