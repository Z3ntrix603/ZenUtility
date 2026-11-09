package de.pvputility.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int pu$getLeftPos();

    @Accessor("topPos")
    int pu$getTopPos();

    @Accessor("imageWidth")
    int pu$getImageWidth();

    @Accessor("imageHeight")
    int pu$getImageHeight();
}
