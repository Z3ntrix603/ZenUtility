package de.pvputility.mixin;

import de.pvputility.durability.LowDurabilityManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void pu$onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LowDurabilityManager mgr = LowDurabilityManager.getInstance();
        if (mgr.isEnabled() && mgr.isBlockMining() && mgr.isHoldingLowDurabilityTool()) {
            mgr.triggerMiningBlockedWarning();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void pu$onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LowDurabilityManager mgr = LowDurabilityManager.getInstance();
        if (mgr.isEnabled() && mgr.isBlockMining() && mgr.isHoldingLowDurabilityTool()) {
            mgr.triggerMiningBlockedWarning();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void pu$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        LowDurabilityManager mgr = LowDurabilityManager.getInstance();
        if (mgr.isEnabled() && mgr.isBlockMining() && mgr.isHoldingLowDurabilityTool()) {
            mgr.triggerMiningBlockedWarning();
            cir.setReturnValue(false);
        }
    }
}
