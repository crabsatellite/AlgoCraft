package com.crabmods.algocraft.mixin;

import com.mojang.blaze3d.platform.Window;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import com.crabmods.algocraft.client.test.UnattendedClientTestMode;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Mixin(Window.class)
public abstract class UnattendedWindowMixin {
    @Shadow private boolean fullscreen;
    @Shadow private boolean actuallyFullscreen;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private long algocraft$hiddenWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        if (!UnattendedClientTestMode.enabled())
            return ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
        // The Gradle pre-launch gate requires the *early* splash to be disabled.
        // This hidden, non-fullscreen context never becomes an interactive window.
        fullscreen = actuallyFullscreen = false;
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(width.getAsInt(), height.getAsInt(), title.get(), 0, 0);
        if (window == 0) throw new IllegalStateException("Hidden test GL context creation failed");
        return window;
    }
    @Inject(method = "setMode", at = @At("HEAD"))
    private void algocraft$neverFullscreen(CallbackInfo ci) {
        if (UnattendedClientTestMode.enabled()) fullscreen = actuallyFullscreen = false;
    }
    @Inject(method = "onFocus", at = @At("HEAD"), cancellable = true)
    private void algocraft$ignoreDesktopFocus(long window, boolean focused, CallbackInfo ci) {
        if (UnattendedClientTestMode.enabled()) ci.cancel();
    }
    @Inject(method = "bootCrash", at = @At("HEAD"))
    private static void algocraft$noModalCrash(int error, long description, CallbackInfo ci) {
        if (UnattendedClientTestMode.enabled())
            throw new IllegalStateException("Unattended GLFW initialization failed: " + error);
    }
}
