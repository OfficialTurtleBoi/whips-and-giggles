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
    public static void gatherClientData(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        PackOutput packOutput = dataGenerator.getPackOutput();
        dataGenerator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, event.getExistingFileHelper()));
        dataGenerator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput));
    }

    public static void gatherServerData(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        PackOutput packOutput = dataGenerator.getPackOutput();
        ModEnchantmentProvider enchantmentProvider = dataGenerator.addProvider(event.includeServer(),
                new ModEnchantmentProvider(packOutput, event.getLookupProvider()));
        var registryLookupFuture = enchantmentProvider.getRegistryProvider();
        dataGenerator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, registryLookupFuture));
        dataGenerator.addProvider(event.includeServer(), new ModItemTagsProvider(packOutput, registryLookupFuture, event.getExistingFileHelper()));
        dataGenerator.addProvider(event.includeServer(), new ModEnchantmentTagsProvider(packOutput, registryLookupFuture, event.getExistingFileHelper()));
    }
}
