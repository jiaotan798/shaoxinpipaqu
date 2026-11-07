package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shaoxinpipaqu.MobFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.function.Predicate;

/** 排除飞行中的实体，前置会按自身的释放流程停止此前的地面导航。 */
@Mixin(targets="com.friendswine.DollAttraction",remap=false)
abstract class GroundAttractionMixin {
    // Loom 1.15.5 会将 Redirect 的单个 At 变成数组；WrapOperation 的数组形式与 MixinExtras 兼容。
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),remap=false)
    private static List<Mob> pipa$filter(ServerLevel level,Class<Mob> type,AABB box,Predicate<Mob> predicate,Operation<List<Mob>> original) {
        return original.call(level,type,box,predicate.and(mob->!MobFlight.controlled(mob)));
    }
}
