package net.turtleboi.whipsandgiggles.client;

import static net.turtleboi.whipsandgiggles.item.custom.WhipItem.RETRACTION_START_TICK;
import static net.turtleboi.whipsandgiggles.item.custom.WhipItem.STRIKE_IMPACT_DELAY_TICKS;
import static net.turtleboi.whipsandgiggles.item.custom.WhipItem.LASH_DURATION_TICKS;

import net.turtleboi.whipsandgiggles.item.custom.WhipItem;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WhipAnimation {
    public static final int FULL_EXTENSION_TEXTURE_STAGE = 5;
    public static final float FIRST_PERSON_MODEL_SCALE_MULTIPLIER = 1.35F;
    public static final float THIRD_PERSON_MODEL_SCALE_MULTIPLIER = 1.1F;
    public static final float FIRST_PERSON_TEXTURE_PLANE_SCALE = 1.36F;
    public static final float THIRD_PERSON_TEXTURE_PLANE_SCALE = 1.7F;

    public static int getLashTextureStage(float lashAgeTicks) {
        if (lashAgeTicks < 0 || lashAgeTicks >= LASH_DURATION_TICKS) return 1;
        if (lashAgeTicks < 1) return 2;
        if (lashAgeTicks < 2) return 3;
        if (lashAgeTicks < STRIKE_IMPACT_DELAY_TICKS) return 4;
        if (lashAgeTicks < RETRACTION_START_TICK) return 5;
        if (lashAgeTicks < RETRACTION_START_TICK + 1) return 4;
        if (lashAgeTicks < RETRACTION_START_TICK + 2) return 3;
        if (lashAgeTicks < RETRACTION_START_TICK + 3) return 2;
        return 1;
    }

    public static int getTextureStage(boolean chargingWhip, float lashAgeTicks, boolean holdingGrapple) {
        return holdingGrapple ? FULL_EXTENSION_TEXTURE_STAGE : chargingWhip ? 1 : getLashTextureStage(lashAgeTicks);
    }

    public static float getTextureStageProperty(int stage) {
        return (stage - 1) / (float) (FULL_EXTENSION_TEXTURE_STAGE - 1);
    }

    public static float getExtensionProgress(float lashAgeTicks) {
        float progress = Math.clamp(lashAgeTicks / STRIKE_IMPACT_DELAY_TICKS, 0, 1);
        return 1 - (1 - progress) * (1 - progress) * (1 - progress);
    }

    public static float getRetractionProgress(float lashAgeTicks) {
        float progress = getLinearRetractionProgress(lashAgeTicks);
        return 1 - (1 - progress) * (1 - progress) * (1 - progress);
    }

    public static float getFollowThroughWeight(float lashAgeTicks) {
        if (lashAgeTicks <= STRIKE_IMPACT_DELAY_TICKS || lashAgeTicks >= LASH_DURATION_TICKS) return 0;
        if (lashAgeTicks >= RETRACTION_START_TICK) return 1 - getRetractionProgress(lashAgeTicks);
        float progress = (lashAgeTicks - STRIKE_IMPACT_DELAY_TICKS) / (RETRACTION_START_TICK - STRIKE_IMPACT_DELAY_TICKS);
        return progress * progress * (3 - 2 * progress);
    }

    public static float getReboundWeight(float lashAgeTicks) {
        float progress = getLinearRetractionProgress(lashAgeTicks);
        if (progress <= 0 || progress >= 1) return 0;
        float reboundWave = (float) Math.sin(Math.PI * Math.pow(progress, 0.65));
        return reboundWave * reboundWave;
    }

    public static float getAimAlignmentWeight(float lashAgeTicks) {
        if (lashAgeTicks < 0 || lashAgeTicks >= LASH_DURATION_TICKS) return 0;
        float settleProgress = Math.clamp((lashAgeTicks - RETRACTION_START_TICK - 3) / (LASH_DURATION_TICKS - RETRACTION_START_TICK - 3), 0, 1);
        return getExtensionProgress(lashAgeTicks) * (1 - settleProgress * settleProgress * (3 - 2 * settleProgress));
    }

    public static float getWristRollDegrees(float lashAgeTicks) {
        return 10 * getFollowThroughWeight(lashAgeTicks) + 6 * getReboundWeight(lashAgeTicks);
    }

    public static float getLashArcPitchDegrees(float lashAgeTicks) {
        if (lashAgeTicks < 0 || lashAgeTicks >= LASH_DURATION_TICKS) return 0;
        if (lashAgeTicks < STRIKE_IMPACT_DELAY_TICKS) {
            float progress = lashAgeTicks / STRIKE_IMPACT_DELAY_TICKS;
            return 28 * (float) Math.pow(Math.sin(Math.PI * progress), 2);
        }
        return -28 * getFollowThroughWeight(lashAgeTicks) + 38 * getReboundWeight(lashAgeTicks);
    }

    private static float getLinearRetractionProgress(float lashAgeTicks) {
        return Math.clamp((lashAgeTicks - RETRACTION_START_TICK) / (LASH_DURATION_TICKS - RETRACTION_START_TICK), 0, 1);
    }

    private static float getFollowThroughPitchDegrees(float lashAgeTicks) {
        return -12 * getFollowThroughWeight(lashAgeTicks) + 18 * getReboundWeight(lashAgeTicks);
    }

    public static float getFirstPersonPitchDegrees(float attackPower, float lashAgeTicks) {
        if (lashAgeTicks < 0) return 55 * attackPower;
        float animatedPitch = 55 * attackPower + (-90 - 55 * attackPower) * getExtensionProgress(lashAgeTicks);
        return animatedPitch * (1 - getRetractionProgress(lashAgeTicks)) + getFollowThroughPitchDegrees(lashAgeTicks);
    }

    public static float getThirdPersonPitchRadians(float attackPower, float lashAgeTicks, float headPitchRadians) {
        float windupPitchRadians = -0.4F - attackPower * 2.5F;
        if (lashAgeTicks < 0) return windupPitchRadians;
        float forwardPitchRadians = -(float) Math.PI / 2 + headPitchRadians;
        float animatedPitch = windupPitchRadians + (forwardPitchRadians - windupPitchRadians) * getExtensionProgress(lashAgeTicks);
        return animatedPitch + (-0.4F - animatedPitch) * getRetractionProgress(lashAgeTicks)
                + getFollowThroughPitchDegrees(lashAgeTicks) * (float) Math.PI / 180;
    }

    private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180;
    private static final float MODEL_EYE_Y_OFFSET = -0.12F;

    public static Matrix4f createFirstPersonTransform(boolean leftHand, float attackPower, float lashAgeTicks, float equipProgress,
                                       Vector3f anchorPosition, float contactDistanceBlocks, float attackReachBlocks) {
        int handMirrorMultiplier = leftHand ? -1 : 1;
        float extensionProgress = lashAgeTicks < 0 ? 0 : getExtensionProgress(lashAgeTicks);
        float forwardExtensionWeight = extensionProgress * (1 - getRetractionProgress(lashAgeTicks));
        float aimAlignmentWeight = getAimAlignmentWeight(lashAgeTicks);
        float windupWeight = attackPower * (1 - extensionProgress);
        float followThroughWeight = getFollowThroughWeight(lashAgeTicks);
        float reboundWeight = getReboundWeight(lashAgeTicks);
        float forwardTravelMultiplier = WhipItem.calculateForwardTravelMultiplier(
                anchorPosition == null ? contactDistanceBlocks : anchorPosition.length(), attackReachBlocks);
        Matrix4f itemTransform = new Matrix4f().translation(handMirrorMultiplier * (0.56F + windupWeight * 0.08F - followThroughWeight * 0.04F),
                -0.52F - equipProgress * 0.6F + windupWeight * 0.15F - followThroughWeight * 0.04F + reboundWeight * 0.54F,
                -0.72F + windupWeight * 0.28F - forwardExtensionWeight * 0.6F * forwardTravelMultiplier
                        - followThroughWeight * 0.16F * forwardTravelMultiplier + reboundWeight * 0.22F)
                .rotateX(getFirstPersonPitchDegrees(attackPower, lashAgeTicks) * DEGREES_TO_RADIANS)
                .rotateY(handMirrorMultiplier * windupWeight * 15 * DEGREES_TO_RADIANS)
                .rotateZ(handMirrorMultiplier * getWristRollDegrees(lashAgeTicks) * DEGREES_TO_RADIANS);
        Vector3f itemGripPosition = calculateItemGripPosition(true, leftHand);
        Vector3f cameraGripPosition = itemTransform.transformPosition(new Vector3f(itemGripPosition));
        Vector3f aimTargetPosition = anchorPosition;
        float maxExtensionScale = WhipItem.FIRST_PERSON_EXTENSION_SCALE_MULTIPLIER;
        itemTransform.translate(itemGripPosition).scale(1 + forwardExtensionWeight * (maxExtensionScale - 1))
                .translate(new Vector3f(itemGripPosition).negate());
        if (aimAlignmentWeight > 0) {
            Vector3f itemTipDirection = itemTransform.transformDirection(calculateItemTipDirection(true, leftHand));
            if (aimTargetPosition == null) {
                float crosshairDepth = (float) Math.sqrt(Math.max(0.01F, itemTipDirection.lengthSquared()
                        - cameraGripPosition.x * cameraGripPosition.x - cameraGripPosition.y * cameraGripPosition.y));
                aimTargetPosition = new Vector3f(0, 0, cameraGripPosition.z - crosshairDepth);
            }
            Vector3f aimDirection = new Vector3f(aimTargetPosition).sub(cameraGripPosition);
            if (aimDirection.lengthSquared() > 0.0001F) {
                Quaternionf aimCorrection = new Quaternionf().rotationTo(itemTipDirection, aimDirection);
                new Quaternionf().slerp(aimCorrection, aimAlignmentWeight, aimCorrection);
                itemTransform = new Matrix4f().translation(cameraGripPosition).rotate(aimCorrection)
                        .translate(new Vector3f(cameraGripPosition).negate()).mul(itemTransform);
            }
            if (anchorPosition == null) {
                itemTransform = new Matrix4f().translation(cameraGripPosition).rotateX(getLashArcPitchDegrees(lashAgeTicks) * DEGREES_TO_RADIANS)
                        .translate(new Vector3f(cameraGripPosition).negate()).mul(itemTransform);
            }
        }
        return itemTransform;
    }

    public static Vector3f calculateThirdPersonArmRotation(boolean leftHand, Vector3f shoulderPosition, Vector3f armRotationRadians,
                                               Vector3f aimTargetPosition, float lashAgeTicks, boolean holdingGrapple) {
        float aimAlignmentWeight = getAimAlignmentWeight(lashAgeTicks);
        if (aimAlignmentWeight <= 0) return new Vector3f(armRotationRadians);
        Quaternionf originalArmRotation = new Quaternionf().rotationZYX(armRotationRadians.z, armRotationRadians.y, armRotationRadians.x);
        Quaternionf alignedArmRotation = new Quaternionf(originalArmRotation);
        Matrix4f heldItemTransform = createThirdPersonItemTransform(leftHand);
        Vector3f itemGripPosition = heldItemTransform.transformPosition(calculateItemGripPosition(false, leftHand));
        Vector3f itemTipDirection = heldItemTransform.transformDirection(calculateItemTipDirection(false, leftHand));
        aimTargetPosition = new Vector3f(aimTargetPosition);
        if (!holdingGrapple) {
            aimTargetPosition.y -= aimTargetPosition.distance(shoulderPosition) * (float) Math.sin(getLashArcPitchDegrees(lashAgeTicks) * DEGREES_TO_RADIANS);
        }
        for (int iteration = 0; iteration < 6; iteration++) {
            Vector3f handPosition = alignedArmRotation.transform(new Vector3f(itemGripPosition)).add(shoulderPosition);
            Vector3f aimDirection = new Vector3f(aimTargetPosition).sub(handPosition);
            if (aimDirection.lengthSquared() < 0.0001F) break;
            Quaternionf aimCorrection = new Quaternionf().rotationTo(alignedArmRotation.transform(new Vector3f(itemTipDirection)), aimDirection);
            alignedArmRotation.premul(aimCorrection);
        }
        Quaternionf blendedArmRotation = originalArmRotation.slerp(alignedArmRotation, aimAlignmentWeight).normalize();
        return new Vector3f((float) Math.atan2(2 * (blendedArmRotation.w * blendedArmRotation.x + blendedArmRotation.y * blendedArmRotation.z),
                        1 - 2 * (blendedArmRotation.x * blendedArmRotation.x + blendedArmRotation.y * blendedArmRotation.y)),
                (float) Math.asin(Math.clamp(2 * (blendedArmRotation.w * blendedArmRotation.y - blendedArmRotation.z * blendedArmRotation.x), -1, 1)),
                (float) Math.atan2(2 * (blendedArmRotation.w * blendedArmRotation.z + blendedArmRotation.x * blendedArmRotation.y),
                        1 - 2 * (blendedArmRotation.y * blendedArmRotation.y + blendedArmRotation.z * blendedArmRotation.z)));
    }

    public static Vector3f calculateThirdPersonAimTarget(float pitchRadians, float yawRadians, float attackReachBlocks) {
        return new Vector3f(0, 0, -attackReachBlocks).rotateX(pitchRadians).rotateY(yawRadians).add(0, MODEL_EYE_Y_OFFSET, 0);
    }

    public static Vector3f convertAnchorToModelSpace(Vector3f offsetFromEye, float bodyYawDegrees) {
        return new Vector3f(offsetFromEye).rotateY((bodyYawDegrees - 180) * DEGREES_TO_RADIANS).mul(-1, -1, 1).add(0, MODEL_EYE_Y_OFFSET, 0);
    }

    public static Vector3f calculateThirdPersonHandOffset(float pitchRadians, float yawRadians, float lashAgeTicks) {
        float reboundWeight = getReboundWeight(lashAgeTicks);
        return new Vector3f(0, 0, -0.1F * getFollowThroughWeight(lashAgeTicks) + 0.14F * reboundWeight)
                .rotateX(pitchRadians).rotateY(yawRadians);
    }

    public static Matrix4f createThirdPersonItemTransform(boolean leftHand) {
        return new Matrix4f().rotateX(-(float) Math.PI / 2).rotateY((float) Math.PI)
                .translate((leftHand ? -1 : 1) / 16.0F, 0.125F, -0.625F);
    }

    public static Vector3f calculateItemGripPosition(boolean firstPersonView, boolean leftHand) {
        float texturePlaneScale = firstPersonView ? FIRST_PERSON_TEXTURE_PLANE_SCALE : THIRD_PERSON_TEXTURE_PLANE_SCALE;
        float baseRotationZDegrees = firstPersonView && !leftHand ? 35 : 45;
        return new Quaternionf().rotationXYZ(0, (leftHand ? 90 : -90) * DEGREES_TO_RADIANS, baseRotationZDegrees * DEGREES_TO_RADIANS)
                .transform(new Vector3f(-7, -6, 0).mul(texturePlaneScale))
                .add(firstPersonView ? (leftHand ? -1.26F : 1.26F) : 0, firstPersonView ? 7.5F : 11, firstPersonView ? 1.13F : 1.5F).div(16);
    }

    public static Vector3f calculateItemTipDirection(boolean firstPersonView, boolean leftHand) {
        float heldTexturePlaneScale = firstPersonView
                ? FIRST_PERSON_TEXTURE_PLANE_SCALE * FIRST_PERSON_MODEL_SCALE_MULTIPLIER
                : THIRD_PERSON_TEXTURE_PLANE_SCALE * THIRD_PERSON_MODEL_SCALE_MULTIPLIER;
        return new Quaternionf().rotationXYZ(firstPersonView ? 0 : -(float) Math.PI / 2,
                (leftHand ? 90 : -90) * DEGREES_TO_RADIANS, 45 * DEGREES_TO_RADIANS)
                .transform(new Vector3f(12, 13, 0).mul(heldTexturePlaneScale / 16));
    }
}
