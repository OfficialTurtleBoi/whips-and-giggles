package net.turtleboi.whipsandgiggles.datagen.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.turtleboi.whipsandgiggles.item.ModItems;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.WHIP.get())
                .pattern(" TT")
                .pattern("S T")
                .pattern(" T ")
                .define('T', Items.STRING)
                .define('S', Items.STICK)
                .unlockedBy("has_string", has(Items.STRING))
                .save(output);
    }
}
