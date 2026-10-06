package net.turtleboi.whipsandgiggles.component;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WhipsAndGiggles.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LASH_START_GAME_TICK =
            DATA_COMPONENT_TYPES.register("release_tick", () -> DataComponentType.<Long>builder()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> LASH_ATTACK_POWER =
            DATA_COMPONENT_TYPES.register("release_power", () -> DataComponentType.<Float>builder()
                    .networkSynchronized(ByteBufCodecs.FLOAT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> LASH_CONTACT_DISTANCE_BLOCKS =
            DATA_COMPONENT_TYPES.register("release_distance", () -> DataComponentType.<Float>builder()
                    .networkSynchronized(ByteBufCodecs.FLOAT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Vec3>> GRAPPLE_ANCHOR_POSITION =
            DATA_COMPONENT_TYPES.register("grapple_anchor", () -> DataComponentType.<Vec3>builder()
                    .networkSynchronized(StreamCodec.composite(
                            ByteBufCodecs.DOUBLE, vector -> vector.x,
                            ByteBufCodecs.DOUBLE, vector -> vector.y,
                            ByteBufCodecs.DOUBLE, vector -> vector.z, Vec3::new)).build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
