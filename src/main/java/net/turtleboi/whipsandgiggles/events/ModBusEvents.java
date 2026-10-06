package net.turtleboi.whipsandgiggles.events;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.datagen.ModDatagen;
import net.turtleboi.whipsandgiggles.item.ModItems;
import net.turtleboi.whipsandgiggles.network.WhipAttackPayload;

@EventBusSubscriber(modid = WhipsAndGiggles.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        WhipAttackPayload.registerServerboundHandler(event);
    }

    @SubscribeEvent
    public static void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.WHIP);
        }
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent event) {
        ModDatagen.gatherServerData(event);
    }
}
