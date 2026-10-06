package net.turtleboi.whipsandgiggles.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;
import net.turtleboi.whipsandgiggles.item.custom.WhipItem;

public final class WhipConfig {
    public static final ModConfigSpec SERVER_CONFIG_SPEC;
    public static final ModConfigSpec.DoubleValue FULL_CHARGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue UNSCALED_REACH_BLOCKS;
    public static final ModConfigSpec.DoubleValue TIP_REACH_PADDING_BLOCKS;

    static {
        ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
        FULL_CHARGE_DAMAGE = configBuilder.comment("Damage at full charge, before enchantments (2 points = 1 heart).")
                .defineInRange("fullChargeDamage", 6.0, 1.0, 100.0);
        UNSCALED_REACH_BLOCKS = configBuilder.comment("Base reach for strikes and grappling, multiplied by the visual extension scale (1.5).",
                        "Effective reach = whipReach * 1.5 + tipReachPadding, rounded to the nearest 0.25 block; default is 4.75 blocks.")
                .defineInRange("whipReach", WhipItem.DEFAULT_UNSCALED_REACH_BLOCKS, 1.0, 16.0);
        TIP_REACH_PADDING_BLOCKS = configBuilder.comment("Extra reach beyond the scaled whip tip, in blocks.")
                .defineInRange("tipReachPadding", WhipItem.DEFAULT_TIP_REACH_PADDING_BLOCKS, 0, 4.0);
        SERVER_CONFIG_SPEC = configBuilder.build();
    }

    public static double getAttackReach() {
        return WhipItem.calculateScaledReach(SERVER_CONFIG_SPEC.isLoaded() ? UNSCALED_REACH_BLOCKS.get() : UNSCALED_REACH_BLOCKS.getDefault(),
                SERVER_CONFIG_SPEC.isLoaded() ? TIP_REACH_PADDING_BLOCKS.get() : TIP_REACH_PADDING_BLOCKS.getDefault());
    }

    public static double getFullChargeDamage() {
        return SERVER_CONFIG_SPEC.isLoaded() ? FULL_CHARGE_DAMAGE.get() : FULL_CHARGE_DAMAGE.getDefault();
    }

    public static double getAttackReach(ItemStack stack) {
        return getAttackReach() + WhipItem.calculateEnchantmentReachBonus(ModEnchantments.getEnchantmentLevel(stack, ModEnchantments.REACH));
    }

    public static double getAttackReach(ItemStack stack, Level level) {
        return getAttackReach() + WhipItem.calculateEnchantmentReachBonus(ModEnchantments.getEnchantmentLevel(stack, level, ModEnchantments.REACH));
    }
}
