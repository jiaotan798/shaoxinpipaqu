package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.ExtraTrades;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.PeaceAura;
import com.shaoxinpipaqu.PlayerFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 更新实体前清理仇恨，更新后交接飞行控制；乘客也需要释放既有飞行状态。 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void pipa$before(Entity entity, CallbackInfo ci) {
        if (entity instanceof Mob mob) PeaceAura.tick(mob);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void pipa$after(Entity entity, CallbackInfo ci) { pipa$finish(entity); }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void pipa$beforePassenger(Entity vehicle, Entity entity, CallbackInfo ci) { pipa$before(entity, ci); }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V", shift = At.Shift.AFTER))
    private void pipa$afterPassenger(Entity vehicle, Entity entity, CallbackInfo ci) { pipa$finish(entity); }

    @Unique private static void pipa$finish(Entity entity) {
        if (entity instanceof Mob mob) MobFlight.entityTick(mob);
        if (entity instanceof ServerPlayer player) PlayerFlight.tick(player);
        if (entity instanceof Villager villager) ExtraTrades.tick(villager);
    }
}
