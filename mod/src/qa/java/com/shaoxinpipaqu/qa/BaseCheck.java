package com.shaoxinpipaqu.qa;

import com.friendswine.*;
import com.shaoxinpipaqu.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 不引用YSM或女仆类型的最小专服验收，验证它们确实是可选依赖。 */
@Mod("pipa_qa")
public final class BaseCheck {
    static final Path ROOT=System.getProperty("pipa.qa.root")==null?null:Path.of(System.getProperty("pipa.qa.root"));
    private int stage,age;
    private DollBlockEntity doll;
    public BaseCheck(){if(Boolean.getBoolean("pipa.qa.baseOnly")&&!Boolean.getBoolean("pipa.qa.memeOnly")&&!Boolean.getBoolean("pipa.qa.promo"))NeoForge.EVENT_BUS.addListener(this::tick);}
    static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    static void write(String name,String text) {
        try {
            Path temp=ROOT.resolve(name+".tmp");Files.writeString(temp,text);
            for(int attempt=0;;attempt++)try{Files.move(temp,ROOT.resolve(name),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);break;}
            catch(AccessDeniedException busy){if(attempt>=9)throw busy;Thread.sleep(10);}
        }catch(Exception error){throw new RuntimeException(error);}
    }
    private void next(int value){stage=value;age=0;write("stage.txt",Integer.toString(value));}
    private boolean ready(String role){return Files.exists(ROOT.resolve(role+"-"+stage+".ok"));}
    private void tick(ServerTickEvent.Post event) {
        if(stage==99||stage==-1)return;
        try {
            var server=event.getServer();var level=server.overworld();
            var actor=server.getPlayerList().getPlayerByName("PipaActor");var viewer=server.getPlayerList().getPlayerByName("PipaViewer");
            if(actor==null||viewer==null||!Files.exists(ROOT.resolve("actor-connected.ok"))||!Files.exists(ROOT.resolve("viewer-connected.ok")))return;
            if(age%20==0)write("base-state.txt","stage="+stage+" age="+age+" actor="+actor.position()+" ground="+actor.onGround()+" mayfly="+actor.getAbilities().mayfly+" mobs="+level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)).stream().map(m->m.getId()+":"+m.position()+" controlled="+MobFlight.controlled(m)).toList());
            require(++age<1200,"无可选依赖专项超时");
            if(stage==0) {
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                for(var pos:BlockPos.betweenClosed(-20,80,-20,20,80,20))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                actor.setGameMode(GameType.SURVIVAL);viewer.setGameMode(GameType.SURVIVAL);
                actor.connection.teleport(0.5,81,7,180,0);viewer.connection.teleport(-5,81,7,150,0);
                for(var player:java.util.List.of(actor,viewer)) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.MILK,1200));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,3600));
                }
                var pos=new BlockPos(0,81,0);level.setBlockAndUpdate(pos,FriendsWine.DOLL.get().defaultBlockState());doll=(DollBlockEntity)level.getBlockEntity(pos);
                SongState.of(doll).pipa$toggle();doll.startPlaying();
                for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();
                for(int i=0;i<5;i++){var pig=EntityType.PIG.create(level);pig.moveTo(3+i*2,81,0);pig.setNoAi(true);level.addFreshEntity(pig);}
                next(1);return;
            }
            if(stage==1&&age>=100&&ready("actor")&&ready("viewer")) {
                require(actor.getAbilities().flying&&actor.getY()>83,"无YSM原版玩家真实双击起飞");
                require(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)).stream().anyMatch(MobFlight::controlled),"无女仆模组时原版生物仍起飞");
                doll.stopPlaying();actor.removeEffect(Pipa.MILK);viewer.removeEffect(Pipa.MILK);next(2);
            } else if(stage==2&&age>=80&&actor.onGround()&&ready("actor")&&ready("viewer")) {
                if(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)).stream().anyMatch(MobFlight::controlled))return;
                require(!actor.getAbilities().mayfly&&!actor.hasEffect(Pipa.AIRBORNE)&&actor.hasEffect(Pipa.PEACE),"无可选依赖时飞行正常回收且不清除忘情");
                require(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)).stream().noneMatch(MobFlight::controlled),"原版生物正常安全落地");
                write("server-result.txt","PASS: no YSM or maid installed; base flight, effects and recovery\n");next(99);
            }
        }catch(Throwable error){error.printStackTrace();write("server-result.txt","FAIL: "+error);next(-1);}
    }
}
