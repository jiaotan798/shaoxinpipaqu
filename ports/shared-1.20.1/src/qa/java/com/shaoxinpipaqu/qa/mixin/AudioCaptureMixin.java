package com.shaoxinpipaqu.qa.mixin;

import com.friendswine.client.DollMusicSound;
import com.shaoxinpipaqu.qa.PortClientCheck;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在解码完成后观察实际流对象，原样交给声音引擎，不读取或改变 PCM。 */
@Mixin(value = DollMusicSound.class, remap = false)
public abstract class AudioCaptureMixin {
    @Inject(method = "getStream", at = @At("RETURN"), cancellable = true)
    private void probe$stream(SoundBufferLibrary buffers, Sound sound, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApply(stream -> {
            PortClientCheck.captureAudio((SoundInstance) (Object) this, stream);
            return stream;
        }));
    }
}
