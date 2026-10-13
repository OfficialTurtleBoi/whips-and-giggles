package net.turtleboi.whipsandgiggles;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.turtleboi.whipsandgiggles.component.ModDataComponents;
import net.turtleboi.whipsandgiggles.config.WhipConfig;
import net.turtleboi.whipsandgiggles.item.ModItems;

@Mod(WhipsAndGiggles.MOD_ID)
public class WhipsAndGiggles {
    public static final String MOD_ID = "whipsandgiggles";

    public WhipsAndGiggles(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, WhipConfig.SERVER_CONFIG_SPEC);
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
