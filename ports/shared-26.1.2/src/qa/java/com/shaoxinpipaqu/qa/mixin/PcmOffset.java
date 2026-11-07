package com.shaoxinpipaqu.qa.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets="com.friendswine.client.DollMusicSound$OffsetAudioStream",remap=false)
public interface PcmOffset {
    @Accessor("skippedBytes") long probe$skippedBytes();
}
