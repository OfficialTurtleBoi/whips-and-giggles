package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;
import net.turtleboi.whipsandgiggles.item.ModItems;

public class ModItemTagsProvider extends TagsProvider<Item> {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup, ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, registryLookup, WhipsAndGiggles.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registryLookup) {
        tag(ModEnchantments.WHIP_ENCHANTABLE_ITEMS).add(ModItems.WHIP.getKey());
        tag(ItemTags.WEAPON_ENCHANTABLE).add(ModItems.WHIP.getKey());
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).add(ModItems.WHIP.getKey());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(ModItems.WHIP.getKey());
    }
}
