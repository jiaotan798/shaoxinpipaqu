package com.shaoxinpipaqu;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** 真实碰撞箱与伤害来源的边界回归；局部扫掠安全不等于全局路径一定可达。 */
@GameTestHolder(Pipa.ID)
@PrefixGameTestTemplate(false)
public final class PipaBoundaryGameTests {
    /** 水中结束交回原生游泳与重力，不保留永久飞行标记；不提供溺水/岩浆免伤。 */
    @GameTest(template="range",batch="pipa-water-recovery",timeoutTicks=60)
    public static void waterRecovery(GameTestHelper h) {
        for(var pos:BlockPos.betweenClosed(53,1,53,56,1,56))h.setBlock(pos,Blocks.STONE);
        for(var pos:BlockPos.betweenClosed(53,2,53,56,5,56))h.setBlock(pos,Blocks.WATER);
        var m=h.spawn(EntityType.PIG,new Vec3(54.5,3,54.5));m.setNoAi(true);m.setNoGravity(true);
        m.getPersistentData().putBoolean("PipaFlightRecovery",true);m.getPersistentData().putBoolean("PipaOriginalNoGravity",false);
        h.runAfterDelay(5,()->{
            h.assertTrue(m.isInWater(),"夹具确实处于液体中");
            h.assertFalse(MobFlight.controlled(m)||m.isNoGravity()||m.hasEffect(Pipa.AIRBORNE)||m.getPersistentData().getBoolean("PipaFlightRecovery"),"水中结束应清理飞行并交回原生控制");
            h.succeed();
        });
    }
    @GameTest(template="range",batch="pipa-terrain-boundaries")
    public static void sizeAndWorldBoundaries(GameTestHelper h) {
        var pig=h.spawn(EntityType.PIG,new Vec3(54.5,3,54.5));pig.setNoAi(true);
        for(var p:BlockPos.betweenClosed(53,2,50,53,8,60))h.setBlock(p,Blocks.STONE);
        for(var p:BlockPos.betweenClosed(55,2,50,55,8,60))h.setBlock(p,Blocks.STONE);
        h.assertTrue(MobFlight.safe(h.getLevel(),pig,new Vec3(0,0,2),false),"一格走廊允许小体型通过");
        pig.discard();
        var ravager=h.spawn(EntityType.RAVAGER,new Vec3(54.5,3,54.5));ravager.setNoAi(true);
        h.assertFalse(MobFlight.safe(h.getLevel(),ravager,new Vec3(0,0,2),false),"相同走廊拒绝大体型，不得只检查中心点");
        ravager.discard();
        var m=h.spawn(EntityType.PIG,new Vec3(65,5,65));m.setNoAi(true);
        double x=m.getX(),z=m.getZ();
        m.setPos(x,h.getLevel().getMaxBuildHeight()-m.getBbHeight()-0.05,z);
        h.assertFalse(MobFlight.safe(h.getLevel(),m,new Vec3(0,0.1,0),false),"不能越过世界顶部");
        m.setPos(x,h.getLevel().getMinBuildHeight()+0.05,z);
        h.assertFalse(MobFlight.safe(h.getLevel(),m,new Vec3(0,-0.1,0),false),"不能越过世界底部");
        m.setPos(x,h.absolutePos(new BlockPos(65,5,65)).getY(),z);
        int chunkX=(int)Math.floor(x/16),chunkZ=(int)Math.floor(z/16),offset=1;
        while(offset<128 && h.getLevel().hasChunk(chunkX+offset,chunkZ))offset++;
        h.assertTrue(offset<128,"夹具必须找到未加载区块");
        h.assertFalse(MobFlight.safe(h.getLevel(),m,new Vec3(offset*16+16,0,0),false),"扫掠跨入未加载区块必须拒绝");
        h.assertFalse(h.getLevel().hasChunk(chunkX+offset,chunkZ),"安全检查不得强制加载目标区块");
        m.discard();h.succeed();
    }

    @GameTest(template="range",batch="pipa-dynamic-block")
    public static void dynamicBlockRecovery(GameTestHelper h) {
        var m=h.spawn(EntityType.PIG,new Vec3(54.5,4,54.5));m.setNoAi(true);m.setNoGravity(true);
        m.getPersistentData().putBoolean("PipaFlightRecovery",true);
        m.getPersistentData().putBoolean("PipaOriginalNoGravity",false);
        h.setBlock(new BlockPos(54,4,54),Blocks.STONE);
        var before=m.position();MobFlight.entityTick(m);
        h.assertTrue(m.position().equals(before)&&!m.isNoGravity()&&!MobFlight.pauseAi(m),"新放方块嵌入时恢复原重力并交回脱困，不穿墙传送");
        h.setBlock(new BlockPos(54,4,54),Blocks.AIR);
        MobFlight.entityTick(m);
        h.assertTrue(m.getY()<=before.y && MobFlight.pauseAi(m),"空间恢复后继续下降");
        m.setOnGround(true);MobFlight.entityTick(m);
        h.assertFalse(MobFlight.controlled(m)||m.isNoGravity()||m.hasEffect(Pipa.AIRBORNE)||m.getPersistentData().getBoolean("PipaFlightRecovery"),"落地清理控制、恢复字段与姿势");
        m.discard();h.succeed();
    }

    /** 多个移动来源取并集，实体任一端在闭区间 50 格内即拦截，投射物归属沿原版伤害源追溯。 */
    @GameTest(template="range",batch="pipa-peace-boundaries")
    public static void peaceSourcesAndProjectiles(GameTestHelper h) {
        var first=h.makeMockServerPlayerInLevel();var second=h.makeMockServerPlayerInLevel();
        var center=h.absoluteVec(new Vec3(54,4,54));first.moveTo(center);second.moveTo(center);
        first.addEffect(new MobEffectInstance(Pipa.PEACE,100));second.addEffect(new MobEffectInstance(Pipa.PEACE,100));
        var attacker=h.spawn(EntityType.SKELETON,new Vec3(105,4,54));attacker.setNoAi(true);
        var victim=h.spawn(EntityType.PIG,new Vec3(104,4,54));victim.setNoAi(true);
        h.assertTrue(PeaceAura.blocks(attacker,victim),"被攻击者在50格闭区间，攻击者在外仍拦截");
        first.removeEffect(Pipa.PEACE);
        h.assertTrue(PeaceAura.blocks(attacker,victim),"移除一个来源不能覆盖其他有效来源");
        var arrow=h.spawn(EntityType.ARROW,new Vec3(104,4,54));arrow.setOwner(attacker);
        h.assertFalse(victim.hurt(victim.damageSources().arrow(arrow,attacker),2),"已射出的生物投射物在受保护端不造成伤害");
        second.removeEffect(Pipa.PEACE);
        h.assertTrue(victim.hurt(victim.damageSources().arrow(arrow,attacker),2),"最后一个来源消失后恢复同一投射物伤害");
        victim.invulnerableTime=0;second.addEffect(new MobEffectInstance(Pipa.PEACE,100));
        h.assertTrue(victim.hurt(victim.damageSources().playerAttack(first),1),"保留玩家主动攻击");
        victim.invulnerableTime=0;
        h.assertTrue(victim.hurt(victim.damageSources().fall(),1),"保留环境伤害");
        victim.moveTo(center.add(50.01,0,0));
        h.assertFalse(PeaceAura.blocks(attacker,victim),"双方均越过50格边界后释放");
        h.getLevel().getServer().getPlayerList().remove(first);h.getLevel().getServer().getPlayerList().remove(second);
        arrow.discard();h.succeed();
    }
}
