package de.pvputility.mixin;

import de.pvputility.hitcolor.HitColorManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OverlayTexture.class, remap = false)
public class OverlayTextureMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pu$onOverlayTextureInit(CallbackInfo ci) {
        HitColorManager.getInstance().applyToOverlayTexture((OverlayTexture) (Object) this);
    }
}
