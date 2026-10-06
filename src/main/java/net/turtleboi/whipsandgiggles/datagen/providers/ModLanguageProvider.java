package net.turtleboi.whipsandgiggles.datagen.providers;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.item.ModItems;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, WhipsAndGiggles.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ModItems.WHIP.get(), "Whip");
        add("enchantment.whipsandgiggles.grappling", "Grappling");
        add("enchantment.whipsandgiggles.piercing", "Piercing");
        add("enchantment.whipsandgiggles.reach", "Reach");
        add("enchantment.whipsandgiggles.grappling.desc", "Fully charge right-click to reel a struck entity or grapple a block. Sneak to detach. Incompatible with Piercing.");
        add("enchantment.whipsandgiggles.piercing.desc", "Strikes one additional entity per level along the lash. Incompatible with Grappling.");
        add("enchantment.whipsandgiggles.reach.desc", "Adds 3 blocks of lash and grappling reach per level. Compatible with Grappling and Piercing.");
    }
}
