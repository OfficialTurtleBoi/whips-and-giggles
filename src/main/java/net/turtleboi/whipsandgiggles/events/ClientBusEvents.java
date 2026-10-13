package net.turtleboi.whipsandgiggles.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.client.WhipClientExtensions;
import net.turtleboi.whipsandgiggles.datagen.ModDatagen;

@EventBusSubscriber(modid = WhipsAndGiggles.MOD_ID, value = Dist.CLIENT)
public final class ClientBusEvents {
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        WhipClientExtensions.registerItemExtensions(event);
    }

    @SubscribeEvent
    public static void registerTextureStageProperty(RegisterRangeSelectItemModelPropertyEvent event) {
        WhipClientExtensions.registerTextureStageProperty(event);
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        WhipClientExtensions.registerRenderStateModifiers(event);
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        ModDatagen.gatherClientData(event);
    }
}
