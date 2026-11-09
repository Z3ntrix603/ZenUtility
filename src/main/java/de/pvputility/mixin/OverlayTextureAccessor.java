package de.pvputility.mixin;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OverlayTexture.class, remap = false)
public interface OverlayTextureAccessor {

    @Accessor("texture")
    DynamicTexture pu$getTexture();
}
