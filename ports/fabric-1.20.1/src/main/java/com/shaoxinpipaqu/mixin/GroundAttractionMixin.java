package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shaoxinpipaqu.MobFlight;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 已在飞行名单内的生物由附属接管，前置会释放它原先的地面控制。 */
@Mixin(targets = "com.friendswine.DollAttraction", remap = false)
public abstract class GroundAttractionMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<Mob> pipa$filter(ServerLevel level, Class<Mob> type, AABB box, Predicate<Mob> predicate, Operation<List<Mob>> original) {
        return original.call(level, type, box, predicate.and(mob -> !MobFlight.controlled(mob)));
    }
}
