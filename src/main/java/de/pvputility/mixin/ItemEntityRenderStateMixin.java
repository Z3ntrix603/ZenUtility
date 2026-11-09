package de.pvputility.mixin;

import de.pvputility.scale.ScaledItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ItemEntityRenderState.class, remap = false)
public class ItemEntityRenderStateMixin implements ScaledItemEntityRenderState {

    @Unique
    private float pu$scale = 1.0f;

    @Override
    public float pu$getScale() {
        return pu$scale;
    }

    @Override
    public void pu$setScale(float scale) {
        this.pu$scale = scale;
    }
}
