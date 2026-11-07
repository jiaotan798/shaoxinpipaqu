package com.shaoxinpipaqu.qa.mixin;

import com.friendswine.client.DollMusicSound;
import com.shaoxinpipaqu.qa.AudioProbe;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 不读取或丢弃音频数据，只在异步定位结束、声音引擎接管之前记录实际 PCM 游标。 */
@Mixin(value=DollMusicSound.class,remap=false)
public abstract class AudioCaptureMixin {
    @Shadow private volatile long elapsedTicks;
    @Inject(method="getStream",at=@At("RETURN"),cancellable=true)
    private void probe$stream(SoundBufferLibrary buffers,Sound sound,boolean looping,CallbackInfoReturnable<java.util.concurrent.CompletableFuture<AudioStream>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApply(stream->{AudioProbe.capture(stream,elapsedTicks,((SoundInstance)this).getLocation().toString());return stream;}));
    }
}
