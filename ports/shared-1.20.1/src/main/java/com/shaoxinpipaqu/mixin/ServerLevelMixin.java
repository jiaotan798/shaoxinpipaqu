package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.ExtraTrades;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.PeaceAura;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 原版实体更新前清理仇恨，更新后接管飞行位移，确保原版运动不会覆盖本刻结果。 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void pipa$beforeTick(Entity entity, CallbackInfo ci) {
        if (entity instanceof Mob mob) PeaceAura.tick(mob);
    }

    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void pipa$afterTick(Entity entity, CallbackInfo ci) {
        if (entity instanceof Mob mob) MobFlight.entityTick(mob);
        if (entity instanceof Villager villager) ExtraTrades.tick(villager);
    }
}
