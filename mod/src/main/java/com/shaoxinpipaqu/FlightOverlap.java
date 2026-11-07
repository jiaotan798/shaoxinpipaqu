package com.shaoxinpipaqu;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 六端共用的生物重叠脱离规则；地形和区块仍由对应版本的飞行实现检查。 */
final class FlightOverlap {
    private static final double STEP = 0.1; // 每 tick 最多脱离 0.1 格，斜向步长也相同。
    private static final double DIAGONAL = STEP / Math.sqrt(2);
    private static final List<Vec3> HORIZONTAL = List.of(
            new Vec3(STEP, 0, 0), new Vec3(-STEP, 0, 0),
            new Vec3(0, 0, STEP), new Vec3(0, 0, -STEP),
            new Vec3(DIAGONAL, 0, DIAGONAL), new Vec3(DIAGONAL, 0, -DIAGONAL),
            new Vec3(-DIAGONAL, 0, DIAGONAL), new Vec3(-DIAGONAL, 0, -DIAGONAL));
    private static final Vec3 UP = new Vec3(0, STEP, 0);
    private static final Vec3 DOWN = new Vec3(0, -STEP, 0);

    private FlightOverlap() {}

    /** 只放行能减轻每一处已有重叠的短步，沿途不能碰到新的生物。 */
    static Vec3 escape(ServerLevel level, Mob mob, boolean landing, Predicate<AABB> terrainClear) {
        AABB start = mob.getBoundingBox().inflate(0.03, 0, 0.03);
        var nearby = level.getEntities(mob, start.inflate(STEP),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator())
                .stream().map(entity -> entity.getBoundingBox()).toList();
        if (nearby.stream().noneMatch(start::intersects)) return Vec3.ZERO;

        // 空中优先上下分开；降落只横移，腾出净空后再继续下降。
        if (!landing) {
            if (canEscape(start, UP, nearby, terrainClear)) return UP;
            if (canEscape(start, DOWN, nearby, terrainClear)) return DOWN;
        }
        for (Vec3 step : HORIZONTAL) {
            if (canEscape(start, step, nearby, terrainClear)) return step;
        }
        return Vec3.ZERO;
    }

    private static boolean canEscape(AABB start, Vec3 step, List<AABB> nearby, Predicate<AABB> terrainClear) {
        AABB sweep = start.expandTowards(step);
        AABB end = start.move(step);
        for (AABB other : nearby) {
            if (!sweep.intersects(other)) continue;
            if (!start.intersects(other)) return false;
            if (end.intersects(other) && !closerToSeparation(start, end, other, step.y != 0)) return false;
        }
        return terrainClear.test(sweep);
    }

    private static boolean closerToSeparation(AABB start, AABB end, AABB other, boolean vertical) {
        if (vertical) {
            return separation(end.minY, end.maxY, other.minY, other.maxY)
                    < separation(start.minY, start.maxY, other.minY, other.maxY);
        }
        // 对角退让可以沿不同的轴远离两只生物。最近出口距离持平时，再比较另一轴，保证连续退让有进展。
        double beforeX = separation(start.minX, start.maxX, other.minX, other.maxX);
        double beforeZ = separation(start.minZ, start.maxZ, other.minZ, other.maxZ);
        double afterX = separation(end.minX, end.maxX, other.minX, other.maxX);
        double afterZ = separation(end.minZ, end.maxZ, other.minZ, other.maxZ);
        double before = Math.min(beforeX, beforeZ);
        double after = Math.min(afterX, afterZ);
        return after < before || (after == before && Math.max(afterX, afterZ) < Math.max(beforeX, beforeZ));
    }

    private static double separation(double min, double max, double otherMin, double otherMax) {
        // 一只生物完全套在另一只里面时，移向边缘也有进展，即使相交体积暂时不变。
        return Math.min(max - otherMin, otherMax - min);
    }
}
