package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.PlayerFlight;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 玩家自主飞行使用原版双击权限，仅替换速度与惯性；move 保留原版方块碰撞。 */
@Mixin(Player.class)
public abstract class PlayerTravelMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void pipa$travel(Vec3 input, CallbackInfo ci) {
        Player p=(Player)(Object)this;
        if (!PlayerFlight.active(p) || !p.isControlledByLocalInstance()) return;
        double speed=p.isSprinting()?0.4:0.3;
        // 原版在 travel 之前把跳跃/潜行写入竖直速度，符号用于升降，抬头不改变高度。
        double y=p.getDeltaMovement().y;
        p.setDeltaMovement(Vec3.ZERO);
        Vec3 flat=new Vec3(input.x,0,input.z);
        p.moveRelative((float)speed,flat.lengthSqr()>0.001?flat.normalize():Vec3.ZERO);
        Vec3 horizontal=p.getDeltaMovement();
        p.setDeltaMovement(new Vec3(horizontal.x,Math.abs(y)<1e-6?0:Math.copySign(0.15,y),horizontal.z));
        p.move(MoverType.SELF,p.getDeltaMovement());
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance=0;
        ci.cancel();
    }
}
