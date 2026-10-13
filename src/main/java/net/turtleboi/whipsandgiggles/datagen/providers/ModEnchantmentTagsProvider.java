package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;

public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup, WhipsAndGiggles.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registryLookup) {
        tag(ModEnchantments.EXCLUSIVE_WHIP_ENCHANTMENTS)
                .add(ModEnchantments.GRAPPLING)
                .add(ModEnchantments.PIERCING);
        tag(EnchantmentTags.IN_ENCHANTING_TABLE)
                .add(ModEnchantments.GRAPPLING)
                .add(ModEnchantments.PIERCING)
                .add(ModEnchantments.REACH);
        tag(EnchantmentTags.ON_RANDOM_LOOT)
                .add(ModEnchantments.GRAPPLING)
                .add(ModEnchantments.PIERCING)
                .add(ModEnchantments.REACH);
        tag(EnchantmentTags.TRADEABLE)
                .add(ModEnchantments.GRAPPLING)
                .add(ModEnchantments.PIERCING)
                .add(ModEnchantments.REACH);
    }
}
