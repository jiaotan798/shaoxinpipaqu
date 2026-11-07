package com.shaoxinpipaqu.qa;

import com.friendswine.*;
import com.shaoxinpipaqu.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 独立发布摄影布景：只改变新建世界，玩法由正式模组执行，不伪造飞行或卡点。 */
@Mod("pipa_qa")
public final class BasePromoServer {
    static int X, Y, Z;
    private int stage, age;
    private DollBlockEntity doll;
    private Villager trader;
    private boolean protectedMob;
    private final Random random = new Random(20261107);

    public BasePromoServer() {
        if (Boolean.getBoolean("pipa.qa.promo")) NeoForge.EVENT_BUS.addListener(this::tick);
    }
    private void block(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(X+x, y, Z+z), state, 2);
    }
    /** 高度图仍包含树干；向下找到真正的实心地面，避免把树梢误当成可用平地。 */
    private int ground(ServerLevel level,int x,int z) {
        // 远处樱花林尚未加载时，高度查询会返回世界最低高度；先完成区块生成再读高度图。
        level.getChunk(x>>4,z>>4);
        int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
        while(y>level.getMinBuildHeight()+1) {
            BlockPos at=new BlockPos(x,y-1,z);var state=level.getBlockState(at);
            if(!state.is(net.minecraft.tags.BlockTags.LOGS)&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&state.isSolidRender(level,at))return y;
            y--;
        }
        return y;
    }
    /** 以原生樱花林及远山为背景，只在局部修筑溪岸、桥和石径，不生成重复的人工树冠。 */
    private void garden(ServerLevel level) {
        var found=level.findClosestBiome3d(b->b.is(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE),level.getSharedSpawnPos(),6400,32,64);
        BaseCheck.require(found!=null,"世界中必须找到真实樱花林");
        BlockPos center=found.getFirst();int best=Integer.MAX_VALUE;
        for(int dx=-24;dx<=24;dx+=12)for(int dz=-24;dz<=24;dz+=12) {
            int cx=center.getX()+dx,cz=center.getZ()+dz;
            int h=ground(level,cx,cz);
            boolean cherry=level.getBiome(new BlockPos(cx,h,cz)).is(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE);
            int lo=h,hi=h;
            for(int[] q:new int[][]{{-9,-9},{9,-9},{-9,9},{9,9}}){int v=ground(level,cx+q[0],cz+q[1]);lo=Math.min(lo,v);hi=Math.max(hi,v);}
            int score=(hi-lo)*100+Math.abs(dx)+Math.abs(dz)+(cherry?0:200);
            if(score<best){best=score;X=cx;Y=h;Z=cz;}
        }
        BaseCheck.require(best<Integer.MAX_VALUE,"樱花林中必须找到摄影空地");
        BaseCheck.require(Y>level.getSeaLevel(),"摄影空地必须位于已生成的地表，不能采用未加载区块的最低高度");
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++) {
            double r=Math.hypot(x,z);if(r>13)continue;
            int original=ground(level,X+x,Z+z);
            double blend=Math.clamp((13-r)/5,0,1);blend=blend*blend*(3-2*blend);
            int top=(int)Math.round(original+(Y-original)*blend);
            for(int y=Math.min(original,top)-3;y<top-1;y++)block(level,x,y,z,Blocks.DIRT.defaultBlockState());
            block(level,x,top-1,z,Blocks.GRASS_BLOCK.defaultBlockState());
            for(int y=top;y<=Math.max(original+13,Y+15);y++)block(level,x,y,z,Blocks.AIR.defaultBlockState());
        }
        // 池岸使用不等宽曲线，底部砂砾与苔石混合，避免平直切口和发光方块占满画面。
        for(int x=-11;x<=11;x++)for(int z=-2;z<=11;z++) {
            double stream=5.8+Math.sin(x*.22)*1.1;
            double width=1.15+.45*Math.sin(x*.31);
            double pond=Math.pow((x+4)/4.2,2)+Math.pow((z-7)/3.0,2);
            boolean water=Math.abs(z-stream)<width||pond<1;
            if(water) {
                block(level,x,Y-3,z,random.nextBoolean()?Blocks.GRAVEL.defaultBlockState():Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                block(level,x,Y-2,z,Blocks.WATER.defaultBlockState());block(level,x,Y-1,z,Blocks.WATER.defaultBlockState());
                for(int y=Y;y<=Y+2;y++)block(level,x,y,z,Blocks.AIR.defaultBlockState());
            }else if(Math.abs(z-stream)<width+1||pond<1.55)block(level,x,Y-1,z,random.nextBoolean()?Blocks.MOSS_BLOCK.defaultBlockState():Blocks.COARSE_DIRT.defaultBlockState());
        }
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x*x+z*z<5)
            block(level,x,Y-1,z,(x+z)%3==0?Blocks.MOSSY_STONE_BRICKS.defaultBlockState():Blocks.ANDESITE.defaultBlockState());
        block(level,0,Y,0,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        for(int[] light:new int[][]{{-3,1},{3,-2}}) {
            block(level,light[0],Y,light[1],Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            block(level,light[0],Y+1,light[1],Blocks.LANTERN.defaultBlockState());
        }
        for(int z=-9;z<=4;z++) {int x=(int)Math.round(Math.sin(z*.43)*.8);block(level,x,Y-1,z,random.nextBoolean()?Blocks.GRAVEL.defaultBlockState():Blocks.COARSE_DIRT.defaultBlockState());}
        for(int x=6;x<=8;x++)for(int z=3;z<=9;z++) {
            block(level,x,Y,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
            if((x==6||x==8)&&(z==3||z==9)) {
                block(level,x,Y+1,z,Blocks.SPRUCE_FENCE.defaultBlockState());block(level,x,Y+2,z,Blocks.LANTERN.defaultBlockState());
            }
        }
        // 原生樱花树使用真实树形生成器；林外的山体、层次和树木保留世界生成结果。
        for(int[] q:new int[][]{{-10,-7},{-5,-11},{6,-10},{11,-5},{-12,9},{12,11}}) {
            int gy=ground(level,X+q[0],Z+q[1]);
            BlockPos at=new BlockPos(X+q[0],gy,Z+q[1]);level.setBlock(at,Blocks.CHERRY_SAPLING.defaultBlockState(),2);
            var sapling=(SaplingBlock)Blocks.CHERRY_SAPLING;
            sapling.advanceTree(level,at,level.getBlockState(at),level.random);
            if(level.getBlockState(at).is(Blocks.CHERRY_SAPLING))sapling.advanceTree(level,at,level.getBlockState(at),level.random);
        }
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)if(random.nextInt(7)==0) {
            int gy=ground(level,X+x,Z+z);
            var below=level.getBlockState(new BlockPos(X+x,gy-1,Z+z));
            if(below.is(Blocks.GRASS_BLOCK))block(level,x,gy,z,random.nextInt(3)==0?Blocks.FLOWERING_AZALEA.defaultBlockState():Blocks.PINK_PETALS.defaultBlockState());
        }
        // 小型茶摊偏离主演奏区，避免实心墙挡住樱花林背景。
        for(int x=-7;x<=-3;x++)for(int z=-5;z<=-3;z++)block(level,x,Y-1,z,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        for(int x=-6;x<=-3;x++)block(level,x,Y,-5,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int x=-5;x<=-4;x++)for(int y=Y+1;y<=Y+2;y++)block(level,x,y,-5,Blocks.STRIPPED_CHERRY_LOG.defaultBlockState());
        for(int x:new int[]{-7,-2})for(int y=Y;y<=Y+3;y++)block(level,x,y,-5,Blocks.SPRUCE_FENCE.defaultBlockState());
        for(int x=-7;x<=-2;x++)for(int z=-6;z<=-4;z++)block(level,x,Y+4,z,Blocks.CHERRY_SLAB.defaultBlockState());
        block(level,-7,Y+3,-4,Blocks.LANTERN.defaultBlockState());
        for(int i=0;i<2;i++) {
            ItemFrame frame=new ItemFrame(level,new BlockPos(X-5+i,Y+2,Z-4),Direction.SOUTH);
            frame.setItem(new ItemStack(i==0?Pipa.NOODLES.get():Pipa.DOG_MILK.get()));level.addFreshEntity(frame);
        }
        BaseCheck.write("promo-scene.json",new com.google.gson.Gson().toJson(Map.of("x",X,"y",Y,"z",Z,"biome",level.getBiome(new BlockPos(X,Y,Z)).unwrapKey().orElseThrow().location().toString())));
    }
    private void guests() {
        var level=(ServerLevel)doll.getLevel();
        for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(X-60,Y-8,Z-60,X+60,Y+25,Z+60)))guest.discard();
    }
    private void teleport(ServerPlayer player,double x,double y,double z,float yaw,float pitch) {
        player.connection.teleport(X+x,y,Z+z,yaw,pitch);
    }
    private void restart() {
        doll.stopPlaying(); doll.startPlaying();guests();
    }
    private void enter(int value, ServerLevel level, ServerPlayer actor, ServerPlayer viewer) {
        stage=value; age=0;
        viewer.closeContainer(); viewer.setGameMode(value==11?GameType.SURVIVAL:GameType.SPECTATOR);
        teleport(viewer,8,Y+4,14,145,10);
        actor.setGameMode(value<8||value>=13&&value!=15?GameType.SPECTATOR:GameType.SURVIVAL);
        // 场景切换先结束上一镜的主动飞行，不能把飞行姿势带进食物和交易特写。
        if(value!=9&&value!=15) {
            actor.getAbilities().flying=false;actor.onUpdateAbilities();
            if(value>=10&&value<=14)actor.removeEffect(Pipa.MILK);
        }
        actor.setHealth(actor.getMaxHealth()); actor.getFoodData().setFoodLevel(20);
        actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        if(value==1)actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(FriendsWine.REMOTE.get()));
        teleport(actor,30,Y,18,160,0);
        if(value==2||value==13)restart();
        if(value==6||value==14) {if(doll.getMode()!=DollBlockEntity.ORBIT)doll.cycleRemote();}
        if(value==7) {restart();doll.cycleRemote();}
        if(value==10)doll.stopPlaying();
        level.setDayTime(value==7||value==16?14500:value==9||value==10||value==11?3500:11500);
        if(value==8||value==9||value==15) {
            teleport(actor,7,Y+1.1,7,180,0);
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Pipa.DOG_MILK.get(),4));
            if(value!=8)actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.MILK,1200));
        }
        if(value==10)teleport(actor,-3,Y,0,150,0);
        if(value==11)teleport(viewer,-7,Y,-.8,160,0);
        if(value==12) {
            teleport(actor,2,Y,0,170,0);
            actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Pipa.NOODLES.get(),4));
            protectedMob=false;
        }
        BaseCheck.write("stage.txt",Integer.toString(stage));
    }
    private boolean ready(String role){return Files.exists(BaseCheck.ROOT.resolve(role+"-"+stage+".ok"));}
    private void tick(ServerTickEvent.Post event) {
        if(stage==99||stage==-1)return;
        try {
            var server=event.getServer();var level=server.overworld();
            var actor=server.getPlayerList().getPlayerByName("PipaActor");var viewer=server.getPlayerList().getPlayerByName("PipaViewer");
            if(actor==null||viewer==null||!Files.exists(BaseCheck.ROOT.resolve("actor-connected.ok"))||!Files.exists(BaseCheck.ROOT.resolve("viewer-connected.ok")))return;
            BaseCheck.require(++age<(stage==1?12000:3600),"发布摄影阶段超时："+stage);
            // 仅清理本轮摄影范围内的掉落实体，不删除展示框、手持物或原生樱花粒子。
            if(stage>0)for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(X-64,Y-16,Z-64,X+64,Y+32,Z+64)))item.discard();
            if(stage==0) {
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
                server.setDifficulty(Difficulty.NORMAL,true);level.setWeatherParameters(100000,0,false,false);
                garden(level);
                level.setBlockAndUpdate(new BlockPos(X,Y+1,Z),FriendsWine.DOLL.get().defaultBlockState());
                doll=(DollBlockEntity)level.getBlockEntity(new BlockPos(X,Y+1,Z));
                SongState.of(doll).pipa$toggle();doll.startPlaying();
                for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(X-60,Y-4,Z-60,X+60,Y+20,Z+60)))guest.discard();
                for(int i=0;i<10;i++) {
                    EntityType<?> type=switch(i%4){case 0->EntityType.SHEEP;case 1->EntityType.COW;case 2->EntityType.PIG;default->EntityType.CHICKEN;};
                    Mob mob=(Mob)type.create(level);double a=i*Math.PI/5;
                    mob.moveTo(X+Math.cos(a)*6,Y,Z+Math.sin(a)*6);mob.removeFreeWill();mob.setPersistenceRequired();level.addFreshEntity(mob);
                }
                trader=EntityType.VILLAGER.create(level);trader.setVillagerData(new VillagerData(VillagerType.PLAINS,VillagerProfession.NITWIT,1));
                trader.moveTo(X-7,Y,Z-3);trader.setNoAi(true);trader.setPersistenceRequired();ExtraTrades.ensure(trader);level.addFreshEntity(trader);
                BaseCheck.write("promo-trader.txt",Integer.toString(trader.getId()));
                enter(1,level,actor,viewer);return;
            }
            if(stage==12&&actor.hasEffect(Pipa.PEACE)&&!protectedMob) {
                var zombie=EntityType.ZOMBIE.create(level);zombie.moveTo(X+4,Y,Z-2);
                zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));zombie.setPersistenceRequired();level.addFreshEntity(zombie);
                zombie.setTarget(actor);protectedMob=true;
                BaseCheck.write("promo-peace.txt","Native completed noodles effect; zombie target="+zombie.getTarget()+"\n");
            }
            if(!ready("actor")||!ready("viewer"))return;
            if(stage==1&&!Files.exists(BaseCheck.ROOT.resolve("promo-continue.ok")))return;
            if(stage==16) {
                BaseCheck.write("server-result.txt","PASS photography only; native song, flight, foods and trading in staged cherry garden\n");
                stage=99;BaseCheck.write("stage.txt","99");
            } else enter(stage+1,level,actor,viewer);
        } catch(Throwable error) {
            error.printStackTrace();BaseCheck.write("server-result.txt","FAIL: "+error);stage=-1;BaseCheck.write("stage.txt","-1");
        }
    }
}
