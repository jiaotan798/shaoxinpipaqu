package com.shaoxinpipaqu;

import com.friendswine.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** 服务端名单与飞行状态机：起飞→巡航→安全下降；原重力写入实体 NBT 以便卸载恢复。 */
public final class MobFlight {
    private static final Map<Mob, Control> CONTROL = new WeakHashMap<>();
    private static final Map<DollBlockEntity, Long> CYCLES = new WeakHashMap<>();
    private static final String RECOVERY = "PipaFlightRecovery";
    private static final String GRAVITY = "PipaOriginalNoGravity";
    private MobFlight() {}
    public static boolean controlled(Mob mob) { return CONTROL.containsKey(mob); }
    public static boolean pauseAi(Mob mob) { Control c=CONTROL.get(mob); return c!=null && !c.embedded; }
    public static boolean posed(LivingEntity entity) { return entity.hasEffect(Pipa.AIRBORNE); }
    public static void unload(Object level) {
        // 不清除恢复 NBT；重新加载的个体必须先降落，不能遗留永久无重力。
        CONTROL.keySet().removeIf(m -> m.level() == level);
        CYCLES.keySet().removeIf(d -> d.getLevel() == level);
    }
    /** 起飞候选排除原生飞行者；人数统计在调用方独立完成，不能用此过滤器缩小统计总数。 */
    public static boolean eligible(Mob m) {
        // 1.21.1 的 Bat 继承 AmbientCreature，未实现 FlyingAnimal，且使用地面导航；必须单独排除，不能凭无重力判断。
        return m.isAlive() && !m.isPassenger() && !m.isVehicle() && !m.isSleeping()
                && !(m instanceof FlyingMob || m instanceof FlyingAnimal || m instanceof EnderDragon || m instanceof Bat)
                && !(m.getNavigation() instanceof FlyingPathNavigation);
    }
    /** 每秒调整人数，每遍重抽；先让旧名单降落再补位，避免重复控制。 */
    public static void levelTick(ServerLevel level) {
        long now = level.getGameTime();
        var dolls = DollBlockEntity.getActiveDolls(level);
        for (var entry : CONTROL.entrySet()) {
            var c = entry.getValue();
            var m = entry.getKey();
            if (m.level() != level || c.landing) continue;
            var nearest = nearest(dolls, m.position());
            if (nearest != c.owner || !dolls.contains(c.owner) || !SongState.of(c.owner).pipa$isSelected() || !eligible(m)) c.landing = true;
        }
        // 同一层tick内复用快照和人口归属；不能跨tick缓存，否则停止或拆除玩偶后会延迟交接。
        Map<Mob,DollBlockEntity> owners = new IdentityHashMap<>();
        for (DollBlockEntity doll : dolls) {
            if (!SongState.of(doll).pipa$isSelected()) continue;
            long cycle = doll.getStartTick() * 65537 + (long) Math.floor(doll.getElapsedTicks(now) / AudioInfo.LOOP_TICKS);
            Long previous = CYCLES.put(doll, cycle);
            boolean redraw = previous == null || previous.longValue() != cycle;
            if (!redraw && now % 20 != 0) continue;
            if (redraw) CONTROL.forEach((m,c) -> { if (c.owner == doll) c.landing = true; });
            Vec3 center = Vec3.atCenterOf(doll.getBlockPos());
            List<Mob> population = level.getEntitiesOfClass(Mob.class, new AABB(doll.getBlockPos()).inflate(50),
                    m -> m.isAlive() && m.position().distanceToSqr(center) <= 2500 && owners.computeIfAbsent(m, key -> nearest(dolls,key.position())) == doll);
            List<Mob> active = new ArrayList<>(CONTROL.keySet().stream().filter(m -> CONTROL.get(m).owner == doll && !CONTROL.get(m).landing).toList());
            int desired = FlightRules.count(population.size());
            if (active.size() > desired) Collections.shuffle(active, new Random(level.random.nextLong()));
            while (active.size() > desired) { Mob m = active.removeLast(); CONTROL.get(m).landing = true; }
            // 满员时仍需上面的人数统计与超额释放，但不必建立、校验或打乱无人使用的候选名单。
            if (active.size() >= desired) continue;
            List<Mob> candidates = new ArrayList<>(population.stream().filter(m -> !CONTROL.containsKey(m) && !Pipa.dataFlag(m, RECOVERY) && eligible(m)).toList());
            Collections.shuffle(candidates, new Random(level.random.nextLong()));
            for (Mob m : candidates) {
                if (active.size() >= desired) break;
                if (!safe(level, m, new Vec3(0, 2.2, 0), false)) continue;
                Control c = new Control(doll, m);
                Pipa.data(m).putBoolean(GRAVITY, m.isNoGravity());
                Pipa.data(m).putBoolean(RECOVERY, true);
                m.getNavigation().stop();
                m.setDeltaMovement(Vec3.ZERO);
                CONTROL.put(m, c);
                active.add(m);
            }
        }
    }
    /** 等价于前置50格最近活动来源规则；全部歌曲参与归属，等距时保留位置长整数的排序。 */
    static DollBlockEntity nearest(List<DollBlockEntity> dolls, Vec3 position) {
        DollBlockEntity nearest = null;
        double nearestDistance = 2500;
        for (DollBlockEntity doll : dolls) {
            double distance = position.distanceToSqr(Vec3.atCenterOf(doll.getBlockPos()));
            if (distance <= nearestDistance && (nearest == null || distance < nearestDistance
                    || doll.getBlockPos().asLong() < nearest.getBlockPos().asLong())) {
                nearest = doll;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
    /** 原版实体 tick 继续处理效果和存活；只替换移动，碰撞箱保持原尺寸。 */
    public static void entityTick(Mob m) {
        if (!(m.level() instanceof ServerLevel level)) return;
        Control c = CONTROL.get(m);
        if (c == null && Pipa.dataFlag(m, RECOVERY)) {
            c = new Control(null, m); c.landing = true; CONTROL.put(m, c);
        }
        if (c == null) return;
        if (!m.isAlive() || m.isPassenger() || m.isVehicle()) { release(m); return; }
        c.embedded=level.getBlockCollisions(m,m.getBoundingBox()).iterator().hasNext();
        if (c.embedded) {
            // 新方块占据身体时交回原版脱困；不瞬移穿墙，也不继续把目标拉向玩偶。
            c.landing=true; c.velocity=Vec3.ZERO; m.setNoGravity(Pipa.data(m).getBoolean(GRAVITY)); m.fallDistance=0; return;
        }
        if (c.landing && (m.onGround() || m.isInWater())) { release(m); return; }
        if (c.landing && level.getBlockCollisions(m,m.getBoundingBox().move(0,-0.12,0)).iterator().hasNext()) {
            // 探地 0.12 格不代表本次限速 0.1 格已经落地；无 AI 个体提前释放后会悬在剩余小缝隙上。
            m.move(MoverType.SELF,new Vec3(0,-0.1,0)); if (m.onGround()) release(m); return;
        }
        m.setNoGravity(true);
        m.fallDistance = 0;
        m.getNavigation().stop();
        if (!m.hasEffect(Pipa.AIRBORNE) || m.tickCount % 20 == 0) {
            m.addEffect(new MobEffectInstance(Pipa.AIRBORNE, 60, 0, false, false, false));
            // 原版只把完整效果发给乘客；旁观客户端需要显式同步，否则只有服务器知道飞行姿势。
            level.getChunkSource().broadcast(m,new ClientboundUpdateMobEffectPacket(m.getId(),m.getEffect(Pipa.AIRBORNE),true));
        }
        long now = level.getGameTime();
        if (c.landing) {
            if (now >= c.nextTarget || c.target == null) { c.target = landingSpot(level, m); c.nextTarget = now + 20; }
        } else {
            Vec3 center = Vec3.atCenterOf(c.owner.getBlockPos());
            if (!c.cruising && m.position().distanceToSqr(center.add(0,5,0)) < 81 && m.getY() >= center.y + 2) c.cruising = true;
            if (!c.cruising) c.target = center.add(0, 5, 0);
            else if (c.owner.getMode() == DollBlockEntity.ORBIT) {
                c.angle -= Math.PI * 2 / 400 * ServerConfig.ORBIT_SPEED.get().doubleValue();
                c.target = center.add(Math.cos(c.angle)*c.radius, FlightRules.wave(now), Math.sin(c.angle)*c.radius);
            } else if (now >= c.nextTarget || c.target == null || m.position().distanceToSqr(c.target) < 1) {
                double angle = level.random.nextDouble()*Math.PI*2, radius = level.random.nextDouble()*10;
                c.target = center.add(Math.cos(angle)*radius, 3 + level.random.nextDouble()*4, Math.sin(angle)*radius);
                c.nextTarget = now + 40 + level.random.nextInt(41);
            }
        }
        Vec3 toward = c.target == null ? Vec3.ZERO : c.target.subtract(m.position());
        if (c.landing && toward.y > 0) toward = new Vec3(toward.x, -0.1, toward.z);
        Vec3 desired = toward.lengthSqr() < 0.01 ? Vec3.ZERO : toward.normalize().scale(Math.min(0.2, toward.length()));
        desired = new Vec3(desired.x, Mth.clamp(desired.y, -0.1, 0.1), desired.z);
        if (c.landing) desired = new Vec3(desired.x, -0.1, desired.z);
        // 每 tick 最大约 4.5° 转向，起停用加速度限幅，避免随机目标造成瞬间折返。
        Vec3 motion = turn(c.velocity, desired);
        // 结束时立即取消残余上升速度；下降避障也不能重新爬升，否则会在障碍上方反复弹起。
        if (c.landing && motion.y>0) motion=new Vec3(motion.x,0,motion.z);
        if (now<c.retryAt) motion=Vec3.ZERO;
        else if (!safe(level, m, motion, c.cruising && !c.landing)) {
            motion = avoid(level, m, motion, c.cruising && !c.landing, c.landing);
            c.nextTarget = 0;
            if (motion.lengthSqr()>1e-8) c.blocked=0;
            else if (++c.blocked>=40) { motion=Vec3.ZERO; c.retryAt=now+20; c.blocked=0; }
        } else c.blocked = 0;
        c.velocity = motion;
        m.setDeltaMovement(Vec3.ZERO);
        m.move(MoverType.SELF, motion);
        m.fallDistance = 0;
        if (motion.horizontalDistanceSqr() > 1e-6) {
            float yaw = (float)(Math.atan2(-motion.x, motion.z)*180/Math.PI);
            m.setYRot(yaw); m.setYBodyRot(yaw); m.setYHeadRot(yaw);
        }
        m.hasImpulse = true;
    }
    private static Vec3 turn(Vec3 old, Vec3 desired) {
        double oldAngle = Math.atan2(old.z, old.x), angle = Math.atan2(desired.z, desired.x);
        double delta = Mth.wrapDegrees((angle-oldAngle)*180/Math.PI);
        double next = old.horizontalDistanceSqr() < 1e-6 ? angle : oldAngle + Mth.clamp(delta,-4.5,4.5)*Math.PI/180;
        double speed = Mth.clamp(desired.horizontalDistance(), Math.max(0,old.horizontalDistance()-0.015),old.horizontalDistance()+0.015);
        return new Vec3(Math.cos(next)*speed, Mth.clamp(desired.y,old.y-0.015,old.y+0.015), Math.sin(next)*speed);
    }
    /** 对整段扫掠体积检查方块、实体及所有横跨区块，拒绝隐式区块加载。 */
    public static boolean safe(ServerLevel level, Mob m, Vec3 delta, boolean clearance) {
        // 只横向留余量；向下膨胀会把脚下地板误当作起飞障碍，导致正常站地生物永远不能起飞。
        AABB box = m.getBoundingBox().expandTowards(delta).inflate(0.03,0,0.03);
        if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (int x = Mth.floor(box.minX)>>4; x <= Mth.floor(box.maxX)>>4; x++) for (int z = Mth.floor(box.minZ)>>4; z <= Mth.floor(box.maxZ)>>4; z++) if (!level.hasChunk(x,z)) return false;
        if (level.getBlockCollisions(m, box).iterator().hasNext() || livingCollision(level,m,box)) return false;
        // 刚进入巡航时可能仍在玩偶顶面上方不足两格；允许原地上升补足净空，否则所有避障方向都会被脚下方块锁死。
        // 仅豁免纯上升的下方净空，前面的身体扫掠仍检查天花板、实体和区块，不允许横向贴地穿越。
        boolean lifting=delta.y>0 && delta.horizontalDistanceSqr()<1e-10;
        if (clearance && !lifting && level.getBlockCollisions(m,m.getBoundingBox().move(delta).expandTowards(0,-2,0)).iterator().hasNext()) return false;
        return true;
    }
    private static Vec3 avoid(ServerLevel l, Mob m, Vec3 v, boolean clearance, boolean landing) {
        for (Vec3 d : new Vec3[]{new Vec3(0,0.1,0),new Vec3(-v.z,v.y,v.x),new Vec3(v.z,v.y,-v.x),new Vec3(v.x,0,v.z),new Vec3(0,-0.1,0)}) if ((!landing || d.y<=0) && safe(l,m,d,clearance)) return d;
        return Vec3.ZERO;
    }
    /** 落点和实际移动使用相同生物占位规则；不能选在普通生物脚下，再被扫掠检查永久挡住。 */
    private static Vec3 landingSpot(ServerLevel l, Mob m) {
        for (int r = 0; r <= 4; r++) for (int i = 0; i < (r == 0 ? 1 : 8); i++) {
            double a = i*Math.PI/4;
            Vec3 base = m.position().add(Math.cos(a)*r,0,Math.sin(a)*r);
            for (int y = Mth.floor(m.getY()); y >= Math.max(l.getMinBuildHeight(),m.getY()-64); y--) {
                BlockPos p = BlockPos.containing(base.x,y,base.z);
                if (!l.hasChunkAt(p)) break;
                if (!l.getBlockState(p).getCollisionShape(l,p).isEmpty()) {
                    Vec3 target = new Vec3(base.x,y+1,base.z);
                    AABB box = m.getBoundingBox().move(target.subtract(m.position()));
                    // 原版noCollision不保证避让所有普通LivingEntity；沿用safe的0.03格横向余量，保留方块/硬实体检查。
                    if (l.noCollision(m,box) && !livingCollision(l,m,box.inflate(0.03,0,0.03))) return target;
                    break;
                }
            }
        }
        return m.position().add(0,-1,0);
    }
    /** 统一巡航扫掠与落点的生物边界：存活且非旁观的生物/玩家均需避让，不推挤让路。 */
    private static boolean livingCollision(ServerLevel level,Mob mob,AABB box) {
        return !level.getEntities(mob,box,e->e.isAlive()&&!e.isSpectator()&&e instanceof LivingEntity).isEmpty();
    }
    private static void release(Mob m) {
        m.setNoGravity(Pipa.data(m).getBoolean(GRAVITY));
        Pipa.data(m).remove(RECOVERY); Pipa.data(m).remove(GRAVITY);
        m.removeEffect(Pipa.AIRBORNE);
        if (m.level() instanceof ServerLevel level) level.getChunkSource().broadcast(m,new ClientboundRemoveMobEffectPacket(m.getId(),Pipa.AIRBORNE));
        m.fallDistance = 0; CONTROL.remove(m);
    }
    private static final class Control {
        final DollBlockEntity owner;
        boolean landing, cruising, embedded;
        Vec3 target, velocity = Vec3.ZERO;
        long nextTarget, retryAt;
        int blocked;
        double angle, radius;
        Control(DollBlockEntity owner, Mob m) { this.owner=owner; angle=m.getRandom().nextDouble()*Math.PI*2; radius=3+m.getRandom().nextDouble()*2; }
    }
}
