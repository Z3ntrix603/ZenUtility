package de.pvputility.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.pvputility.scale.ItemScaleConfig;
import de.pvputility.scale.ScaledItemEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemEntityRenderer.class, remap = false)
public class ItemEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void pu$extractScale(ItemEntity entity, ItemEntityRenderState state, float delta, CallbackInfo ci) {
        if (state instanceof ScaledItemEntityRenderState scaledState) {
            float scale = ItemScaleConfig.getInstance().getScaleForItem(entity.getItem());
            scaledState.pu$setScale(scale);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void pu$applyScale(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof ScaledItemEntityRenderState scaledState) {
            float scale = scaledState.pu$getScale();
            if (scale > 1.01f) {
                poseStack.scale(scale, scale, scale);
            }
        }
    }
}
