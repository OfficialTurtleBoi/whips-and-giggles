package net.turtleboi.whipsandgiggles.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.turtleboi.whipsandgiggles.datagen.providers.ModEnchantmentProvider;
import net.turtleboi.whipsandgiggles.datagen.providers.ModEnchantmentTagsProvider;
import net.turtleboi.whipsandgiggles.datagen.providers.ModItemModelProvider;
import net.turtleboi.whipsandgiggles.datagen.providers.ModItemTagsProvider;
import net.turtleboi.whipsandgiggles.datagen.providers.ModLanguageProvider;
import net.turtleboi.whipsandgiggles.datagen.providers.ModRecipeProvider;

public final class ModDatagen {
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator dataGenerator = event.getGenerator();
        PackOutput packOutput = dataGenerator.getPackOutput();
        dataGenerator.addProvider(true, new ModItemModelProvider(packOutput));
        dataGenerator.addProvider(true, new ModLanguageProvider(packOutput));
    }

    public static void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator dataGenerator = event.getGenerator();
        PackOutput packOutput = dataGenerator.getPackOutput();
        ModEnchantmentProvider enchantmentProvider = dataGenerator.addProvider(true,
                new ModEnchantmentProvider(packOutput, event.getLookupProvider()));
        var registryLookupFuture = enchantmentProvider.getRegistryProvider();
        dataGenerator.addProvider(true, new ModRecipeProvider.Runner(packOutput, registryLookupFuture));
        dataGenerator.addProvider(true, new ModItemTagsProvider(packOutput, registryLookupFuture));
        dataGenerator.addProvider(true, new ModEnchantmentTagsProvider(packOutput, registryLookupFuture));
    }
}
