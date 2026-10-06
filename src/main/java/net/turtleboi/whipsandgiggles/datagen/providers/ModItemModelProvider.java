package net.turtleboi.whipsandgiggles.datagen.providers;

import static net.turtleboi.whipsandgiggles.client.WhipAnimation.FIRST_PERSON_MODEL_SCALE_MULTIPLIER;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.THIRD_PERSON_MODEL_SCALE_MULTIPLIER;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.FIRST_PERSON_TEXTURE_PLANE_SCALE;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.THIRD_PERSON_TEXTURE_PLANE_SCALE;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.client.WhipAnimation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WhipsAndGiggles.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder idleModel = createWhipModel("whip", "whip1", false);
        ItemModelBuilder inventoryModel = createWhipModel(nested(), "whip1", false);
        for (int stage = 2; stage <= WhipAnimation.FULL_EXTENSION_TEXTURE_STAGE; stage++) {
            String textureName = "whip" + stage;
            ItemModelBuilder stageModel = getBuilder("whip_stage_" + stage)
                    .customLoader(SeparateTransformsModelBuilder::begin)
                    .base(createWhipModel(nested(), textureName, true))
                    .perspective(ItemDisplayContext.GUI, inventoryModel).end();
            idleModel.override().predicate(WhipsAndGiggles.resourceLocation("stage"), WhipAnimation.getTextureStageProperty(stage)).model(stageModel).end();
        }
    }

    private ItemModelBuilder createWhipModel(String modelName, String textureName, boolean useLashPose) {
        return createWhipModel(getBuilder(modelName), textureName, useLashPose);
    }

    private ItemModelBuilder createWhipModel(ItemModelBuilder modelBuilder, String textureName, boolean useLashPose) {
        modelBuilder.parent(getExistingFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/" + textureName));
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, 0, -90, 45,
                useLashPose ? -90 : 0, -90, 45, 0, 11, 1.5F, THIRD_PERSON_TEXTURE_PLANE_SCALE, 0.85F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, 0, -90, -45,
                useLashPose ? -90 : 0, -90, -45, 0, 11, 1.5F, THIRD_PERSON_TEXTURE_PLANE_SCALE, 0.85F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, 0, -90, 35,
                0, -90, useLashPose ? 45 : 35, 1.26F, 7.5F, 1.13F, FIRST_PERSON_TEXTURE_PLANE_SCALE, 0.68F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, 0, -90, -45,
                0, -90, -45, 1.26F, 7.5F, 1.13F, FIRST_PERSON_TEXTURE_PLANE_SCALE, 0.68F);
        modelBuilder.transforms().transform(ItemDisplayContext.GUI).translation(8, 9.5F, 0).scale(2, 2, 1).end()
                .transform(ItemDisplayContext.GROUND).translation(4, 6.75F, 0).scale(1, 1, 0.5F).end()
                .transform(ItemDisplayContext.FIXED).translation(8, 9.5F, 0).scale(2, 2, 1).end()
                .end();
        return modelBuilder;
    }

    private static void applyHeldItemTransform(ItemModelBuilder modelBuilder, ItemDisplayContext displayContext, boolean leftHand,
                                               float baseRotationXDegrees, float baseRotationYDegrees, float baseRotationZDegrees,
                                               float rotationXDegrees, float rotationYDegrees, float rotationZDegrees,
                                               float translationX, float translationY, float translationZ,
                                               float texturePlaneScale, float depthScale) {
        int handMirrorMultiplier = leftHand ? -1 : 1;
        float perspectiveScaleMultiplier = displayContext.firstPerson()
                ? FIRST_PERSON_MODEL_SCALE_MULTIPLIER : THIRD_PERSON_MODEL_SCALE_MULTIPLIER;
        Vector3f scaledGripPosition = new Vector3f(-7 * texturePlaneScale, -6 * texturePlaneScale, 0);
        Quaternionf baseGripRotation = rotationFromDegrees(baseRotationXDegrees,
                handMirrorMultiplier * baseRotationYDegrees, handMirrorMultiplier * baseRotationZDegrees);
        Quaternionf heldGripRotation = rotationFromDegrees(rotationXDegrees,
                handMirrorMultiplier * rotationYDegrees, handMirrorMultiplier * rotationZDegrees);
        Vector3f gripPositionCorrection = baseGripRotation.transform(new Vector3f(scaledGripPosition))
                .sub(heldGripRotation.transform(new Vector3f(scaledGripPosition).mul(perspectiveScaleMultiplier)));
        modelBuilder.transforms().transform(displayContext).rotation(rotationXDegrees, rotationYDegrees, rotationZDegrees)
                .translation(translationX + handMirrorMultiplier * gripPositionCorrection.x,
                        translationY + gripPositionCorrection.y, translationZ + gripPositionCorrection.z)
                .scale(texturePlaneScale * perspectiveScaleMultiplier, texturePlaneScale * perspectiveScaleMultiplier, depthScale).end().end();
    }

    private static Quaternionf rotationFromDegrees(float rotationXDegrees, float rotationYDegrees, float rotationZDegrees) {
        float degreesToRadians = (float) Math.PI / 180;
        return new Quaternionf().rotationXYZ(rotationXDegrees * degreesToRadians,
                rotationYDegrees * degreesToRadians, rotationZDegrees * degreesToRadians);
    }
}
