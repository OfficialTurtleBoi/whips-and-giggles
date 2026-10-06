package net.turtleboi.whipsandgiggles.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.item.custom.WhipItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhipsAndGiggles.MOD_ID);
    public static final DeferredItem<WhipItem> WHIP = ITEMS.registerItem("whip", WhipItem::new,
            new Item.Properties().durability(384));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
