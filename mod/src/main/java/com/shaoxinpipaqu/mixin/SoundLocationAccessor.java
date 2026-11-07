package com.shaoxinpipaqu.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface SoundLocationAccessor {
    @Mutable @Accessor("location") void pipa$location(ResourceLocation id);
}
