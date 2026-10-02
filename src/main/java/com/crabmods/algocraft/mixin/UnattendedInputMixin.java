package com.crabmods.algocraft.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.crabmods.algocraft.client.test.UnattendedClientTestMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public abstract class UnattendedInputMixin {
    @Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
    private static void algocraft$neverMoveOrCaptureDesktopCursor(long window, int mode, double x, double y, CallbackInfo ci) {
        if (UnattendedClientTestMode.enabled()) {
            UnattendedClientTestMode.suppressedMouseGrabs++;
            ci.cancel();
        }
    }
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void algocraft$ignoreNativeKeyPoll(long window, int key, CallbackInfoReturnable<Boolean> ci) {
        if (UnattendedClientTestMode.enabled()) ci.setReturnValue(false);
    }
}
