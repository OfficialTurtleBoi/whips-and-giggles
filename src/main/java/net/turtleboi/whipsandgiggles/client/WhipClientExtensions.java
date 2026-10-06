package net.turtleboi.whipsandgiggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.component.ModDataComponents;
import net.turtleboi.whipsandgiggles.config.WhipConfig;
import net.turtleboi.whipsandgiggles.item.ModItems;
import org.joml.Vector3f;

public final class WhipClientExtensions implements IClientItemExtensions {
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new WhipClientExtensions(), ModItems.WHIP.get());
    }

    public static void registerTextureStageProperty() {
        ItemProperties.register(ModItems.WHIP.get(), WhipsAndGiggles.resourceLocation("stage"), (stack, level, entity, seed) -> {
            if (level == null || entity == null) {
                return 0;
            }
            InteractionHand hand = entity.getOffhandItem() == stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (entity.getMainHandItem().getItem() != stack.getItem()
                    && entity.getOffhandItem().getItem() == stack.getItem()) {
                hand = InteractionHand.OFF_HAND;
            }
            if (entity.getItemInHand(hand).getItem() != stack.getItem()) {
                return 0;
            }
            var animationFrame = WhipAnimationController.getAnimationFrame(entity, hand, entity.getItemInHand(hand),
                    WhipAnimationController.getRenderPartialTick(entity));
            return WhipAnimation.getTextureStageProperty(WhipAnimation.getTextureStage(
                    animationFrame.chargingWhip(), animationFrame.lashAgeTicks(), animationFrame.holdingGrapple()));
        });
    }

    public static final EnumProxy<HumanoidModel.ArmPose> WHIP_LASH_ARM_POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
                InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                ItemStack stack = entity.getItemInHand(hand);
                float partialTick = WhipAnimationController.getRenderPartialTick(entity);
                var animationFrame = WhipAnimationController.getAnimationFrame(entity, hand, stack, partialTick);
                float poseAgeTicks = animationFrame.poseAgeTicks();
                var armModelPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                float attackPower = animationFrame.attackPower();
                Vec3 anchorPosition = animationFrame.holdingGrapple() ? stack.get(ModDataComponents.GRAPPLE_ANCHOR_POSITION) : null;
                Vec3 anchorDirection = anchorPosition == null ? null : anchorPosition.subtract(entity.getEyePosition());
                float aimPitchRadians = anchorDirection == null ? model.head.xRot : calculateTargetPitchDegrees(anchorDirection) * Mth.DEG_TO_RAD;
                armModelPart.xRot = WhipAnimation.getThirdPersonPitchRadians(attackPower, poseAgeTicks, aimPitchRadians);
                float windupWeight = animationFrame.chargingWhip() ? attackPower
                        : attackPower * (1 - WhipAnimation.getExtensionProgress(poseAgeTicks));
                armModelPart.yRot = (anchorDirection == null ? model.head.yRot
                        : Mth.wrapDegrees(calculateTargetYawDegrees(anchorDirection) - entity.yBodyRot) * Mth.DEG_TO_RAD)
                        + (arm == HumanoidArm.RIGHT ? -1 : 1) * windupWeight * 0.25F;
                armModelPart.zRot = (arm == HumanoidArm.RIGHT ? 1 : -1)
                        * (windupWeight * 0.1F + WhipAnimation.getWristRollDegrees(poseAgeTicks) * Mth.DEG_TO_RAD);
                Vector3f aimTargetPosition = anchorDirection == null ? WhipAnimation.calculateThirdPersonAimTarget(model.head.xRot, model.head.yRot,
                        (float) WhipConfig.getAttackReach(stack, entity.level()))
                        : WhipAnimation.convertAnchorToModelSpace(anchorDirection.toVector3f(), entity.yBodyRot);
                Vector3f handPositionOffset = WhipAnimation.calculateThirdPersonHandOffset(aimPitchRadians, armModelPart.yRot, poseAgeTicks);
                armModelPart.x += handPositionOffset.x * 16;
                armModelPart.z += handPositionOffset.z * 16;
                Vector3f alignedArmRotation = WhipAnimation.calculateThirdPersonArmRotation(arm == HumanoidArm.LEFT,
                        new Vector3f(armModelPart.x / 16, (model.crouching ? 5.2F : 2) / 16, armModelPart.z / 16),
                        new Vector3f(armModelPart.xRot, armModelPart.yRot, armModelPart.zRot),
                        aimTargetPosition, poseAgeTicks, animationFrame.holdingGrapple());
                armModelPart.xRot = alignedArmRotation.x;
                armModelPart.yRot = alignedArmRotation.y;
                armModelPart.zRot = alignedArmRotation.z;
            });

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        var animationFrame = WhipAnimationController.getAnimationFrame(entity, hand, stack,
                WhipAnimationController.getRenderPartialTick(entity));
        return animationFrame.isAnimating() ? WHIP_LASH_ARM_POSE.getValue() : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                          float partialTick, float equipProgress, float swingProgress) {
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var animationFrame = WhipAnimationController.getAnimationFrame(player, hand, stack, partialTick);
        if (!animationFrame.isAnimating()) return false;
        float attackPower = animationFrame.attackPower();
        Vec3 anchorPosition = animationFrame.holdingGrapple() ? stack.get(ModDataComponents.GRAPPLE_ANCHOR_POSITION) : null;
        Vec3 anchorDirection = anchorPosition == null ? null : anchorPosition.subtract(player.getEyePosition(partialTick));
        Vector3f cameraAnchorPosition = anchorDirection == null ? null : anchorDirection.toVector3f()
                .rotateY((player.getViewYRot(partialTick) + 180) * Mth.DEG_TO_RAD)
                .rotateX(player.getViewXRot(partialTick) * Mth.DEG_TO_RAD);
        poseStack.mulPose(WhipAnimation.createFirstPersonTransform(arm == HumanoidArm.LEFT, attackPower, animationFrame.poseAgeTicks(), equipProgress,
                cameraAnchorPosition, animationFrame.contactDistanceBlocks(), (float) WhipConfig.getAttackReach(stack, player.level())));
        return true;
    }

    private static float calculateTargetYawDegrees(Vec3 anchorDirection) {
        return (float) Math.toDegrees(Math.atan2(-anchorDirection.x, anchorDirection.z));
    }

    private static float calculateTargetPitchDegrees(Vec3 anchorDirection) {
        return (float) -Math.toDegrees(Math.atan2(anchorDirection.y, Math.hypot(anchorDirection.x, anchorDirection.z)));
    }
}
