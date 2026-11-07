package com.shaoxinpipaqu;

import com.friendswine.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import org.slf4j.LoggerFactory;

/** 人数抽选回归；覆盖原生飞行资格、取整边界与全部 20 tick 补位相位，失败时保留候选及碰撞状态。 */
@GameTestHolder(Pipa.ID)
@PrefixGameTestTemplate(false)
public final class PipaPopulationGameTests {
    /** 全是原生飞行者时仍计入总数，但不得为了凑两个名额接管任何一只。旧逻辑会稳定选中蝙蝠。 */
    @GameTest(template="range",batch="pipa-native-flight",timeoutTicks=60)
    public static void batsCountButNeverTakeOff(GameTestHelper h) {
        var pos=new BlockPos(54,2,54);h.setBlock(pos,FriendsWine.DOLL.get());
        DollBlockEntity doll=h.getBlockEntity(pos);SongState.of(doll).pipa$toggle();
        List<net.minecraft.world.entity.ambient.Bat> bats=new ArrayList<>();
        for(int i=0;i<10;i++) {
            var bat=h.spawn(EntityType.BAT,new Vec3(40+i*2,3,48));
            bat.setNoAi(true);bat.setNoGravity(true);bat.setResting(i%2==0);bats.add(bat);
        }
        doll.startPlaying();
        for(var guest:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60)))guest.discard();
        h.runAfterDelay(10,()->{
            long selected=bats.stream().filter(MobFlight::controlled).count();
            int total=population(h,doll).size();
            LoggerFactory.getLogger("pipa-population-test").info("native-only N={} desired={} controlled={} navigation={}",total,FlightRules.count(total),selected,bats.getFirst().getNavigation().getClass().getSimpleName());
            h.assertTrue(total==10,"十只蝙蝠必须仍计入人口，不能通过移出统计绕过资格错误");
            h.assertTrue(selected==0,"原生蝙蝠不得接管，实际="+selected);
            h.assertTrue(bats.stream().noneMatch(MobFlight::eligible),"原生蝙蝠不得进入候选池");
            h.assertTrue(bats.stream().noneMatch(b->b.hasEffect(Pipa.AIRBORNE)||b.getPersistentData().getBoolean("PipaFlightRecovery")),"排除的蝙蝠不得写入姿势或恢复标记");
            // 无重力可能来自地图或其他模组，不能把普通陆生生物一并当作原生飞行者排除。
            var pig=h.spawn(EntityType.PIG,new Vec3(65,3,55));pig.setNoGravity(true);
            h.assertTrue(MobFlight.eligible(pig),"无重力猪仍应具有抽选资格");pig.discard();
            doll.stopPlaying();h.succeed();
        });
    }
    /** 每轮间隔 11 tick，与 20 互质；每组 20 轮覆盖全部补位相位，N=10 原失败组合重复覆盖两组。 */
    @GameTest(template="range",batch="pipa-population-stress",timeoutTicks=2000)
    public static void repeatedPopulation(GameTestHelper h) {
        var pos=new BlockPos(54,2,54);h.setBlock(pos,FriendsWine.DOLL.get());
        DollBlockEntity doll=h.getBlockEntity(pos);SongState.of(doll).pipa$toggle();
        for(BlockPos p:BlockPos.betweenClosed(35,0,40,75,0,55))h.setBlock(p,Blocks.STONE);
        trial(h,doll,0);
    }
    /** 每次生成全新实体，经过真实服务端 tick 后核对总数、名额及蝙蝠状态，不使用概率重试掩盖失败。 */
    private static void trial(GameTestHelper h,DollBlockEntity doll,int round) {
        int total=new int[]{9,10,14,15,10}[round/20];
        int expected=total==9?1:total==15?3:2;
        long phase=h.getLevel().getGameTime()%20;
        List<Pig> pigs=new ArrayList<>();
        for(int i=0;i<total-1;i++){var p=h.spawn(EntityType.PIG,new Vec3(40+i*2,3,48));p.setNoAi(true);pigs.add(p);}
        var bat=h.spawn(EntityType.BAT,new Vec3(58,3,52));bat.setNoAi(true);bat.setNoGravity(true);
        doll.startPlaying();
        for(var guest:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60)))guest.discard();
        String initial=snapshot(h,doll,pigs);
        h.runAfterDelay(10,()->{
            long selected=pigs.stream().filter(MobFlight::controlled).count();
            int actual=population(h,doll).size();
            boolean passed=actual==total && selected==expected && !MobFlight.eligible(bat) && !MobFlight.controlled(bat) && !bat.hasEffect(Pipa.AIRBORNE);
            LoggerFactory.getLogger("pipa-population-test").info("round={} phase={} N={} expected={} selected={} batEligible={} batControlled={} PASS={}",round,phase,actual,expected,selected,MobFlight.eligible(bat),MobFlight.controlled(bat),passed);
            if(!passed)LoggerFactory.getLogger("pipa-population-test").error("initial={} final={}",initial,snapshot(h,doll,pigs));
            h.assertTrue(passed,"人数抽选 round="+round+" N="+actual+" expected="+expected+" selected="+selected+" batControlled="+MobFlight.controlled(bat));
            // 仅在独立 GameTest 世界清理本轮控制表，防止前一轮的下降恢复参与下一轮夹具。
            doll.stopPlaying();MobFlight.unload(h.getLevel());pigs.forEach(Entity::discard);bat.discard();
            if(round==99)h.succeed();else h.runAfterDelay(1,()->trial(h,doll,round+1));
        });
    }
    private static String snapshot(GameTestHelper h,DollBlockEntity doll,List<Pig> pigs) {
        var population=population(h,doll);
        return "tick="+h.getLevel().getGameTime()+" N="+population.size()+" mobs="+population.stream().map(m->m.getType()+":"+m.getId()+":eligible="+MobFlight.eligible(m)+":control="+MobFlight.controlled(m)).toList()+" pigs="+pigs.stream().map(p->p.getId()+":"+p.position()+":control="+MobFlight.controlled(p)+":safe="+MobFlight.safe(h.getLevel(),p,new Vec3(0,2.2,0),false)).toList();
    }
    private static List<Mob> population(GameTestHelper h,DollBlockEntity doll) {
        var center=Vec3.atCenterOf(doll.getBlockPos());
        return h.getLevel().getEntitiesOfClass(Mob.class,new AABB(doll.getBlockPos()).inflate(50),m->m.isAlive()&&m.position().distanceToSqr(center)<=2500);
    }
}
