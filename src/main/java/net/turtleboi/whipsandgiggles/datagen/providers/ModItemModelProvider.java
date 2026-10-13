package net.turtleboi.whipsandgiggles.datagen.providers;

import static net.turtleboi.whipsandgiggles.client.WhipAnimation.FIRST_PERSON_MODEL_SCALE_MULTIPLIER;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.FIRST_PERSON_TEXTURE_PLANE_SCALE;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.THIRD_PERSON_MODEL_SCALE_MULTIPLIER;
import static net.turtleboi.whipsandgiggles.client.WhipAnimation.THIRD_PERSON_TEXTURE_PLANE_SCALE;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import net.turtleboi.whipsandgiggles.WhipsAndGiggles;
import net.turtleboi.whipsandgiggles.client.WhipAnimation;
import net.turtleboi.whipsandgiggles.client.WhipClientExtensions;
import net.turtleboi.whipsandgiggles.item.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModItemModelProvider extends ModelProvider {
    public ModItemModelProvider(PackOutput output) {
        super(output, WhipsAndGiggles.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var idleModel = ItemModelUtils.plainModel(createWhipModel(itemModels, "whip", "whip1", false));
        var stages = new ArrayList<RangeSelectItemModel.Entry>();
        for (int stage = 2; stage <= WhipAnimation.FULL_EXTENSION_TEXTURE_STAGE; stage++) {
            stages.add(ItemModelUtils.override(ItemModelUtils.plainModel(
                    createWhipModel(itemModels, "whip_stage_" + stage, "whip" + stage, true)),
                    WhipAnimation.getTextureStageProperty(stage)));
        }
        var animatedModel = ItemModelUtils.rangeSelect(new WhipClientExtensions.WhipStageProperty(), idleModel, stages);
        itemModels.itemModelOutput.accept(ModItems.WHIP.get(), ItemModelUtils.select(new DisplayContext(), animatedModel,
                ItemModelUtils.when(List.of(ItemDisplayContext.GUI), idleModel)));
    }

    private Identifier createWhipModel(ItemModelGenerators itemModels, String modelName, String textureName, boolean useLashPose) {
        var modelBuilder = ModelTemplates.FLAT_HANDHELD_ITEM.extend();
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, 0, -90, 45,
                useLashPose ? -90 : 0, -90, 45, 0, 11, 1.5F, THIRD_PERSON_TEXTURE_PLANE_SCALE, 0.85F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, 0, -90, -45,
                useLashPose ? -90 : 0, -90, -45, 0, 11, 1.5F, THIRD_PERSON_TEXTURE_PLANE_SCALE, 0.85F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, 0, -90, 35,
                0, -90, useLashPose ? 45 : 35, 1.26F, 7.5F, 1.13F, FIRST_PERSON_TEXTURE_PLANE_SCALE, 0.68F);
        applyHeldItemTransform(modelBuilder, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, 0, -90, -45,
                0, -90, -45, 1.26F, 7.5F, 1.13F, FIRST_PERSON_TEXTURE_PLANE_SCALE, 0.68F);
        modelBuilder.transform(ItemDisplayContext.GUI, transform -> transform.translation(8, 9.5F, 0).scale(2, 2, 1))
                .transform(ItemDisplayContext.GROUND, transform -> transform.translation(4, 6.75F, 0).scale(1, 1, 0.5F))
                .transform(ItemDisplayContext.FIXED, transform -> transform.translation(8, 9.5F, 0).scale(2, 2, 1));
        return modelBuilder.build().create(WhipsAndGiggles.identifier("item/" + modelName),
                TextureMapping.layer0(new Material(WhipsAndGiggles.identifier("item/" + textureName))), itemModels.modelOutput);
    }

    private static void applyHeldItemTransform(ExtendedModelTemplateBuilder modelBuilder, ItemDisplayContext displayContext, boolean leftHand,
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
        modelBuilder.transform(displayContext, transform -> transform.rotation(rotationXDegrees, rotationYDegrees, rotationZDegrees)
                .translation(translationX + handMirrorMultiplier * gripPositionCorrection.x,
                        translationY + gripPositionCorrection.y, translationZ + gripPositionCorrection.z)
                .scale(texturePlaneScale * perspectiveScaleMultiplier, texturePlaneScale * perspectiveScaleMultiplier, depthScale));
    }

    private static Quaternionf rotationFromDegrees(float rotationXDegrees, float rotationYDegrees, float rotationZDegrees) {
        float degreesToRadians = (float) Math.PI / 180;
        return new Quaternionf().rotationXYZ(rotationXDegrees * degreesToRadians,
                rotationYDegrees * degreesToRadians, rotationZDegrees * degreesToRadians);
    }
}
