package net.turtleboi.whipsandgiggles.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.client.WhipClientExtensions;
import net.turtleboi.whipsandgiggles.datagen.ModDatagen;

@EventBusSubscriber(modid = WhipsAndGiggles.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientBusEvents {
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        WhipClientExtensions.registerItemExtensions(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(WhipClientExtensions::registerTextureStageProperty);
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent event) {
        ModDatagen.gatherClientData(event);
    }
}
