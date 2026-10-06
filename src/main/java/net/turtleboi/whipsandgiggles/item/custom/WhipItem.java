package net.turtleboi.whipsandgiggles.item.custom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.turtleboi.whipsandgiggles.component.ModDataComponents;
import net.turtleboi.whipsandgiggles.config.WhipConfig;
import net.turtleboi.whipsandgiggles.enchantment.ModEnchantments;

public class WhipItem extends Item {
    public static final int FULL_CHARGE_DURATION_TICKS = 8;
    public static final float NORMAL_ATTACK_POWER_FRACTION = 0.5F;
    public static final int STRIKE_IMPACT_DELAY_TICKS = 3;
    public static final int RETRACTION_START_TICK = 7;
    public static final int LASH_DURATION_TICKS = 13;
    public static final double DEFAULT_UNSCALED_REACH_BLOCKS = Math.hypot(2, 2);
    public static final double DEFAULT_TIP_REACH_PADDING_BLOCKS = 0.5;
    public static final float FIRST_PERSON_EXTENSION_SCALE_MULTIPLIER = 1.5F;
    public static final int MAX_REACH_ENCHANTMENT_LEVEL = 3;
    public static final double REACH_BLOCKS_PER_ENCHANTMENT_LEVEL = 3;

    private static final Map<UUID, ScheduledStrike> PENDING_STRIKES_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, ActiveGrapple> ACTIVE_GRAPPLES_BY_PLAYER = new HashMap<>();

    public WhipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(ModDataComponents.GRAPPLE_ANCHOR_POSITION)) {
            detachGrapple(player);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player.isShiftKeyDown()) detachGrapple(player);
        stack.remove(ModDataComponents.LASH_START_GAME_TICK);
        stack.remove(ModDataComponents.LASH_ATTACK_POWER);
        stack.remove(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity itemUser) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity itemUser, int timeLeft) {
        if (!(itemUser instanceof Player player) || player.isSpectator() || !player.isAlive()
                || player.getCooldowns().isOnCooldown(this)) return;
        int chargeDurationTicks = getUseDuration(stack, itemUser) - timeLeft;
        if (chargeDurationTicks < 2) return;
        beginLash(player, stack, player.getUsedItemHand(), calculateChargePower(chargeDurationTicks),
                chargeDurationTicks >= FULL_CHARGE_DURATION_TICKS);
    }

    public boolean tryNormalAttack(Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive() || player.isUsingItem()
                || player.getCooldowns().isOnCooldown(this) || player.getItemInHand(hand).getItem() != this) return false;
        beginLash(player, player.getItemInHand(hand), hand, NORMAL_ATTACK_POWER_FRACTION, false);
        return true;
    }

    private void beginLash(Player player, ItemStack stack, InteractionHand hand, float attackPower, boolean allowGrappling) {
        detachGrapple(player);
        stack.set(ModDataComponents.LASH_START_GAME_TICK, player.level().getGameTime());
        stack.set(ModDataComponents.LASH_ATTACK_POWER, attackPower);
        stack.set(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS,
                findContactDistance(player, stack, player.getEyePosition(), player.getLookAngle()));
        player.getCooldowns().addCooldown(this, LASH_DURATION_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            scheduleStrike(serverPlayer, stack, hand, attackPower, allowGrappling);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity itemHolder, int slot, boolean selected) {
        Long lashStartGameTick = stack.get(ModDataComponents.LASH_START_GAME_TICK);
        if (!level.isClientSide && !stack.has(ModDataComponents.GRAPPLE_ANCHOR_POSITION)
                && lashStartGameTick != null && level.getGameTime() - lashStartGameTick >= LASH_DURATION_TICKS) {
            stack.remove(ModDataComponents.LASH_START_GAME_TICK);
            stack.remove(ModDataComponents.LASH_ATTACK_POWER);
            stack.remove(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairIngredient) {
        return repairIngredient.is(Items.STRING);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    public static float calculateChargePower(int chargeTicks) {
        return Math.clamp(chargeTicks / (float) FULL_CHARGE_DURATION_TICKS, 0.0F, 1.0F);
    }

    public static double calculateAttackDamage(float attackPower) {
        return WhipConfig.getFullChargeDamage() * (0.35 + 0.65 * attackPower);
    }

    public static double calculateAttackSpeed() {
        return 20.0 / LASH_DURATION_TICKS;
    }

    public static double calculateEnchantmentReachBonus(int enchantmentLevel) {
        return REACH_BLOCKS_PER_ENCHANTMENT_LEVEL * Math.clamp(enchantmentLevel, 0, MAX_REACH_ENCHANTMENT_LEVEL);
    }

    public static double calculateScaledReach(double unscaledReachBlocks, double tipReachPaddingBlocks) {
        return Math.round((unscaledReachBlocks * FIRST_PERSON_EXTENSION_SCALE_MULTIPLIER + tipReachPaddingBlocks) * 4) / 4.0;
    }

    public static float calculateForwardTravelMultiplier(float contactDistanceBlocks, float attackReachBlocks) {
        if (contactDistanceBlocks < 0) return 1;
        return 0.35F + 0.65F * Math.clamp(contactDistanceBlocks / attackReachBlocks, 0, 1);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                        calculateAttackDamage(NORMAL_ATTACK_POWER_FRACTION) - 1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                        calculateAttackSpeed() - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(AttributeUtil.BASE_ENTITY_REACH_ID,
                        WhipConfig.getAttackReach(stack) - 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static void scheduleStrike(ServerPlayer player, ItemStack stack, InteractionHand hand,
                                       float attackPower, boolean allowGrappling) {
        detachGrapple(player);
        PENDING_STRIKES_BY_PLAYER.put(player.getUUID(), new ScheduledStrike(stack, hand, attackPower,
                player.level().getGameTime() + STRIKE_IMPACT_DELAY_TICKS,
                player.serverLevel(), allowGrappling));
    }

    public static void tickPlayerWhipActions(ServerPlayer player) {
        ScheduledStrike scheduledStrike = PENDING_STRIKES_BY_PLAYER.get(player.getUUID());
        if (scheduledStrike != null && player.level().getGameTime() >= scheduledStrike.impactGameTick()) {
            PENDING_STRIKES_BY_PLAYER.remove(player.getUUID());
            if (isStillHoldingWhip(player, scheduledStrike.stack(), scheduledStrike.hand()) && player.serverLevel() == scheduledStrike.level()) {
                applyStrike(player, scheduledStrike.stack(), scheduledStrike.hand(), scheduledStrike.attackPower(),
                        player.getEyePosition(), player.getLookAngle(), scheduledStrike.allowGrappling());
            }
        }
        ActiveGrapple activeGrapple = ACTIVE_GRAPPLES_BY_PLAYER.get(player.getUUID());
        if (activeGrapple == null) return;
        Vec3 anchorOffset = activeGrapple.anchorPosition().subtract(player.getEyePosition());
        BlockHitResult obstructionHit = clipStrikeAgainstBlocks(player.serverLevel(), player, player.getEyePosition(), activeGrapple.anchorPosition());
        if (!isStillHoldingWhip(player, activeGrapple.stack(), activeGrapple.hand()) || player.isShiftKeyDown()
                || player.serverLevel() != activeGrapple.level()
                || anchorOffset.length() < 0.6 || anchorOffset.length() > WhipConfig.getAttackReach(activeGrapple.stack(), player.level()) + 1
                || player.level().getBlockState(activeGrapple.anchorBlockPos()).isAir()
                || (obstructionHit.getType() == HitResult.Type.BLOCK && !obstructionHit.getBlockPos().equals(activeGrapple.anchorBlockPos()))) {
            detachGrapple(player);
            return;
        }
        Vec3 grappleVelocity = player.getDeltaMovement().scale(0.85).add(anchorOffset.normalize().scale(0.22));
        if (grappleVelocity.lengthSqr() > 1.0) grappleVelocity = grappleVelocity.normalize();
        player.setDeltaMovement(grappleVelocity);
        player.fallDistance = 0;
        synchronizeEntityMotion(player);
    }

    private static boolean isStillHoldingWhip(Player player, ItemStack stack, InteractionHand hand) {
        return player.isAlive() && !player.isSpectator() && !stack.isEmpty() && player.getItemInHand(hand) == stack;
    }

    private static void applyStrike(ServerPlayer player, ItemStack stack, InteractionHand hand,
                              float attackPower, Vec3 origin, Vec3 direction, boolean allowGrappling) {
        ServerLevel level = player.serverLevel();
        int grapplingEnchantmentLevel = ModEnchantments.getEnchantmentLevel(stack, level, ModEnchantments.GRAPPLING);
        int effectiveGrapplingLevel = allowGrappling ? grapplingEnchantmentLevel : 0;
        int piercingEnchantmentLevel = grapplingEnchantmentLevel > 0 ? 0 : ModEnchantments.getEnchantmentLevel(stack, level, ModEnchantments.PIERCING);
        double attackReachBlocks = WhipConfig.getAttackReach(stack, level);
        Vec3 strikeEnd = origin.add(direction.normalize().scale(attackReachBlocks));
        BlockHitResult blockHit = clipStrikeAgainstBlocks(level, player, origin, strikeEnd);
        Vec3 unobstructedStrikeEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : strikeEnd;
        List<StrikeTarget> strikeTargets = findStrikeTargets(level, player, origin, unobstructedStrikeEnd);
        int targetCount = Math.min(strikeTargets.size(), 1 + piercingEnchantmentLevel);
        stack.set(ModDataComponents.LASH_CONTACT_DISTANCE_BLOCKS, (float) (targetCount > 0
                ? origin.distanceTo(strikeTargets.get(targetCount - 1).hitPosition())
                : blockHit.getType() == HitResult.Type.BLOCK ? origin.distanceTo(unobstructedStrikeEnd) : -1));
        for (int i = 0; i < targetCount; i++) {
            StrikeTarget strikeTarget = strikeTargets.get(i);
            LivingEntity hitEntity = strikeTarget.hitEntity();
            if (hitEntity instanceof Player victim && (!level.getServer().isPvpAllowed() || !player.canHarmPlayer(victim))) continue;
            if (!CommonHooks.onPlayerAttackTarget(player, hitEntity)) continue;
            DamageSource damageSource = player.damageSources().playerAttack(player);
            float attackDamage = EnchantmentHelper.modifyDamage(level, stack, hitEntity, damageSource,
                    (float) calculateAttackDamage(attackPower));
            if (hitEntity.hurt(damageSource, attackDamage)) {
                Vec3 hitPosition = strikeTarget.hitPosition();
                level.sendParticles(ParticleTypes.CRIT, hitPosition.x, hitPosition.y, hitPosition.z, 6, 0.12, 0.12, 0.12, 0.08);
                EnchantmentHelper.doPostAttackEffectsWithItemSource(level, hitEntity, damageSource, stack);
                player.setLastHurtMob(hitEntity);
                float knockback = EnchantmentHelper.modifyKnockback(level, stack, hitEntity, damageSource, 0);
                if (knockback > 0) hitEntity.knockback(knockback * 0.5, -direction.x, -direction.z);
                if (effectiveGrapplingLevel > 0) {
                    Vec3 pullVelocity = player.position().subtract(hitEntity.position()).normalize().scale(0.65 + 0.35 * attackPower);
                    hitEntity.setDeltaMovement(pullVelocity.x, Math.max(hitEntity.getDeltaMovement().y, 0.2), pullVelocity.z);
                }
                synchronizeEntityMotion(hitEntity);
            }
        }
        if (effectiveGrapplingLevel > 0 && strikeTargets.isEmpty() && blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3 anchorPosition = blockHit.getLocation().add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.05));
            ACTIVE_GRAPPLES_BY_PLAYER.put(player.getUUID(), new ActiveGrapple(stack, hand, anchorPosition, blockHit.getBlockPos(), level));
            stack.set(ModDataComponents.GRAPPLE_ANCHOR_POSITION, anchorPosition);
        }
        if (strikeTargets.isEmpty() && blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPosition = blockHit.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockHit.getBlockPos())),
                    hitPosition.x, hitPosition.y, hitPosition.z, 8, 0.1, 0.1, 0.1, 0.05);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.PLAYERS, 0.6F, 0.8F);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public static float findContactDistance(Player player, ItemStack stack, Vec3 origin, Vec3 direction) {
        Level level = player.level();
        Vec3 strikeEnd = origin.add(direction.normalize().scale(WhipConfig.getAttackReach(stack, level)));
        BlockHitResult blockHit = clipStrikeAgainstBlocks(level, player, origin, strikeEnd);
        Vec3 unobstructedStrikeEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : strikeEnd;
        List<StrikeTarget> strikeTargets = findStrikeTargets(level, player, origin, unobstructedStrikeEnd);
        int piercingEnchantmentLevel = ModEnchantments.getEnchantmentLevel(stack, level, ModEnchantments.GRAPPLING) > 0 ? 0
                : ModEnchantments.getEnchantmentLevel(stack, level, ModEnchantments.PIERCING);
        if (!strikeTargets.isEmpty()) {
            int lastTargetIndex = Math.min(strikeTargets.size(), 1 + piercingEnchantmentLevel) - 1;
            return (float) origin.distanceTo(strikeTargets.get(lastTargetIndex).hitPosition());
        }
        return blockHit.getType() == HitResult.Type.BLOCK ? (float) origin.distanceTo(unobstructedStrikeEnd) : -1;
    }

    private static List<StrikeTarget> findStrikeTargets(Level level, Player attackingPlayer, Vec3 origin, Vec3 strikeEnd) {
        List<StrikeTarget> strikeTargets = new ArrayList<>();
        double strikeLengthSquared = origin.distanceToSqr(strikeEnd);
        for (LivingEntity hitEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, strikeEnd).inflate(0.3),
                hitEntity -> hitEntity != attackingPlayer && hitEntity.isAlive() && !hitEntity.isSpectator() && hitEntity.isPickable()
                        && !hitEntity.isPassengerOfSameVehicle(attackingPlayer))) {
            AABB targetBounds = hitEntity.getBoundingBox().inflate(0.15);
            Vec3 hitPosition = targetBounds.contains(origin) ? origin : targetBounds.clip(origin, strikeEnd).orElse(null);
            if (hitPosition != null && origin.distanceToSqr(hitPosition) < strikeLengthSquared) strikeTargets.add(new StrikeTarget(hitEntity, hitPosition));
        }
        strikeTargets.sort(Comparator.comparingDouble(strikeTarget -> origin.distanceToSqr(strikeTarget.hitPosition())));
        return strikeTargets;
    }

    private static BlockHitResult clipStrikeAgainstBlocks(Level level, Player attackingPlayer, Vec3 origin, Vec3 strikeEnd) {
        return level.clip(new ClipContext(origin, strikeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attackingPlayer));
    }

    private static void synchronizeEntityMotion(LivingEntity hitEntity) {
        hitEntity.hurtMarked = true;
        if (hitEntity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    public static void detachGrapple(Player player) {
        if (!player.level().isClientSide) {
            ActiveGrapple activeGrapple = ACTIVE_GRAPPLES_BY_PLAYER.remove(player.getUUID());
            if (activeGrapple != null) beginGrappleRetraction(player, activeGrapple.stack());
        } else {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.has(ModDataComponents.GRAPPLE_ANCHOR_POSITION)) beginGrappleRetraction(player, stack);
            }
        }
    }

    private static void beginGrappleRetraction(Player player, ItemStack stack) {
        stack.remove(ModDataComponents.GRAPPLE_ANCHOR_POSITION);
        stack.set(ModDataComponents.LASH_START_GAME_TICK, player.level().getGameTime() - RETRACTION_START_TICK);
        player.getCooldowns().addCooldown(stack.getItem(), LASH_DURATION_TICKS - RETRACTION_START_TICK);
    }

    public static void clearPlayerWhipActions(Player player) {
        PENDING_STRIKES_BY_PLAYER.remove(player.getUUID());
        detachGrapple(player);
    }

    public static void clearAllWhipActions() {
        PENDING_STRIKES_BY_PLAYER.clear();
        ACTIVE_GRAPPLES_BY_PLAYER.clear();
    }

    private record StrikeTarget(LivingEntity hitEntity, Vec3 hitPosition) {
    }

    private record ScheduledStrike(ItemStack stack, InteractionHand hand, float attackPower, long impactGameTick,
                                 ServerLevel level, boolean allowGrappling) {
    }

    private record ActiveGrapple(ItemStack stack, InteractionHand hand, Vec3 anchorPosition, BlockPos anchorBlockPos, ServerLevel level) {
    }

    public static void updateAttackDamageTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof WhipItem)) {
            return;
        }
        String damageRangeText = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(WhipItem.calculateAttackDamage(WhipItem.NORMAL_ATTACK_POWER_FRACTION))
                + "-" + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(WhipItem.calculateAttackDamage(1));
        event.getToolTip().replaceAll(line -> replaceAttackDamageValue(line, damageRangeText));
    }

    private static Component replaceAttackDamageValue(Component component, String damageRangeText) {
        MutableComponent updatedTooltipLine = component.copy();
        if (component.getContents() instanceof TranslatableContents translatedLine
                && translatedLine.getKey().equals("attribute.modifier.equals.0") && translatedLine.getArgs().length == 2
                && translatedLine.getArgs()[1] instanceof Component attributeName
                && attributeName.getContents() instanceof TranslatableContents translatedAttributeName
                && translatedAttributeName.getKey().equals(Attributes.ATTACK_DAMAGE.value().getDescriptionId())) {
            updatedTooltipLine = Component.translatable(translatedLine.getKey(), damageRangeText, attributeName).withStyle(component.getStyle());
        }
        updatedTooltipLine.getSiblings().clear();
        for (Component sibling : component.getSiblings()) {
            updatedTooltipLine.append(replaceAttackDamageValue(sibling, damageRangeText));
        }
        return updatedTooltipLine;
    }
}
