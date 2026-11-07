package com.shaoxinpipaqu.qa.mixin;

import com.friendswine.DollBlockEntity;
import com.friendswine.client.DollMusicSound;
import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;

@Mixin(value=DollMusicSound.class,remap=false)
abstract class AudioCaptureMixin {
    @Shadow private volatile long elapsedTicks;
    @Inject(method="<init>",at=@At("TAIL"))
    private void probe$sound(DollBlockEntity doll,CallbackInfo ci) { PortClientCheck.INSTANCE.track((DollMusicSound)(Object)this); }

    /** 只查看定位完成的游标，不读取或丢弃引擎将要播放的 PCM。 */
    @Inject(method="getStream",at=@At("RETURN"),cancellable=true)
    private void probe$stream(SoundBufferLibrary buffers,Sound sound,boolean looping,CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        var instance=(DollMusicSound)(Object)this;
        cir.setReturnValue(cir.getReturnValue().thenApply(stream->{
            if(instance.getIdentifier().toString().equals("shaoxinpipaqu:pipa"))
                PortClientCheck.INSTANCE.pcm(stream,elapsedTicks);
            return stream;
        }));
    }
}
