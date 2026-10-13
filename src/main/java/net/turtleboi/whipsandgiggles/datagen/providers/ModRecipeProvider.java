package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.turtleboi.whipsandgiggles.item.ModItems;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput output) {
        super(registryLookup, output);
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.COMBAT, ModItems.WHIP.get())
                .pattern(" TT")
                .pattern("S T")
                .pattern(" T ")
                .define('T', Items.STRING)
                .define('S', Items.STICK)
                .unlockedBy("has_string", has(Items.STRING))
                .save(output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
            super(output, registryLookupFuture);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput output) {
            return new ModRecipeProvider(registryLookup, output);
        }

        @Override
        public String getName() {
            return "Whips & Giggles recipes";
        }
    }
}
