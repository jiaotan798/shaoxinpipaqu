package com.shaoxinpipaqu.qa;

import com.friendswine.*;
import com.shaoxinpipaqu.SongState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 卡点专项只操作真实玩偶与播放状态，不伪造客户端时间、纹理或渲染结果。 */
@Mod("pipa_qa")
public final class BaseMemeServer {
    private int stage,age;
    private DollBlockEntity doll;
    private final BlockPos position=new BlockPos(0,81,0);
    private long configStart;
    public BaseMemeServer() { if(Boolean.getBoolean("pipa.qa.memeOnly"))NeoForge.EVENT_BUS.addListener(this::tick); }
    private boolean ready(String role) { return java.nio.file.Files.exists(BaseCheck.ROOT.resolve(role+"-"+stage+".ok")); }
    private void next(int value) {stage=value;age=0;BaseCheck.write("stage.txt",Integer.toString(value));}
    private void tick(ServerTickEvent.Post event) {
        if(stage==99||stage==-1)return;
        try {
            var server=event.getServer();var level=server.overworld();
            var actor=server.getPlayerList().getPlayerByName("PipaActor");var viewer=server.getPlayerList().getPlayerByName("PipaViewer");
            if(actor==null||viewer==null||!java.nio.file.Files.exists(BaseCheck.ROOT.resolve("actor-connected.ok"))||!java.nio.file.Files.exists(BaseCheck.ROOT.resolve("viewer-connected.ok")))return;
            BaseCheck.require(++age<2400,"卡点专项阶段超时："+stage);
            if(stage>=6&&stage<=9)BaseCheck.require(doll.isPlaying()&&SongState.of(doll).pipa$isSelected()&&doll.getStartTick()==configStart,"本机设置不能停止音乐、切歌或重置服务端起播时钟");
            if(stage==0) {
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);
                for(var p:BlockPos.betweenClosed(-12,80,-12,12,80,12))level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
                actor.connection.teleport(0.5,81,7.5,180,-12);viewer.connection.teleport(-5,81,7.5,145,-12);
                level.setBlockAndUpdate(position,FriendsWine.DOLL.get().defaultBlockState());doll=(DollBlockEntity)level.getBlockEntity(position);
                SongState.of(doll).pipa$toggle();doll.startPlaying();next(1);return;
            }
            if(!ready("actor")||!ready("viewer"))return;
            if(stage==1) {doll.stopPlaying();next(2);}
            else if(stage==2) {
                SongState.of(doll).pipa$toggle();doll.startPlaying();
                // 用前置物品绑定生成真实数据，让客户端经按键和 TCP 请求切歌。
                actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FriendsWine.REMOTE.get()));
                FriendsWine.REMOTE.get().useOn(new UseOnContext(actor,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(position),Direction.UP,position,false)));
                actor.releaseUsingItem();
                next(3);
            }
            else if(stage==3 && SongState.of(doll).pipa$isSelected()) {
                BaseCheck.require(doll.getMode()==DollBlockEntity.FULL,"遥控器切歌保持播放档位");
                next(4);
            }
            else if(stage==4) {level.destroyBlock(position,false);next(5);}
            else if(stage==5) {
                level.setBlockAndUpdate(position,FriendsWine.DOLL.get().defaultBlockState());doll=(DollBlockEntity)level.getBlockEntity(position);
                SongState.of(doll).pipa$toggle();doll.startPlaying();configStart=doll.getStartTick();next(6);
            }
            else if(stage>=6&&stage<9)next(stage+1);
            else if(stage==9) {BaseCheck.write("server-result.txt","PASS playback, local config independence and unchanged song clock\n");next(99);}
        }catch(Throwable error){error.printStackTrace();BaseCheck.write("server-result.txt","FAIL: "+error);next(-1);}
    }
}
