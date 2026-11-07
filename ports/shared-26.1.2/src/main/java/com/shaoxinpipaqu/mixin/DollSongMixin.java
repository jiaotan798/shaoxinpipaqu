package com.shaoxinpipaqu.mixin;

import com.friendswine.DollBlockEntity;
import com.shaoxinpipaqu.SongState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 共用前置保存及同步包，旧存档缺字段时保持原曲。 */
@Mixin(value = DollBlockEntity.class, remap = false)
public abstract class DollSongMixin implements SongState {
    @Unique private boolean pipa$selected;
    @Shadow private long startTick;
    @Shadow private void sync() {}
    @Override public boolean pipa$isSelected() { return pipa$selected; }
    @Override public void pipa$toggle() {
        var doll = (DollBlockEntity) (Object) this;
        if (doll.getLevel() == null || doll.getLevel().isClientSide() || doll.isRemoved()) return;
        pipa$selected = !pipa$selected;
        // 不调用 startPlaying，避免切歌重复生成立板；档位与旋转时钟保持不变。
        if (doll.isPlaying()) startTick = doll.getLevel().getGameTime();
        sync();
    }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void pipa$save(ValueOutput tag, CallbackInfo ci) { tag.putBoolean("PipaSelected", pipa$selected); }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void pipa$load(ValueInput tag, CallbackInfo ci) { pipa$selected = tag.getBooleanOr("PipaSelected",false); }
}
