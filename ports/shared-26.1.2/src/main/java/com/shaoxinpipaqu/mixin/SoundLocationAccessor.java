package com.shaoxinpipaqu.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface SoundLocationAccessor {
    @Mutable @Accessor("identifier") void pipa$location(Identifier id);
}
