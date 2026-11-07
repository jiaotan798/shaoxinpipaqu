package com.shaoxinpipaqu;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 只撤销本效果授予的权限；下降标记保存在玩家 NBT，重进可继续落地。 */
public final class PlayerFlight {
    private static final String OWNED="PipaOwnsFlight", LANDING="PipaLanding", SPEED="PipaPreviousSpeed";
    private static final Map<ServerPlayer,Boolean> POSED = new WeakHashMap<>();
    private PlayerFlight() {}
    public static boolean landing(Player p) { return Pipa.data(p).getBooleanOr(LANDING,false); }
    public static boolean active(Player p) { return p.hasEffect(Pipa.MILK) && p.getAbilities().flying && !p.isCreative() && !p.isSpectator(); }
    /** 效果授予→主动飞行→到期回收→落地清理；旁观者只接收姿势，不参与权限判定。 */
    public static void tick(ServerPlayer p) {
        var d=Pipa.data(p); var abilities=p.getAbilities();
        boolean nativeFlight=p.isCreative() || p.isSpectator();
        if (!p.isAlive()) {
            if (d.getBooleanOr(OWNED,false) && !nativeFlight) { abilities.mayfly=false; abilities.flying=false; abilities.setFlyingSpeed(d.getFloatOr(SPEED,0)); p.onUpdateAbilities(); }
            d.remove(OWNED); d.remove(LANDING); p.removeEffect(Pipa.AIRBORNE); sync(p,false); return;
        }
        if (p.hasEffect(Pipa.MILK) && !nativeFlight) {
            // 不覆盖进入本效果前已经存在的飞行来源。
            if (!abilities.mayfly) {
                d.putBoolean(OWNED,true); d.putFloat(SPEED,abilities.getFlyingSpeed());
                abilities.mayfly=true; p.onUpdateAbilities();
            }
            boolean was=POSED.getOrDefault(p,false);
            if (active(p)) d.putBoolean(LANDING,false);
            else if (was && !p.onGround()) d.putBoolean(LANDING,true);
            int remaining=p.getEffect(Pipa.MILK).getDuration();
            if (remaining>0 && remaining<=200 && remaining%20==0) p.sendOverlayMessage(Component.translatable("message.shaoxinpipaqu.remaining",remaining/20));
        } else if (d.getBooleanOr(OWNED,false)) {
            boolean flying=abilities.flying;
            // 只回收本模组实际授予的 mayfly；创造、旁观优先交还原版。
            // shortcut: mayfly 是共享布尔值，无法识别期间新增的第三方来源；适配具体飞行模组时需接入其权限协议。
            if (!nativeFlight) {
                abilities.mayfly=false; abilities.flying=false; abilities.setFlyingSpeed(d.getFloatOr(SPEED,0)); p.onUpdateAbilities();
                if (flying && !p.onGround()) d.putBoolean(LANDING,true);
            }
            d.remove(OWNED);
        }
        if (nativeFlight || abilities.flying && !p.hasEffect(Pipa.MILK)) d.remove(LANDING);
        if (landing(p)) {
            // 缓降只限制竖直速度，水平移动仍交给玩家；第一次落地即恢复正常摔落规则。
            if (p.onGround() || p.isInWater()) { d.remove(LANDING); p.fallDistance=0; }
            else {
                Vec3 v=p.getDeltaMovement();
                p.setDeltaMovement(new Vec3(v.x,Math.max(-0.1,Math.min(0,v.y)),v.z));
                p.fallDistance=0;
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
        }
        boolean pose=active(p) || landing(p);
        if (pose) {
            p.fallDistance=0;
            if (!p.hasEffect(Pipa.AIRBORNE) || p.tickCount%20==0) p.addEffect(new MobEffectInstance(Pipa.AIRBORNE,60,0,false,false,false));
        } else p.removeEffect(Pipa.AIRBORNE);
        sync(p,pose);
    }
    private static void sync(ServerPlayer p, boolean pose) {
        Boolean previous=POSED.put(p,pose);
        if (!shouldSync(previous,pose,p.tickCount)) return;
        if (pose && p.getEffect(Pipa.AIRBORNE)!=null) p.level().getChunkSource().sendToTrackingPlayers(p,new ClientboundUpdateMobEffectPacket(p.getId(),p.getEffect(Pipa.AIRBORNE),true));
        else p.level().getChunkSource().sendToTrackingPlayers(p,new ClientboundRemoveMobEffectPacket(p.getId(),Pipa.AIRBORNE));
    }
    /** 开启姿势需定期刷新有时限的标记；关闭后仅首次或状态改变发送，避免普通玩家每秒广播移除包。 */
    static boolean shouldSync(Boolean previous, boolean pose, int tickCount) {
        return previous==null || previous!=pose || pose && tickCount%20==0;
    }
    public static void tracking(ServerPlayer observer, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity p && p.hasEffect(Pipa.AIRBORNE)) observer.connection.send(new ClientboundUpdateMobEffectPacket(p.getId(),p.getEffect(Pipa.AIRBORNE),true));
    }
}
