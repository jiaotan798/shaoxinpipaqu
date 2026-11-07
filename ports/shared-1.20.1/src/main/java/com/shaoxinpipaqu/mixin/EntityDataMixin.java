package com.shaoxinpipaqu.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 恢复数据随实体保存；专属键可在 Forge 与 Fabric 之间读取，且不依赖加载器附加数据。 */
@Mixin(Entity.class)
public abstract class EntityDataMixin implements com.shaoxinpipaqu.PipaEntityData {
    @Unique private CompoundTag pipa$data;

    @Override
    public CompoundTag pipa$data() {
        if (pipa$data == null) pipa$data = new CompoundTag();
        return pipa$data;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void pipa$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (pipa$data != null && !pipa$data.isEmpty()) cir.getReturnValue().put("PipaData", pipa$data.copy());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void pipa$load(CompoundTag tag, CallbackInfo ci) {
        pipa$data = tag.getCompound("PipaData").copy();
    }
}
