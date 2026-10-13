package net.turtleboi.whipsandgiggles.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;

public final class ModEnchantments {
    public static final TagKey<Item> WHIP_ENCHANTABLE_ITEMS =
            TagKey.create(Registries.ITEM, WhipsAndGiggles.identifier("enchantable/whip"));
    public static final TagKey<Enchantment> EXCLUSIVE_WHIP_ENCHANTMENTS =
            TagKey.create(Registries.ENCHANTMENT, WhipsAndGiggles.identifier("exclusive_set/whip_utility"));
    public static final ResourceKey<Enchantment> GRAPPLING = createEnchantmentKey("grappling");
    public static final ResourceKey<Enchantment> PIERCING = createEnchantmentKey("piercing");
    public static final ResourceKey<Enchantment> REACH = createEnchantmentKey("reach");

    private static ResourceKey<Enchantment> createEnchantmentKey(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, WhipsAndGiggles.identifier(path));
    }

    public static int getEnchantmentLevel(ItemStack stack, Level level, ResourceKey<Enchantment> enchantmentKey) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentKey)
                .map(stack::getEnchantmentLevel).orElse(0);
    }

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        for (var enchantmentHolder : stack.getTagEnchantments().keySet()) {
            if (enchantmentHolder.is(enchantmentKey)) return stack.getEnchantmentLevel(enchantmentHolder);
        }
        return 0;
    }
}
