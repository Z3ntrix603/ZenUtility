package de.pvputility.mixin;

import de.pvputility.totem.TotemPopColorManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TotemParticle.class, remap = false)
public abstract class TotemParticleMixin extends SimpleAnimatedParticle {

    protected TotemParticleMixin(ClientLevel clientLevel, double d, double e, double f, SpriteSet spriteSet, float g) {
        super(clientLevel, d, e, f, spriteSet, g);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pu$onTotemParticleInit(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, SpriteSet spriteSet, CallbackInfo ci) {
        TotemPopColorManager mgr = TotemPopColorManager.getInstance();
        if (mgr.isEnabled()) {
            float[] rgb = mgr.getCurrentColorRgbFloat();
            float jitter = (this.random.nextFloat() - 0.5f) * 0.15f;
            float r = Math.max(0.0f, Math.min(1.0f, rgb[0] + jitter));
            float gr = Math.max(0.0f, Math.min(1.0f, rgb[1] + jitter));
            float b = Math.max(0.0f, Math.min(1.0f, rgb[2] + jitter));
            this.setColor(r, gr, b);
        }
    }
}
