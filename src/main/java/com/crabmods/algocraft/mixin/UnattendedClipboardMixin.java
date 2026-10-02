package com.crabmods.algocraft.mixin;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.crabmods.algocraft.client.test.UnattendedClientTestMode;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClipboardManager.class)
public abstract class UnattendedClipboardMixin {
    @Inject(method = "getClipboard", at = @At("HEAD"), cancellable = true)
    private void algocraft$getClipboard(long window, GLFWErrorCallbackI callback, CallbackInfoReturnable<String> ci) {
        if (UnattendedClientTestMode.enabled()) {
            UnattendedClientTestMode.clipboardReads++;
            ci.setReturnValue(UnattendedClientTestMode.clipboard);
        }
    }
    @Inject(method = "setClipboard", at = @At("HEAD"), cancellable = true)
    private void algocraft$setClipboard(long window, String content, CallbackInfo ci) {
        if (UnattendedClientTestMode.enabled()) {
            UnattendedClientTestMode.clipboardWrites++;
            UnattendedClientTestMode.clipboard = content;
            ci.cancel();
        }
    }
}
