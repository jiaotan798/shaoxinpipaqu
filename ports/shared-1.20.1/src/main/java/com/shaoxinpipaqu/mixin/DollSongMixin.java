package com.shaoxinpipaqu.mixin;

import com.friendswine.DollBlockEntity;
import com.shaoxinpipaqu.SongState;
import net.minecraft.nbt.CompoundTag;
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
        if (doll.getLevel() == null || doll.getLevel().isClientSide || doll.isRemoved()) return;
        pipa$selected = !pipa$selected;
        // 不调用 startPlaying，避免切歌重复生成立板；档位与旋转时钟保持不变。
        if (doll.isPlaying()) startTick = doll.getLevel().getGameTime();
        sync();
    }
    // 两个加载器发布包分别使用 SRG 和 intermediary；开发环境仍使用 Mojmap。
    @Inject(method = {"saveAdditional", "m_183515_", "method_11007"}, at = @At("TAIL"))
    private void pipa$save(CompoundTag tag, CallbackInfo ci) { tag.putBoolean("PipaSelected", pipa$selected); }
    @Inject(method = {"load", "m_142466_", "method_11014"}, at = @At("TAIL"))
    private void pipa$load(CompoundTag tag, CallbackInfo ci) { pipa$selected = tag.getBoolean("PipaSelected"); }
}
