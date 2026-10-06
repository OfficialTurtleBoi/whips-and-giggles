package net.turtleboi.whipsandgiggles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.item.custom.WhipItem;

public record WhipAttackPayload(boolean useOffHand) implements CustomPacketPayload {
    public static final Type<WhipAttackPayload> TYPE = new Type<>(WhipsAndGiggles.resourceLocation("attack_whip"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhipAttackPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, WhipAttackPayload::useOffHand, WhipAttackPayload::new);

    public static void registerServerboundHandler(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, WhipAttackPayload::handleNormalAttackRequest);
    }

    private static void handleNormalAttackRequest(WhipAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            InteractionHand hand = payload.useOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (player.getItemInHand(hand).getItem() instanceof WhipItem whip) whip.tryNormalAttack(player, hand);
        });
    }

    @Override
    public Type<WhipAttackPayload> type() { return TYPE; }
}
