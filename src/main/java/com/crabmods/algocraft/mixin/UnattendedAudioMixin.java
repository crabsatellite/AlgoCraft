package com.crabmods.algocraft.mixin;

import com.mojang.blaze3d.audio.Listener;
import com.crabmods.algocraft.client.test.UnattendedClientTestMode;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Listener.class)
public abstract class UnattendedAudioMixin {
    @Redirect(method = "setGain", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alListenerf(IF)V"))
    private void algocraft$silentDeviceNotSkippedSounds(int parameter, float gain) {
        boolean quiet = UnattendedClientTestMode.enabled();
        AL10.alListenerf(parameter, quiet ? 0.0f : gain);
        if (quiet) {
            UnattendedClientTestMode.deviceGain = AL10.alGetListenerf(AL10.AL_GAIN);
            UnattendedClientTestMode.gainWrites++;
        }
        // Listener.gain remains the real option. SoundEngine still creates and
        // starts channels; setting the option/field to zero would skip playback.
    }
}
