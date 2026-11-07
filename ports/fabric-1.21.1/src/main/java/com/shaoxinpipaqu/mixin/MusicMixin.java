package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.friendswine.*;
import com.friendswine.client.DollMusicSound;
import com.shaoxinpipaqu.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 复用前置流式解码与中途加入定位，仅替换所选声音和 PCM 循环长度。 */
@Mixin(value=DollMusicSound.class,remap=false)
public abstract class MusicMixin {
    @Shadow @Final private DollBlockEntity doll;
    @Unique private boolean pipa$selected;
    @Inject(method="<init>",at=@At("TAIL"))
    private void pipa$sound(DollBlockEntity d,CallbackInfo ci) {
        pipa$selected=SongState.of(d).pipa$isSelected();
        if (pipa$selected) ((SoundLocationAccessor)this).pipa$location(ResourceLocation.fromNamespaceAndPath(Pipa.ID,"pipa"));
    }
    @Inject(method="matchesPlayingDoll",at=@At("HEAD"),cancellable=true)
    private void pipa$match(DollBlockEntity d,CallbackInfoReturnable<Boolean> cir) { if (pipa$selected!=SongState.of(d).pipa$isSelected()) cir.setReturnValue(false); }
    @WrapOperation(method="lambda$getStream$0",at=@At(value="INVOKE",target="Lcom/friendswine/JellyAnimation;musicFrame(J)J"))
    private long pipa$frame(long tick, Operation<Long> original) { return pipa$selected ? Math.floorMod(Math.floorMod(tick,AudioInfo.FRAMES)*(AudioInfo.SAMPLE_RATE/20),AudioInfo.FRAMES) : original.call(tick); }
}
