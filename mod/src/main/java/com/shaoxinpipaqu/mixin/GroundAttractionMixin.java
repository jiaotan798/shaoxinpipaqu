package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.MobFlight;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** 从地面候选移除空中个体，前置会按原释放逻辑清掉旧导航路径。 */
@Mixin(targets = "com.friendswine.DollAttraction", remap = false)
public abstract class GroundAttractionMixin {
    @Redirect(method="tick", at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<Mob> pipa$filter(ServerLevel l, Class<Mob> type, AABB box, Predicate<Mob> predicate) { return l.getEntitiesOfClass(type,box,predicate.and(m -> !MobFlight.controlled(m))); }
}
