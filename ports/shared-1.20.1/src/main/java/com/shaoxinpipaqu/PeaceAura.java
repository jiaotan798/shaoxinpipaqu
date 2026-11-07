package com.shaoxinpipaqu;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

/** 只保存食用者来源；攻击任一端受保护即拦截，离开后不恢复旧仇恨。 */
public final class PeaceAura {
    private PeaceAura() {}
    public static boolean contains(LivingEntity e) {
        return !e.level().isClientSide && e.level().players().stream().anyMatch(p -> p.isAlive() && p.hasEffect(Pipa.peace()) && p.distanceToSqr(e) <= 2500);
    }
    public static boolean blocks(Mob a, LivingEntity v) { return a.level() == v.level() && (v instanceof Mob || v instanceof Player) && (contains(a) || contains(v)); }
    public static boolean cancelTarget(Mob mob, LivingEntity target) {
        return !(mob instanceof Creeper) && target != null && blocks(mob, target);
    }
    public static boolean cancelDamage(DamageSource source, LivingEntity victim) {
        return source.getEntity() instanceof Mob mob && !source.is(DamageTypeTags.IS_EXPLOSION) && blocks(mob, victim);
    }
    public static void tick(Mob mob) {
        if (mob.level().isClientSide || mob instanceof Creeper) return;
        if (mob.getTarget() != null && blocks(mob, mob.getTarget())) mob.setTarget(null);
        var brain = mob.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && blocks(mob, brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow())) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
