package net.turtleboi.whipsandgiggles.client;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.turtleboi.whipsandgiggles.component.ModDataComponents;
import net.turtleboi.whipsandgiggles.item.custom.WhipItem;
import net.turtleboi.whipsandgiggles.network.WhipAttackPayload;

public final class WhipAnimationController {
    private static final Map<LivingEntity, EnumMap<InteractionHand, AnimationState>> ANIMATION_STATES_BY_ENTITY = new WeakHashMap<>();

    public record AnimationFrame(float lashAgeTicks, float attackPower, float contactDistanceBlocks,
                                 boolean chargingWhip, boolean holdingGrapple) {
        public boolean isAnimating() {
            return chargingWhip || lashAgeTicks >= 0 && lashAgeTicks < WhipItem.LASH_DURATION_TICKS;
        }

        public float poseAgeTicks() {
            return chargingWhip ? -1 : lashAgeTicks;
        }
    }

    public static void beginPredictedLash(LivingEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        AnimationState animationState = new AnimationState();
        animationState.updateLashAge(entity.tickCount + getRenderPartialTick(entity), entity.level().getGameTime(),
                stack.get(ModDataComponents.LASH_START_GAME_TICK), false, false, true,
                stack.getOrDefault(ModDataComponents.LASH_ATTACK_POWER, 1F),
                stack.getOrDefault(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS, -1F));
        ANIMATION_STATES_BY_ENTITY.computeIfAbsent(entity, ignored -> new EnumMap<>(InteractionHand.class))
                .put(hand, animationState);
    }

    public static float getRenderPartialTick(LivingEntity entity) {
        return Minecraft.getInstance().getTimer()
                .getGameTimeDeltaPartialTick(!entity.level().tickRateManager().isEntityFrozen(entity));
    }

    public static AnimationFrame getAnimationFrame(LivingEntity entity, InteractionHand hand, ItemStack stack, float partialTick) {
        boolean chargingWhip = entity.isUsingItem() && entity.getUsedItemHand() == hand;
        boolean holdingGrapple = stack.has(ModDataComponents.GRAPPLE_ANCHOR_POSITION);
        AnimationState animationState = ANIMATION_STATES_BY_ENTITY.computeIfAbsent(entity, ignored -> new EnumMap<>(InteractionHand.class))
                .computeIfAbsent(hand, ignored -> new AnimationState());
        float lashAgeTicks = animationState.updateLashAge(entity.tickCount + partialTick, entity.level().getGameTime(),
                stack.get(ModDataComponents.LASH_START_GAME_TICK), chargingWhip, holdingGrapple,
                entity == Minecraft.getInstance().player, stack.getOrDefault(ModDataComponents.LASH_ATTACK_POWER, 1F),
                stack.getOrDefault(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS, -1F));
        float attackPower = chargingWhip ? Math.clamp((entity.getTicksUsingItem() + partialTick) / WhipItem.FULL_CHARGE_DURATION_TICKS, 0, 1)
                : animationState.getAttackPower();
        return new AnimationFrame(lashAgeTicks, attackPower, animationState.getContactDistanceBlocks(), chargingWhip,
                holdingGrapple && lashAgeTicks == WhipItem.STRIKE_IMPACT_DELAY_TICKS);
    }

    public static void handleNormalAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!event.isAttack() || player == null || minecraft.screen != null) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof WhipItem
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(player.getItemInHand(hand).getItem() instanceof WhipItem whipItem)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (whipItem.tryNormalAttack(player, hand)) {
            beginPredictedLash(player, hand);
            PacketDistributor.sendToServer(new WhipAttackPayload(hand == InteractionHand.OFF_HAND));
        }
    }

    private static final class AnimationState {
        private Long lastObservedLashStartTick;
        private float animationStartClientTick;
        private float lashAgeTicks = -1;
        private boolean animationRunning;
        private boolean holdingGrapple;
        private boolean grappleDetached;
        private float attackPower = 1;
        private float contactDistanceBlocks = -1;

        private float updateLashAge(float clientAnimationTick, long worldGameTick, Long syncedLashStartTick, boolean chargingWhip, boolean grappleAnchorPresent,
                            boolean isLocalPlayer, float syncedAttackPower, float syncedContactDistanceBlocks) {
            if (chargingWhip) {
                lastObservedLashStartTick = null;
                lashAgeTicks = -1;
                animationRunning = holdingGrapple = grappleDetached = false;
                return lashAgeTicks;
            }
            if (grappleAnchorPresent && !grappleDetached) {
                if (!animationRunning && !holdingGrapple) captureLashParameters(syncedAttackPower, syncedContactDistanceBlocks);
                lastObservedLashStartTick = syncedLashStartTick;
                holdingGrapple = true;
                animationRunning = true;
                lashAgeTicks = WhipItem.STRIKE_IMPACT_DELAY_TICKS;
                return lashAgeTicks;
            }
            if (holdingGrapple) {
                holdingGrapple = false;
                grappleDetached = true;
                lastObservedLashStartTick = syncedLashStartTick;
                lashAgeTicks = WhipItem.RETRACTION_START_TICK;
                animationStartClientTick = clientAnimationTick - lashAgeTicks;
                return lashAgeTicks;
            }
            if (syncedLashStartTick != null && (lastObservedLashStartTick == null || !syncedLashStartTick.equals(lastObservedLashStartTick))) {
                boolean newLash = lastObservedLashStartTick == null || !isLocalPlayer && !animationRunning
                        && Math.abs(syncedLashStartTick - lastObservedLashStartTick) >= WhipItem.LASH_DURATION_TICKS;
                if (newLash) {
                    lashAgeTicks = isLocalPlayer ? 0 : Math.clamp(worldGameTick - syncedLashStartTick, 0, WhipItem.LASH_DURATION_TICKS);
                    animationStartClientTick = clientAnimationTick - lashAgeTicks;
                    animationRunning = lashAgeTicks < WhipItem.LASH_DURATION_TICKS;
                    grappleDetached = false;
                    captureLashParameters(syncedAttackPower, syncedContactDistanceBlocks);
                }
                lastObservedLashStartTick = syncedLashStartTick;
            }
            if (animationRunning) {
                lashAgeTicks = Math.max(lashAgeTicks, clientAnimationTick - animationStartClientTick);
                if (lashAgeTicks >= WhipItem.LASH_DURATION_TICKS) animationRunning = false;
            }
            return lashAgeTicks;
        }

        private void captureLashParameters(float syncedAttackPower, float syncedContactDistanceBlocks) {
            attackPower = syncedAttackPower;
            contactDistanceBlocks = syncedContactDistanceBlocks;
        }

        private float getAttackPower() { return attackPower; }
        private float getContactDistanceBlocks() { return contactDistanceBlocks; }
    }
}
